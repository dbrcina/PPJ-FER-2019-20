import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class FRISCGenerator {

    public static void main(String[] args) {
        new Test();
    }
}

class Test{

    private BufferedReader reader;
    private List<Map<String, String>> varList;
    private String currentLine;
    private int lineNumber;
    private int forDepth;
    private String issue;
    private PrintWriter writer;
    private Map<String, String> variables = new LinkedHashMap<>();
    private int counter = 0;
    private boolean mulDiv = false;
    private boolean addOperation = false;
    private boolean subOperation = false;
    private boolean mulOperation = false;
    private boolean divOperation = false;
    private String varSign = "";
    private boolean writeIncrement = false;
    private boolean labelNotAdded = true;
    private String varPetlja = "";

    Test(){
        try {
//	          reader = new BufferedReader(new FileReader("C:\\Users\\Ante.MIREO\\Downloads\\primjer[1]\\Test.in"));
            reader = new BufferedReader(new InputStreamReader(System.in));
            writer = new PrintWriter(Files.newOutputStream(new File("a.frisc").toPath()));
            writer.println("    MOVE %D 40000, r7");
            varList = new LinkedList<>();
            varList.add(new HashMap<>());
            lineNumber = 0;
            forDepth = 0;
            issue = null;
            boolean pass = true;

            if (!readNextLine()) pass = false;

            while(getNextLine() != null && pass) {
                switch(currentLine) {
                    case "<naredba_pridruzivanja>":
                        if (!naredbaPridruzivanja()) pass = false;
                        break;
                    case "<za_petlja>":
                        if (!zaPetlja()) pass = false;
                        break;
                    default:
                        continue;
                }
            }

            if(!pass) {
                if (issue != null) {
                    //System.out.println("err " + lineNumber + " " + issue);
                }
            }
        } catch (NullPointerException | IOException exc){
            exc.printStackTrace();
        }
        //System.err.println(varList);
        writer.println("    POP r0");
        writer.println("    STORE r0, ("+variables.get("rez")+")");
        writer.println("    LOAD r6, ("+variables.get("rez")+")");
        writer.println("    HALT\n");

        for (String value : variables.values()) {
            writer.println(value + "  DW 0");
        }
        writer.println("DO  DW 0");
        writer.println();

        if (mulDiv) writeMulDiv();

        writer.flush();
        writer.close();
    }

    private boolean zaPetlja() {
        varList.add(++forDepth, new HashMap<>());
        if(!readNextLines(2)) return false;

        String k = readVarName()+"PETLJA";
        varPetlja = k;
        String v = String.valueOf(lineNumber);
        variables.computeIfAbsent(k, k1 -> "V" + counter);
        counter = counter + 1;
        writeIncrement = true;

        int lineNoComp = lineNumber;
        boolean jobNotDone = true;


        boolean OD = false;
        boolean DO = false;
        do {

            if (!readNextLine()) return false;

            if (lineNoComp == lineNumber && currentLine.contains("KR_OD")) {
                OD = true;
            }
            if (lineNoComp == lineNumber && currentLine.contains("KR_DO")) {
                DO = true;
            }

            if (lineNoComp == lineNumber && currentLine.contains("IDN")) {
                String var = readVarName();
                writer.println("    LOAD r0, ("+variables.get(var)+")");
                if (OD) {
                    writer.println("    STORE r0, ("+variables.get(k)+")");
                    OD = false;
                }
                if (DO) {
                    writer.println("    STORE r0, (DO)");
                    DO = false;
                }
                if(var.equals(k)) {
                    issue = var;
                    return false;
                }
            }

            if (currentLine.contains("OP_MINUS")) varSign = "-";

            if (lineNoComp == lineNoComp && currentLine.contains("BROJ")) {
                String var = readVarName();
                writer.println("    MOVE %D "+varSign+var+", r0");
                varSign = "";
                if (OD) {
                    writer.println("    STORE r0, ("+variables.get(k)+")");
                    OD = false;
                }
                if (DO) {
                    writer.println("    STORE r0, (DO)");
                    DO = false;
                }
            }

            if (lineNumber != lineNoComp && jobNotDone) {
                varList.get(forDepth).put(k, v);
                jobNotDone = false;
            }

            switch(currentLine) {
                case "<naredba_pridruzivanja>":
                    if (!naredbaPridruzivanja()) return false;
                    break;
                case "<za_petlja>":
                    if (!zaPetlja()) return false;
                    break;
                default:
                    if (currentLine.contains("IDN")) {
                        if (!varCheck()) return false;
                    }
            }
        } while (!currentLine.startsWith("KR_AZ"));

        forDepth--;

        return true;
    }

    private boolean naredbaPridruzivanja() {
        if (!readNextLine()) return false;
        String k = readVarName();
        String v = String.valueOf(lineNumber);
        variables.computeIfAbsent(k, k1 -> "V" + counter);
        counter = counter + 1;

        if(!readNextLine()) return false;
        int rowNo = lineNumber;
        while(rowNo == lineNumber) {
            if (!readNextLine()) return false;

            if (currentLine.contains("<P>")) {
                readNextLine();
                if (currentLine.contains("OP_MINUS")) varSign = "-";
            }

            if (currentLine.contains("BROJ") || currentLine.contains("IDN")) {
                if (currentLine.contains("BROJ")) {
                    writer.println("    MOVE %D " + varSign + readVarName() + ", r0");
                    writer.println("    STORE r0, ("+variables.get(k)+")");
                    varSign = "";
                } else {
                    String varName = readVarName();
                    String var = !varPetlja.isEmpty() && varName.startsWith(varPetlja.substring(0,1)) ?
                            varPetlja : varName;
                    if (writeIncrement && labelNotAdded) {
                        writer.print("L0");
                        labelNotAdded = false;
                    }
                    writer.println("    LOAD r0, ("+variables.get(var)+")");
                }
                writer.println("    PUSH r0");
                if (addOperation || subOperation) {
                    // check for association e.g rez = x + y*x rather than rez = y*x + x
                    // skip to <T_lista>
                    // skip to first child od <T_lista>
                    readNextLines(2);
                    if (!currentLine.contains("$")) {
                        if (currentLine.contains("OP_PUTA")) mulOperation = true;
                        if (currentLine.contains("OP_DIJELI")) divOperation = true;
                        while(!currentLine.contains("<P>")) readNextLine();
                        // skip to IDN or BROJ
                        readNextLine();
                        if (currentLine.contains("BROJ")) {
                            writer.println("    MOVE %D "+readVarName()+", r0");
                        } else {
                            writer.println("    LOAD r0, ("+variables.get(readVarName())+
                                    ")");
                        }
                        writer.println("    PUSH r0");
                        mulDivOperation();
                    }
                    addSubOperation(k);
                    if (writeIncrement) incrementCheck();
                }
                if (mulOperation || divOperation) mulDivOperation();
            }

            if (currentLine.contains("<E_lista>")) {
                readNextLine();
                if (currentLine.contains("OP_PLUS")) addOperation = true;
                if (currentLine.contains("OP_MINUS")) subOperation = true;
            }

            if (currentLine.contains("<T_lista")) {
                readNextLine();
                if (currentLine.contains("OP_PUTA")) mulOperation = true;
                if (currentLine.contains("OP_DIJELI")) divOperation = true;
            }

        }

        boolean noKey = true;
        for(int depthLevel = forDepth; depthLevel > 0; depthLevel--) {
            if (varList.get(depthLevel).containsKey(k)) noKey = false;
        }

        if (noKey) varList.get(forDepth).put(k, v);

        return true;
    }

    private void incrementCheck() {
        writer.println("    LOAD R0, ("+variables.get(varPetlja)+")");
        writer.println("    ADD R0, 1, R0");
        writer.println("    STORE R0, ("+variables.get(varPetlja)+")");
        writer.println("    LOAD R0, ("+variables.get(varPetlja)+")");
        writer.println("    LOAD R1, (DO)");
        writer.println("    CMP R0, R1");
        writer.println("    JP_SLE L0");
        writeIncrement = false;
    }

    private boolean varCheck() {
        String k = readVarName();

        boolean haveKey = false;
        int depthLevel;
        for (depthLevel = forDepth; depthLevel >= 0; depthLevel--) {
            if(varList.get(depthLevel).containsKey(k)) {
                haveKey = true;
                break;
            }
        }

//		System.err.println("var check "+k);
        if (haveKey) {
            //System.out.println(String.valueOf(lineNumber) + " " + varList.get(depthLevel).get(k) + "
            // " + k);
        } else {
            issue = k;
            return false;
        }

        return true;
    }

    private boolean readNextLines(int n) {
        for (int i = 0; i < n; i++) {
            if(!readNextLine()) return false;
        }
        return true;
    }

    private String readVarName() {
        return currentLine.split(" ")[2];
    }

    private boolean readNextLine() {
        try {
            if ((currentLine = reader.readLine()) != null) {
                currentLine = currentLine.trim();
//                System.err.println("read "+currentLine);
                if (currentLine.equals("<lista_naredbi>")) {
                    lineNumber++;
                    readNextLine();
                }
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getNextLine() {
        currentLine = null;
        if (!readNextLine()) return null;
        return currentLine;
    }

    private void addSubOperation(String k) {
        writer.println("    POP r1");
        writer.println("    POP r0");
        writer.println("    "+(addOperation ? "ADD" : "SUB")+ " r0, r1, r2");
        writer.println("    PUSH r2");
        writer.println("    STORE r2, ("+variables.get(k)+")");
        addOperation = subOperation = false;
        /*if (!k.equals("rez")) {
            writer.println("    STORE r2, ("+variables.get(k)+")");
        }*/
    }

    private void mulDivOperation() {
        writer.println("    CALL " + (mulOperation ? "MUL" : "DIV"));
        mulOperation = divOperation = false;
        mulDiv = true;
    }

    private void writeMulDiv() {
        writer.println("MD_SGN MOVE 0, R6\n"+
                "   XOR R0, 0, R0\n"+
                "   JP_P MD_TST1\n"+
                "   XOR R0, -1, R0\n"+
                "   ADD R0, 1, R0\n"+
                "   MOVE 1, R6\n"+
                "MD_TST1 XOR R1, 0, R1\n"+
                "   JP_P MD_SGNR\n"+
                "   XOR R1, -1, R1\n"+
                "   ADD R1, 1, R1\n"+
                "   XOR R6, 1, R6\n"+
                "MD_SGNR RET\n");
        writer.println("MD_INIT POP R4\n"+
                "   POP R3\n"+
                "   POP R1\n"+
                "   POP R0\n"+
                "   CALL MD_SGN\n"+
                "   MOVE 0, R2\n"+
                "   PUSH R4\n"+
                "   RET\n");
        writer.println("MD_RET XOR R6, 0, R6\n"+
                "   JP_Z MD_RET1\n"+
                "   XOR R2, -1, R2\n"+
                "   ADD R2, 1, R2\n"+
                "MD_RET1 POP R4\n"+
                "   PUSH R2\n"+
                "   PUSH R3\n"+
                "   PUSH R4\n"+
                "   RET\n");
        writer.println("MUL CALL MD_INIT\n"+
                "   XOR R1, 0, R1\n"+
                "   JP_Z MUL_RET\n"+
                "   SUB R1, 1, R1\n"+
                "MUL_1  ADD R2, R0, R2\n"+
                "   SUB R1, 1, R1\n"+
                "   JP_NN MUL_1\n"+
                "MUL_RET CALL MD_RET\n"+
                "   RET\n");
        writer.println("DIV CALL MD_INIT\n"+
                "   XOR R1, 0, R1\n"+
                "   JP_Z DIV_RET\n"+
                "DIV_1  ADD R2, 1, R2\n"+
                "   SUB R0, R1, R0\n"+
                "   JP_NN DIV_1\n"+
                "   SUB R2, 1, R2\n"+
                "DIV_RET CALL MD_RET\n"+
                "   RET\n");
    }

}
