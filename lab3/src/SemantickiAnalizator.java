import java.util.*;

public class SemantickiAnalizator {

    private static Collection<String> inputSyntaxTree;

    // map for global declaration
    private static Map<String, String> globalDeclaration;
    // list of local declarations
    // local declaration is a map where key is a unit and value is a row of declaration
    private static List<Map<String, String>> localDeclarations;
    // counter for current loop, starts with -1 and every loop will increment it
    private static int loopCounter;

    private static boolean inLoop;

    // collection of results
    private static Collection<String> result;

    public static void main(String[] args) {
        // read syntax tree from stdin
        initInputSyntaxTree();
        // generate output from semantic analysis
        semanticAnalysis();
        // print results to stdout
        result.forEach(System.out::println);
    }

    private static void initInputSyntaxTree() {
        inputSyntaxTree = new ArrayList<>();
        try (Scanner sc = new Scanner(System.in)) {
            while (sc.hasNextLine()) {
                inputSyntaxTree.add(sc.nextLine().trim());
            }
        }
    }

    private static void semanticAnalysis() {
        globalDeclaration = new HashMap<>();
        localDeclarations = new ArrayList<>();
        loopCounter = -1;
        inLoop = false;
        result = new ArrayList<>();
        Iterator<String> it = inputSyntaxTree.iterator();
        while (it.hasNext()) {
            String node = it.next();
            // end of loop
            if (node.startsWith("KR_AZ")) {
                localDeclarations.remove(loopCounter--);
                if (localDeclarations.isEmpty()) inLoop = false;
            }
            if (!node.equals("<naredba_pridruzivanja>") && !node.equals("<za_petlja>"))
                continue;
            boolean end;
            if (node.equals("<naredba_pridruzivanja>")) end = naredbaPridruzivanja(it);
            else end = zaPetlja(it, new HashMap<>());
            if (end) break;
        }
    }

    private static boolean naredbaPridruzivanja(Iterator<String> it) {
        // main declaration
        String declaration = it.next();

        String value;
        while (!(value = it.next()).equals("<lista_naredbi>")) {
            // left side
            if (!value.startsWith("IDN")) continue;
            String[] parts = value.split("\\s+");
            boolean end = checkDeclaration(parts, false);
            if (end) return true;
            String declarationRow = findDeclarationRow(parts[2]);
            if (declarationRow == null) {
                result.add("err " + parts[1] + " " + parts[2]);
                return true;
            }
            result.add(parts[1] + " " + declarationRow + " " + parts[2]);
        }

        // check left side declaration
        return checkDeclaration(declaration.split("\\s+"), true);
    }

    private static boolean zaPetlja(Iterator<String> it, Map<String, String> localDeclaration) {
        inLoop = true;
        loopCounter++;
        localDeclarations.add(localDeclaration);

        // skip KR_ZA keyword
        it.next();
        // loop declaration
        String[] parts = it.next().split("\\s+"); // [IDN, row, unit]
        localDeclaration.put(parts[2], parts[1]);

        // KR_OD and KR_DO expressions
        String value;
        while (!(value = it.next()).equals("<lista_naredbi>")) {
            if (!value.startsWith("IDN")) continue;
            parts = value.split("\\s+");
            if (localDeclaration.containsKey(parts[2])) {
                result.add("err " + parts[1] + " " + parts[2]);
                return true;
            }
            String declarationRow = findDeclarationRow(parts[2]);
            if (declarationRow == null) {
                result.add("err " + parts[1] + " " + parts[2]);
                return true;
            }
            result.add(parts[1] + " " + declarationRow + " " + parts[2]);
        }
        return false;
    }

    private static boolean checkDeclaration(String[] parts, boolean leftSide) {
        if (!globalDeclaration.containsKey(parts[2])) {
            String declarationRow = findDeclarationRow(parts[2]);
            if (declarationRow == null) {
                if (!leftSide) {
                    result.add("err " + parts[1] + " " + parts[2]);
                    return true;
                }
                declarationRow = parts[1];
            }
            if (inLoop) {
                if (!localDeclarations.get(loopCounter).containsKey(parts[2])) {
                    localDeclarations.get(loopCounter).put(parts[2], declarationRow);
                }
            } else globalDeclaration.put(parts[2], declarationRow);
        }
        return false;
    }

    private static String findDeclarationRow(String unit) {
        String declarationRow = null;
        for (int i = localDeclarations.size() - 1; i > -1; i--) {
            if (declarationRow != null) break;
            //if (i == loopCounter) continue;
            Map<String, String> localDeclarationMap = localDeclarations.get(i);
            String tempRow = localDeclarationMap.get(unit);
            if (tempRow != null) {
                declarationRow = tempRow;
            }
        }
        if (declarationRow == null) {
            declarationRow = globalDeclaration.get(unit);
        }
        return declarationRow;
    }

}
