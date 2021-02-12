import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class LeksickiAnalizator {

    private static final String ZA = "za";
    private static final String AZ = "az";
    private static final String OD = "od";
    private static final String DO = "do";
    private static final char EQUALS = '=';
    private static final char L_BRACKET = '(';
    private static final List<Character> OPERATORS = Arrays.asList('+', '-', '*', '/');
    private static final List<Character> BRACKETS = Arrays.asList('(', ')');
    private static final List<String> KEY_WORDS = Arrays.asList(ZA, AZ, OD, DO);

    private static List<Token> tokens;

    public static void main(String[] args) {
        tokens = new ArrayList<>();
        analize();
        tokens.forEach(System.out::println);
    }

    private static void analize() {
        try (Scanner sc = new Scanner(System.in)) {
            int row = 0;
            while (sc.hasNextLine()) {
                row++;
                String line = sc.nextLine();
                char[] chars = line.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    char c = chars[i];
                    if (c == '/' && i + 1 < chars.length) {
                        if (chars[i + 1] == '/')
                            break;
                    }
                    if (c == ' ' || c == '\t' || c == '\n') continue;
                    if (c == EQUALS) {
                        tokens.add(new Token(TokenType.OP_PRIDRUZI, String.valueOf(c), row));
                        continue;
                    }
                    if (BRACKETS.contains(c)) {
                        tokens.add(generateBracketToken(c, row));
                        continue;
                    }
                    if (OPERATORS.contains(c)) {
                        tokens.add(generateOperatorToken(c, row));
                        continue;
                    }
                    if (Character.isDigit(c)) {
                        int end = i + 1;
                        while (end < chars.length) {
                            c = chars[end];
                            if (!Character.isDigit(c)) break;
                            end++;
                        }
                        String value = new String(chars, i, end - i);
                        tokens.add(new Token(TokenType.BROJ, value, row));
                        i += end - i - 1;
                        continue;
                    }
                    if (Character.isLetter(c)) {
                        int end = i + 1;
                        while (end < chars.length) {
                            c = chars[end];
                            if (!Character.isDigit(c) && !Character.isLetter(c)) break;
                            end++;
                        }
                        String value = new String(chars, i, end - i);
                        if (KEY_WORDS.contains(value)) {
                            tokens.add(generateKeyWordToken(value, row));
                        } else {
                            tokens.add(new Token(TokenType.IDN, value, row));
                        }
                        i += end - i - 1;
                    }
                }

            }
        }
    }

    private static Token generateKeyWordToken(String value, int row) {
        TokenType type = null;
        switch (value) {
            case ZA:
                type = TokenType.KR_ZA;
                break;
            case AZ:
                type = TokenType.KR_AZ;
                break;
            case OD:
                type = TokenType.KR_OD;
                break;
            case DO:
                type = TokenType.KR_DO;
                break;
            default:
        }
        return new Token(type, value, row);
    }

    private static Token generateBracketToken(char c, int row) {
        TokenType type = c == L_BRACKET ? TokenType.L_ZAGRADA : TokenType.D_ZAGRADA;
        return new Token(type, String.valueOf(c), row);
    }

    private static Token generateOperatorToken(char c, int row) {
        TokenType type = null;
        switch (c) {
            case '+':
                type = TokenType.OP_PLUS;
                break;
            case '-':
                type = TokenType.OP_MINUS;
                break;
            case '*':
                type = TokenType.OP_PUTA;
                break;
            case '/':
                type = TokenType.OP_DIJELI;
                break;
            default:
        }
        return new Token(type, String.valueOf(c), row);
    }

    private enum TokenType {
        IDN,
        BROJ,
        OP_PRIDRUZI,
        OP_PLUS,
        OP_MINUS,
        OP_PUTA,
        OP_DIJELI,
        L_ZAGRADA,
        D_ZAGRADA,
        KR_ZA,
        KR_AZ,
        KR_OD,
        KR_DO;
    }

    private static class Token {
        TokenType type;
        String value;
        int row;

        Token(TokenType type, String value, int row) {
            this.type = type;
            this.value = value;
            this.row = row;
        }

        @Override
        public String toString() {
            return type + " " + row + " " + value;
        }
    }
}
