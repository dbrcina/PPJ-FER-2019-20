import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FRISCGeneratorBegs {

    private static final String NAREDBA_PRIDRUZIVANJA = "<naredba_pridruzivanja>";
    private static final String ZA_PETLJA = "<za_petlja>";
    private static final String LISTA_NAREDBI = "<lista_naredbi>";
    private static final String IDN = "IDN";
    private static final String BROJ = "BROJ";
    private static final String P = "<P>";
    private static final String KR_AZ = "KR_AZ";
    private static final String KR_ZA = "KR_ZA";
    private static final String KR_OD = "KR_OD";
    private static final String KR_DO = "KR_DO";
    private static final String EPSILON = "$";
    private static final String T_LISTA = "<T_lista>";
    private static final String E_LISTA = "<E_lista>";
    private static final String OP_PLUS = "OP_PLUS";
    private static final String OP_MINUS = "OP_MINUS";
    private static final String OP_PUTA = "OP_PUTA";
    private static final String OP_DIJELI = "OP_DIJELI";

    public static void main(String[] args) throws IOException {
        List<String> syntaxTree = initSyntaxTreeFromStdin();

        BufferedWriter bwr = Files.newBufferedWriter(Paths.get("a.frisc"));
        bwr.write("        MOVE 40000, R7\n\n");
        Map<String, String> memory = new LinkedHashMap<>();
        String V = "V";
        int vCounter = -1;

        boolean inLoop = false;
        String loopVar = null;
        boolean addedLabel = false;
        boolean increment = true;
        for (int i = 0; i < syntaxTree.size(); i++) {
            String rootNode = syntaxTree.get(i);
            if (!rootNode.equals(NAREDBA_PRIDRUZIVANJA) && !rootNode.equals(ZA_PETLJA))
                continue;

            // skip key words NAREDBA_PRIDRUZIVANJA and ZA_PETLJA
            String nextNode = syntaxTree.get(++i);

            boolean opPlus = false;
            boolean opMinus = false;
            boolean opPuta = false;
            boolean opDijeli = false;
            String currentVar;
            String sign = "";
            if (rootNode.equals(NAREDBA_PRIDRUZIVANJA)) {
                // first next node is IDN
                String[] parts = nextNode.split("\\s+");
                memory.putIfAbsent(parts[2], V + ++vCounter + "      DW 0");
                currentVar = parts[2];
                nextNode = syntaxTree.get(++i);
                while (!nextNode.equals(LISTA_NAREDBI)) {
                    if (nextNode.startsWith(P)) nextNode = syntaxTree.get(++i);
                    if (nextNode.startsWith(OP_MINUS)) sign = "-";
                    if (nextNode.startsWith(BROJ) || nextNode.startsWith(IDN)) {
                        parts = nextNode.split("\\s+");
                        if (parts[0].equals(BROJ)) {
                            if (opPlus || opMinus) sign = "";
                            bwr.write("        MOVE %D " + sign + parts[2] + ", R0\n");
                            bwr.write("        PUSH R0\n");
                            bwr.write("        STORE R0, ("+memory.get(currentVar).split("\\s+")[0]+
                                    ")\n");
                            sign = "";
                        } else {
                            String s;
                            if (loopVar == null) {
                                s = parts[2];
                            } else {
                                s = !parts[2].equals(loopVar.substring(0,1)) ? parts[2] : loopVar;
                            }
                            String m = memory.get(s).split("\\s+")[0];
                            if (inLoop) {
                                if (!addedLabel) {
                                    bwr.write("L0      LOAD R0, (" + m + ")\n");
                                    bwr.write("        PUSH R0\n");
                                    bwr.write("        LOAD R0, ("+memory.get(loopVar).split("\\s" +
                                            "+")[0]+")\n");
                                    bwr.write("        PUSH R0\n");
                                    addedLabel = true;
                                }
                            } else {
                                bwr.write("        LOAD R0, (" + m + ")\n");
                                bwr.write("        PUSH R0\n\n");
                            }
                        }
                        if (opPlus || opMinus) {
                            if (!syntaxTree.get(i+2).equals(EPSILON)) {
                                if (syntaxTree.get(i+2).startsWith(OP_PUTA)) opPuta = true;
                                else opDijeli = true;
                                while(!syntaxTree.get(i).startsWith(P)) i++;
                                nextNode = syntaxTree.get(++i);
                                parts = nextNode.split("\\s+");
                                String s;
                                if (loopVar == null) s = parts[2];
                                else s = !parts[2].equals(loopVar.substring(0,1)) ? parts[2] : loopVar;
                                String m = memory.get(s).split("\\s+")[0];
                                bwr.write("        LOAD R0, ("+m+")\n");
                                bwr.write("        PUSH R0\n\n");
                                bwr.write("        CALL " + (opPuta ? "MUL" : "DIV")+"\n");
                                opPuta = opDijeli = false;
                            }
                            bwr.write("        POP R1\n");
                            bwr.write("        POP R0\n");
                            String op = opPlus ? "ADD" : "SUB";
                            bwr.write("        " + op + " R0, R1, R2\n");
                            bwr.write("        PUSH R2\n\n");
                            bwr.write("        STORE R2, ("+memory.get(currentVar).split("\\s+")[0]+")" +
                                    "\n");
                            if (!increment) {
                                String m = memory.get(loopVar).split("\\s+")[0];
                                bwr.write("        LOAD R0, ("+m+")\n");
                                bwr.write("        ADD R0, 1, R0\n");
                                bwr.write("        STORE R0, ("+m+")\n");
                                bwr.write("        LOAD R0, ("+m+")\n");
                                bwr.write("        LOAD R1, (DO)\n");
                                bwr.write("        CMP R0, R1\n");
                                bwr.write("        JP_SLE L0\n");
                                increment = true;
                            }
                            opPlus = opMinus = false;
                        }
                        if (opPuta || opDijeli) {
                            bwr.write("        CALL " + (opPuta ? "MUL" : "DIV")+"\n");
                            opPuta = opDijeli = false;
                        }
                    }
                    if (nextNode.startsWith(IDN)) {
                        parts = nextNode.split("\\s+");
                    }
                    nextNode = syntaxTree.get(++i);
                    if (nextNode.equals(E_LISTA)) {
                        nextNode = syntaxTree.get(++i);
                        if (nextNode.equals(EPSILON)) {
                            /*if (!increment) {
                                bwr.write("        POP R0\n");
                                String var = rezIDN ? "rez" : currentVar;
                                String m = memory.get(var).split("\\s+")[0];
                                bwr.write("        STORE R0, (" + m + ")\n\n");
                            }*/
                        } else if (nextNode.startsWith(OP_PLUS)) {
                            opPlus = true;
                        } else {
                            opMinus = true;
                        }
                    }
                    if (nextNode.equals(T_LISTA)) {
                        nextNode = syntaxTree.get(++i);
                        if (nextNode.startsWith(OP_PUTA)) opPuta = true;
                        if (nextNode.startsWith(OP_DIJELI)) opDijeli = true;
                    }
                }
            } else {
                increment = false;
                inLoop = true;
                // second node is IDN
                nextNode = syntaxTree.get(++i);
                String[] parts = nextNode.split("\\s+");
                loopVar = parts[2]+"L";
                memory.putIfAbsent(loopVar, V + ++vCounter + "      DW 0");
                nextNode = syntaxTree.get(++i);
                boolean krOdFlag = false;
                boolean krDoFlag = false;
                String sign2 = "";
                while (!nextNode.equals(LISTA_NAREDBI)) {
                    if (nextNode.startsWith(P)) {
                        if (syntaxTree.get(i - 3).startsWith(KR_DO)) {
                            krDoFlag = true;
                        }
                        if (syntaxTree.get(i - 3).startsWith(KR_OD)) {
                            krOdFlag = true;
                        }
                        nextNode = syntaxTree.get(++i);
                    }
                    if (nextNode.startsWith(OP_MINUS)) sign2 = "-";
                    if (nextNode.startsWith(BROJ)) {
                        bwr.write("        MOVE %D "+sign2+nextNode.split("\\s+")[2]+", R0\n");
                        sign2 = "";
                        if (krOdFlag && !krDoFlag)
                            bwr.write("        STORE R0, ("+memory.get(parts[2]+"L").split("\\s+")[0]+")\n");
                        else {
                            bwr.write("        STORE R0, (DO)\n");
                            memory.putIfAbsent("bla", "DO      DW 0");
                        }
                    }
                    if (nextNode.startsWith(IDN)) {
                        String[] temp = nextNode.split("\\s+");
                        String mem = memory.get(temp[2]).split("\\s+")[0];
                        bwr.write("        LOAD R0, ("+mem+")\n");
                        bwr.write("        STORE R0, (DO)\n");
                        memory.putIfAbsent("bla", "DO      DW 0");
                        parts = nextNode.split("\\s+");
                    }
                    nextNode = syntaxTree.get(++i);
                }
            }
        }

        String m = memory.get("rez").split("\\s+")[0];
        bwr.write("        POP R0\n");
        bwr.write("        STORE R0, ("+m+")\n");
        bwr.write("        LOAD R6, ("+m+")\n");
        bwr.write("        HALT\n\n");
        bwr.write("MD_SGN  MOVE 0, R6\n"+
                        "        XOR R0, 0, R0\n"+
                        "        JP_P MD_TST1\n"+
                        "        XOR R0, -1, R0\n"+
                        "        ADD R0, 1, R0\n"+
                        "        MOVE 1, R6\n"+
                 "MD_TST1 XOR R1, 0, R1\n"+
                        "        JP_P MD_SGNR\n"+
                        "        XOR R1, -1, R1\n"+
                        "        ADD R1, 1, R1\n"+
                        "        XOR R6, 1, R6\n"+
                 "MD_SGNR RET\n\n"+
                 "MD_INIT POP R4\n"+
                        "        POP R3\n"+
                        "        POP R1\n"+
                        "        POP R0\n"+
                        "        CALL MD_SGN\n"+
                        "        MOVE 0, R2\n"+
                        "        PUSH R4\n"+
                        "        RET\n\n"+
                  "MD_RET  XOR R6, 0, R6\n"+
                        "        JP_Z MD_RET1\n"+
                        "        XOR R2, -1, R2\n"+
                        "        ADD R2, 1, R2\n"+
                 "MD_RET1 POP R4\n"+
                        "        PUSH R2\n"+
                        "        PUSH R3\n"+
                        "        PUSH R4\n"+
                        "        RET\n\n"+
                     "MUL     CALL MD_INIT\n"+
                        "        XOR R1, 0, R1\n"+
                        "        JP_Z MUL_RET\n"+
                        "        SUB R1, 1, R1\n"+
                   "MUL_1   ADD R2, R0, R2\n"+
                        "        SUB R1, 1, R1\n"+
                        "        JP_NN MUL_1\n"+
                 "MUL_RET CALL MD_RET\n"+
                        "        RET\n\n"+
                     "DIV     CALL MD_INIT\n"+
                        "        XOR R1, 0, R1\n"+
                        "        JP_Z DIV_RET\n"+
                   "DIV_1   ADD R2, 1, R2\n"+
                        "        SUB R0, R1, R0\n"+
                        "        JP_NN DIV_1\n"+
                        "        SUB R2, 1, R2\n"+
                 "DIV_RET CALL MD_RET\n"+
                        "        RET\n\n");
        for (String value : memory.values()) {
            bwr.write(value + "\n");
        }
        bwr.flush();
        bwr.close();
    }

    private static List<String> initSyntaxTreeFromStdin() {
        List<String> syntaxTree = new ArrayList<>();
        try (Scanner sc = new Scanner(System.in)) {
            while (sc.hasNextLine()) syntaxTree.add(sc.nextLine().trim());
        }
        return syntaxTree;
    }

}
