package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    TokenType.AND);
        keywords.put("class",  TokenType.CLASS);
        keywords.put("else",   TokenType.ELSE);
        keywords.put("false",  TokenType.FALSE);
        keywords.put("for",    TokenType.FOR);
        keywords.put("fun",    TokenType.FUN);
        keywords.put("if",     TokenType.IF);
        keywords.put("nil",    TokenType.NIL);
        keywords.put("or",     TokenType.OR);
        keywords.put("print",  TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super",  TokenType.SUPER);
        keywords.put("this",   TokenType.THIS);
        keywords.put("true",   TokenType.TRUE);
        keywords.put("var",    TokenType.VAR);
        keywords.put("while",  TokenType.WHILE);
    }
  
    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // we are at the beginning of the next lexeme
            start = current;
            scanToken();
        }
        
        // add a final End of file token to make the parser a bit cleaner
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            case '!':
                // if the character after the `!` is an equals sign, add the BANG_EQUAL token, else just add the BANG token. same logic applies for next 3 cases 
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '/':
                if (match('/')) {
                    // while the current character is not a newline and we're not at the end of the file, consume characters to ignore the comment
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    blockComment();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            case '\n':
                line++;
                break;
            case '"': string(); break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    // for user with identifier, AKA variable names and keywords
    private void identifier() {
        // while the current character is a number, letter, or underscore... consume it
        while (isAlphaNumeric(peek())) advance();
    
        // get the identifier out of the source string
        String text = source.substring(start, current);
        // lookup the type in the hashmap, this returns null if nothing is found
        TokenType type = keywords.get(text);
        // if the type is null, we have an identifier created by the user... a variable name
        if (type == null) type = TokenType.IDENTIFIER;
        // add the token of the type we found. We pass a single argument to addToken() since identifiers have no literal value 
        addToken(type);
    }

    private void number() {
        // while the current character is a digit, consume it
        while (isDigit(peek())) advance();
    
        // Look for the decimal point. If the current character is a `.` and the next character is a number we consume the `.`
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();
    
            // consume all the digits after the decimal
            while (isDigit(peek())) advance();
        }
    
        // add the token of type number. make the literal type a floating point number (even if its an int) since Lox only has a "number" type
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {

        // custom change I made. before looping, get the line so errors say the line the error starts on rather than ends on
        // i think this is useful because the line will report the last line of the file everytime otherwise
        int startLine = line;

        // while the current character does not equal a double quote and not at the end of the source code
        while (peek() != '"' && !isAtEnd()) {
            // if the current character is a newline, increment the line counter
            if (peek() == '\n') line++;
            // consume the current character
            advance();
        }

        // if we reached the end, that means we never found the last double quote needed to end the string, so we have an error
        if (isAtEnd()) {
            Lox.error(startLine, "Unterminated string.");
            return;
        }

        // Conmsume the closing double quote
        advance();

        // Trim the quotes surrounding the string
        String value = source.substring(start + 1, current - 1);
        // add a new String token with the type STRING and literal value of the `value` we found above
        addToken(TokenType.STRING, value);
    }

    private void blockComment() {

        // see the String method `startLine` comment
        int startLine = line;

        // while the next 2 characters aren't `*/` and we're not at the end of the file
        while (!(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
            // if the next character is a new line, indrement the line counter, then advance
            if (peek() == '\n') line++;
            // consume the character in the comment
            advance();
        }

        // if we reached the end of the file before finding the closing `*/`, we have an error
        if (isAtEnd()) {
            Lox.error(startLine, "Unfinished comment");
            return;
        }

        // advance twice to consume the last two characters of the comment `*/`
        advance();
        advance();
    }

    // returns true if the current character matches the `expected` character
    // also incremenets current, so the "matched" character gets consumed
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
    
        current++;
        return true;
    }

    // returns the character we are currently on
    // if we are at the end of the file, returns the null terminator
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // returns the character after the current character
    // if the character after the current doesn't exist (because the file ends) we return the null terminator
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    // returns true if `c` is a lowercase/capital letter OR an underscore
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
                c == '_';
    }
    
    // returns true if `c` is a letter, number, or underscore
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // returns true if `c` is 0-9, we don't use Java's built in isDigit because it has some special behaviors we don't want
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // if the character we're currently examining is >= the length of the source String, we have reached the end of the file
    // keep in mind we consume the `source` String 1 character at a time liek the source String is an array
    // we start at source[0] and end at source[length -1].
    private boolean isAtEnd() {
        return current >= source.length();
    }

    // "consumes" the current character and returns it
    // by consumes we mean: increments the `current` counter and moves to the next character so we can evaluate it
    private char advance() {
        return source.charAt(current++);
    }

    // to add a token that doesn't have a literal value. For tokens like PAREN or PRINT 
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        // get the pure textual representation of this token, this is the substring in the source file from the tokens start up to the current character
        String text = source.substring(start, current);
        // add a new Token to the tokens list. The literal value for the string "Hello" will be Hello, but the lexeme is "Hello" with the quotes
        tokens.add(new Token(type, text, literal, line));
    }
}
