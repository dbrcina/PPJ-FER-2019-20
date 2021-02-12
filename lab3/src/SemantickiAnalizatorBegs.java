import java.util.*;

public class SemantickiAnalizator {

    private static final String NAREDBA_PRIDRUZIVANJA = "<naredba_pridruzivanja>";
    private static final String ZA_PETLJA = "<za_petlja>";
    private static final String LISTA_NAREDBI = "<lista_naredbi>";
    private static final String IDN = "IDN";
    private static final String P = "<P>";
    private static final String KR_AZ = "KR_AZ";
    private static final String KR_ZA = "KR_ZA";
    private static final String KR_OD = "KR_OD";
    private static final String KR_DO = "KR_DO";

    public static void main(String[] args) {
        List<String> syntaxTree = initSyntaxTreeFromStdin();

        Map<DeclarationToken, String> globalDeclarations = new HashMap<>();
        List<Map<DeclarationToken, String>> localDeclarations = new ArrayList<>();
        Map<DeclarationToken, String> initialMap = new HashMap<>();
        localDeclarations.add(initialMap);
        int loopCounter = 0;
        int counter = 0;
        List<String> output = new ArrayList<>();

        boolean inLoop = false;
        boolean error = false;

        for (int i = 0; i < syntaxTree.size(); i++) {
            String rootNode = syntaxTree.get(i);
            if (rootNode.startsWith(KR_AZ)) {
                if (i < syntaxTree.size() - 1) {
                    if (!syntaxTree.get(i + 1).startsWith(KR_ZA)) {
                        inLoop = false;
                        localDeclarations.get(loopCounter).clear();
                        loopCounter--;
                    } else loopCounter++;
                }
            }
            if (!rootNode.equals(NAREDBA_PRIDRUZIVANJA) && !rootNode.equals(ZA_PETLJA))
                continue;

            // skip key words NAREDBA_PRIDRUZIVANJA and ZA_PETLJA
            String nextNode = syntaxTree.get(++i);

            if (rootNode.equals(NAREDBA_PRIDRUZIVANJA)) {
                // first next node is IDN
                String[] parts = nextNode.split("\\s+");
                DeclarationToken dToken = new DeclarationToken(parts[1], parts[2]);
                nextNode = syntaxTree.get(++i);
                while (!nextNode.equals(LISTA_NAREDBI)) {
                    if (nextNode.startsWith(P)) nextNode = syntaxTree.get(++i);
                    if (nextNode.startsWith(IDN)) {
                        parts = nextNode.split("\\s+");
                        DeclarationToken tempToken = new DeclarationToken(parts[1], parts[2]);
                        if (!globalDeclarations.containsKey(tempToken) && localDeclarations.stream().noneMatch(m -> m.containsKey(tempToken))) {
                            output.add("err " + parts[1] + " " + parts[2]);
                            error = true;
                            break;
                        }
                        String declarationRow;
                        if (localDeclarations.stream().anyMatch(m -> m.containsKey(tempToken))) {
                            declarationRow = localDeclarations.stream()
                                    .filter(m -> m.containsKey(tempToken))
                                    .findFirst().get().get(tempToken);
                        } else {
                            declarationRow = globalDeclarations.get(tempToken);
                        }
                        OutputToken outputToken = new OutputToken(
                                parts[1], // usage row
                                declarationRow, // declaration row
                                parts[2]); // unit
                        output.add(outputToken.toString());
                    }
                    nextNode = syntaxTree.get(++i);
                }
                if (error) break;
                // check scope
                if (!globalDeclarations.containsKey(dToken)) {
                    if (inLoop) {
                        if (!localDeclarations.get(loopCounter).containsKey(dToken))
                            localDeclarations.get(loopCounter).put(dToken, parts[1]);
                    } else globalDeclarations.put(dToken, parts[1]);
                }
            } else {
                counter++;
                if (counter > 1) loopCounter++;
                if (loopCounter > 0) localDeclarations.add(new HashMap<>());
                inLoop = true;
                // second node is IDN
                nextNode = syntaxTree.get(++i);
                String[] parts = nextNode.split("\\s+");
                DeclarationToken dToken = new DeclarationToken(parts[1], parts[2]);
                if (!localDeclarations.get(loopCounter).containsKey(dToken))
                    localDeclarations.get(loopCounter).put(dToken, parts[1]);
                nextNode = syntaxTree.get(++i);
                boolean krOdFlag = false;
                boolean krDoFlag = false;
                while (!nextNode.equals(LISTA_NAREDBI)) {
                    if (nextNode.startsWith(P)) {
                        if (syntaxTree.get(i - 3).startsWith(KR_DO)) krDoFlag = true;
                        if (syntaxTree.get(i - 3).startsWith(KR_OD)) krOdFlag = true;
                        nextNode = syntaxTree.get(++i);
                    }
                    if (nextNode.startsWith(IDN)) {
                        parts = nextNode.split("\\s+");
                        DeclarationToken tempToken = new DeclarationToken(parts[1], parts[2]);
                        if (!globalDeclarations.containsKey(tempToken) && localDeclarations.stream().noneMatch(m -> m.containsKey(tempToken))
                                || localDeclarations.get(loopCounter).containsKey(tempToken) && krDoFlag
                                || localDeclarations.get(loopCounter).containsKey(tempToken) && krOdFlag
                        ) {
                            output.add("err " + parts[1] + " " + parts[2]);
                            error = true;
                            break;
                        }
                        String declarationRow;
                        if (localDeclarations.stream().anyMatch(m -> m.containsKey(tempToken)) && !globalDeclarations.containsKey(tempToken)) {
                            declarationRow = localDeclarations.stream()
                                    .filter(m -> m.containsKey(tempToken))
                                    .findFirst().get().get(tempToken);
                        } else {
                            declarationRow = globalDeclarations.get(tempToken);
                        }
                        OutputToken outputToken = new OutputToken(
                                parts[1], // usage row
                                declarationRow, // declaration row
                                parts[2]); // unit
                        output.add(outputToken.toString());
                    }
                    nextNode = syntaxTree.get(++i);
                }
                if (error) break;
            }
        }

        output.forEach(System.out::println);
    }

    private static List<String> initSyntaxTreeFromStdin() {
        List<String> syntaxTree = new ArrayList<>();
        try (Scanner sc = new Scanner(System.in)) {
            while (sc.hasNextLine()) syntaxTree.add(sc.nextLine().trim());
        }
        return syntaxTree;
    }

    private static class OutputToken {
        String usageRow;
        String declarationRow;
        String unit;

        OutputToken(String usageRow, String declarationRow, String unit) {
            this.usageRow = usageRow;
            this.declarationRow = declarationRow;
            this.unit = unit;
        }

        @Override
        public String toString() {
            return usageRow + " " + declarationRow + " " + unit;
        }
    }

    private static class DeclarationToken {
        String row;
        String unit;

        DeclarationToken(String row, String unit) {
            this.row = row;
            this.unit = unit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DeclarationToken)) return false;
            DeclarationToken that = (DeclarationToken) o;
            return unit.equals(that.unit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(unit);
        }

    }

}
