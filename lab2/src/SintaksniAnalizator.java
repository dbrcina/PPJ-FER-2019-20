import java.util.*;

public class SintaksniAnalizator {

    private static final String EPSILON = "$";
    private static final String IDN = "IDN";
    private static final String BROJ = "BROJ";
    private static final String OP_PLUS = "OP_PLUS";
    private static final String OP_MINUS = "OP_MINUS";
    private static final String L_ZAGRADA = "L_ZAGRADA";
    private static final String KR_ZA = "KR_ZA";

    private static int currentIndex;
    private static Token currentToken;
    private static List<Token> tokens;
    private static List<String> outputTree;

    public static void main(String[] args) {
        currentIndex = 0;
        outputTree = new ArrayList<>();
        tokens = readFromLexer();
        analizeInput();
        outputTree.forEach(System.out::println);
    }

    private static void analizeInput() {
        try {
            program();
        } catch (SyntaxException e) {
            outputTree.clear();
            outputTree.add(e.getMessage());
        }
    }

    // <program> -> <lista_naredbi> {IDN, KR_ZA, input_terminator}
    private static void program() {
        outputTree.add("<program>");
        if (tokens.isEmpty()) {
            listaNaredbi(true, 1);
            return;
        }
        currentToken = tokens.get(currentIndex++);
        // check terminal signs
        checkStartsWith(Arrays.asList(IDN, KR_ZA));
        listaNaredbi(false, 1);
    }

    // <lista_naredbi> -> epsilon {KR_AZ, input_terminator}
    // <lista_naredbi> -> <naredba> <lista_naredbi> {IDN, KR_ZA}
    private static void listaNaredbi(boolean empty, int offset) {
        outputTree.add(nBlanks(offset) + "<lista_naredbi>");
        // epsilon production
        if (empty || currentToken.getType() == TokenType.KR_AZ) {
            addEpsilon(offset + 1);
            return;
        }
        // check terminal signs
        checkStartsWith(Arrays.asList(IDN, KR_ZA));
        // call naredba
        naredba(offset + 1);
        // call listaNaredbi recursively
        listaNaredbi(currentIndex == tokens.size(), offset + 1);
    }

    // <naredba> -> <naredba_pridruzivanja> {IDN}
    // <naredba> -> <za_petlja> {KR_ZA}
    // terminal signs are already checked in listaNaredbi
    private static void naredba(int offset) {
        outputTree.add(nBlanks(offset) + "<naredba>");
        if (currentToken.getType() == TokenType.IDN) {
            naredbaPridruzivanja(offset + 1);
        } else {
            zaPetlja(offset + 1);
        }
    }

    // <naredba_pridruzivanja> -> IDN OP_PRIDRUZI <E> {IDN}
    // terminal signs are already checked in listNaredbi
    private static void naredbaPridruzivanja(int offset) {
        outputTree.add(nBlanks(offset) + "<naredba_pridruzivanja>");
        outputTree.add(nBlanks(offset + 1) + currentToken);
        // read next token
        currentToken = tokens.get(currentIndex++);
        // validate token
        if (currentToken.getType() != TokenType.OP_PRIDRUZI) {
            throw new SyntaxException("err " + currentToken);
        }
        outputTree.add(nBlanks(offset + 1) + currentToken);
        // read next token for E is possible
        try {
            currentToken = tokens.get(currentIndex++);
        } catch (IndexOutOfBoundsException e) {
            throw new SyntaxException("err kraj");
        }

        E(offset + 1);
    }

    // <E> -> <T> <E_lista> {IDN, BROJ, OP_PLUS, OP_MINUS, L_ZAGRADA}
    private static void E(int offset) {
        // check terminal signs
        checkStartsWith(Arrays.asList(IDN, BROJ, OP_PLUS, OP_MINUS, L_ZAGRADA));
        outputTree.add(nBlanks(offset) + "<E>");
        T(offset + 1);
        ELista(offset + 1);
    }

    // <T> -> <P> <T_lista> {IDN, BROJ, OP_PLUS, OP_MINUS, L_ZAGRADA}
    private static void T(int offset) {
        // check terminal signs
        checkStartsWith(Arrays.asList(IDN, BROJ, OP_PLUS, OP_MINUS, L_ZAGRADA));
        outputTree.add(nBlanks(offset) + "<T>");
        P(offset + 1);
        TLista(offset + 1);
    }

    // <P> -> OP_PLUS <P> {OP_PLUS}
    // <P> -> OP_MINUS <P> {OP_MINUS}
    // <P> -> L_ZAGRADA <E> D_ZAGRADA {L_ZAGRADA}
    // <P> -> IDN {IDN}
    // <P> -> BROJ {BROJ}
    private static void P(int offset) {
        outputTree.add(nBlanks(offset) + "<P>");
        outputTree.add(nBlanks(offset + 1) + currentToken);
        switch (currentToken.getType()) {
            // check OP_PLUS and OP_MINUS
            case OP_PLUS:
            case OP_MINUS:
                // read next token for P recursion
                currentToken = tokens.get(currentIndex++);
                P(offset + 1);
                break;
            case L_ZAGRADA:
                // read next token for E
                currentToken = tokens.get(currentIndex++);
                E(offset + 1);
                if (currentToken.getType() != TokenType.D_ZAGRADA) {
                    throw new SyntaxException("err " + currentToken);
                }
                outputTree.add(nBlanks(offset + 1) + currentToken);
            case IDN:
            case BROJ:
                // read next token if possible
                if (currentIndex != tokens.size()) {
                    currentToken = tokens.get(currentIndex++);
                }
                break;
            default:
                throw new SyntaxException("err " + currentToken);
        }
    }

    // <E_lista> -> epsilon {IDN, KR_ZA, KR_DO, KR_AZ, D_ZAGRADA, input_terminator}
    // <E_lista> -> OP_PLUS <E> {OP_PLUS}
    // <E_lista> -> OP_MINUS <E> {OP_MINUS}
    private static void ELista(int offset) {
        outputTree.add(nBlanks(offset) + "<E_lista>");
        // epsilon production
        if (currentIndex == tokens.size()) {
            addEpsilon(offset + 1);
            return;
        }
        switch (currentToken.getType()) {
            case OP_MINUS:
            case OP_PLUS:
                outputTree.add(nBlanks(offset + 1) + currentToken);
                // read next token for E
                currentToken = tokens.get(currentIndex++);
                E(offset + 1);
                break;
            case IDN:
            case KR_ZA:
            case KR_DO:
            case KR_AZ:
            case D_ZAGRADA:
                addEpsilon(offset + 1);
                break;
            default:
                throw new SyntaxException("err " + currentToken);
        }

    }

    // <T_lista> -> epsilon {IDN, KR_ZA, KR_DO, KR_AZ, OP_PLUS, OP_MINUS, D_ZAGRADA, input_terminator}
    // <T_lista> -> OP_PUTA <E> {OP_PUTA}
    // <T_lista> -> OP_DIJELI <E> {OP_DIJELI}
    private static void TLista(int offset) {
        outputTree.add(nBlanks(offset) + "<T_lista>");
        // epsilon production
        if (currentIndex == tokens.size()) {
            addEpsilon(offset + 1);
            return;
        }
        switch (currentToken.getType()) {
            case OP_PUTA:
            case OP_DIJELI:
                outputTree.add(nBlanks(offset + 1) + currentToken);
                // read next token for T
                currentToken = tokens.get(currentIndex++);
                T(offset + 1);
                break;
            case IDN:
            case KR_ZA:
            case KR_DO:
            case KR_AZ:
            case OP_PLUS:
            case OP_MINUS:
            case D_ZAGRADA:
                addEpsilon(offset + 1);
                break;
            default:
                throw new SyntaxException("err " + currentToken);
        }
    }

    // <za_petlja> -> KR_ZA IDN KR_OD <E> KR_DO <E> <lista_naredbi> KR_AZ = {KR_ZA}
    private static void zaPetlja(int offset) {
        outputTree.add(nBlanks(offset) + "<za_petlja>");
        outputTree.add(nBlanks(offset + 1) + currentToken);
        currentToken = tokens.get(currentIndex++);
        if (currentToken.getType() != TokenType.IDN) {
            throw new SyntaxException("err " + currentToken);
        }
        outputTree.add(nBlanks(offset + 1) + currentToken);
        currentToken = tokens.get(currentIndex++);
        if (currentToken.getType() != TokenType.KR_OD) {
            throw new SyntaxException("err " + currentToken);
        }
        outputTree.add(nBlanks(offset + 1) + currentToken);
        // read next token for E
        currentToken = tokens.get(currentIndex++);
        E(offset + 1);
        if (currentToken.getType() != TokenType.KR_DO) {
            throw new SyntaxException("err " + currentToken);
        }
        outputTree.add(nBlanks(offset + 1) + currentToken);
        // read next token for E
        currentToken = tokens.get(currentIndex++);
        E(offset + 1);
        listaNaredbi(currentIndex == tokens.size(), offset + 1);
        if (currentToken.getType() != TokenType.KR_AZ) {
            throw new SyntaxException("err " + currentToken);
        }
        outputTree.add(nBlanks(offset + 1) + currentToken);
        // read next token if possible
        if (currentIndex != tokens.size()) {
            currentToken = tokens.get(currentIndex++);
        }
    }

    private static void addEpsilon(int offset) {
        outputTree.add(nBlanks(offset) + EPSILON);
    }

    private static String nBlanks(int offset) {
        StringBuilder blanks = new StringBuilder();
        for (int i = 0; i < offset; i++) {
            blanks.append(" ");
        }
        return blanks.toString();
    }

    private static void checkStartsWith(List<String> validTypes) {
        TokenType type = currentToken.getType();
        if (!validTypes.contains(type.toString())) {
            throw new SyntaxException("err " + currentToken);
        }
    }

    private static List<Token> readFromLexer() {
        List<Token> tokens = new ArrayList<>();
        try (Scanner sc = new Scanner(System.in)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                String[] parts = line.split("\\s++");
                tokens.add(new Token(
                        TokenType.valueOf(parts[0]),
                        parts[1],
                        parts[2])
                );
            }
        }
        return tokens;
    }

}
