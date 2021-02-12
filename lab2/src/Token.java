public class Token {

    private TokenType type;
    private String row;
    private String value;

    public Token(TokenType type, String row, String value) {
        this.type = type;
        this.row = row;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + " " + row + " " + value;
    }

}
