package lox;

// object that stores token information
public class Token {

    // each token has these properties
    final TokenType type;   // type of the token, see TokenType.jave for possible types
    final String lexeme;    // the value we read for this token
    final Object literal;   // the literal value of this token, what we store it as
    final int line;         // the line this token is on
  
    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }
  
    // lets us print formatted Tokens in this format
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
