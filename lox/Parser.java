package lox;

import java.util.List;

public class Parser {

    // a custome error to throw when the Parser encounters a syntax error
    private static class ParseError extends RuntimeException {}

    // the tokens to parse
    private final List<Token> tokens;
    // the token index we're currently on in the List
    private int current = 0;
    
    // constructor accepts a list of tokens (generated by the scanner) as input
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    // currently parses a single expression and returns the expression, will add more later
    Expr parse() {
        try {
            // try to parse the expression and return it
            return expression();
        } catch (ParseError error) {
            // return null if a syntax error occurs. This means once the first syntax error is found, we can no longer return a valid syntax tree
            return null;
        }
    }

    // expression     → equality ;
    private Expr expression() {
        // expressions always expand to equality
        return equality();
    }
    
    // equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    private Expr equality() {

        // the expr we return MUST expand to at least one comparison
        Expr expr = comparison();
        
        // then there can be 0 or more `!=` or '==' followed by another comparison
        // if there are 0, this loop never gets entered and we just return the comparison
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        
        return expr;
    }
    
    // comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    // same rules as equality, but our while loop matches different tokens and comparisons expand to term
    private Expr comparison() {

        Expr expr = term();
        
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        
        return expr;
    }
    
    // term           → factor ( ( "-" | "+" ) factor )* ;
    // same rules as equality but match different tokens and expand to factor
    private Expr term() {
        Expr expr = factor();

        while(match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // factor         → unary ( ( "/" | "*" ) unary )* ;
    // same rules as equality but match different tokens and expand to unary
    private Expr factor() {
        Expr expr = unary();

        while(match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // unary          → ( "!" | "-" ) unary | primary ;
    private Expr unary() {

        // if the current current token matches '!' or '-'
        if (match(TokenType.BANG, TokenType.MINUS)) {
            // match consumes the '!'/'-' so we need to grab it
            Token operator = previous();
            // the right side of the unary is another unary
            Expr right = unary();
            // return a new unary using the operator and the right side expression
            return new Expr.Unary(operator, right);
        }

        // unary always finishes with a primary because that is the only case that breaks the recursion
        return primary();
    }

    // primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
    private Expr primary() {

        // if the TokenType is FALSE, TRUE, or NIL we can immediately return corresponding Literal values
        if (match(TokenType.FALSE)) { return new Expr.Literal(false); }
        if (match(TokenType.TRUE)) { return new Expr.Literal(true); }
        if (match(TokenType.NIL)) { return new Expr.Literal(null); }

        // if the TokenType is a number or STRING, we can return the literal value of the number/string
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            // we return the previous token's literal value. we have to use previous because match consumed the string/number token
            return new Expr.Literal(previous().literal);
        }

        // if the TokenType is a left parenthesis we have the start of a grouped expression
        if (match(TokenType.LEFT_PAREN)) {
            // get the expression inside of the parentheses
            Expr expr = expression();
            // consume the closing parenthesis. We tell consume that we're expecting the next token to be a right parenthesis
            // and we provide an error message if the next token is NOT the token we expect
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            // return a new grouping with the expression, this line will not be reached if consume fails since consume can throw an error
            return new Expr.Grouping(expr);
        }

        // throw an error at the current token with the error message "Expect Expression."
        throw error(peek(), "Expect Expression.");
    }

    // START HELPERS
    private boolean match(TokenType... types) {
        // loop through all of the types passed in
        for (TokenType type : types) {
            // if the type of the current token matches a type we passed as an argument
            if (check(type)) {
                // consume the token
                advance();
                // return true, indicating we matched on of our types
                return true;
            }
        }
        // return false if we had no matches
        return false;
    }

    // consumes the current token and returns it if the current token matches the passed in type
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        // throws an error wihth the passed in message if there was no match
        throw error(peek(), message);
    }

    //returns true if the passed in type matches the current token's type. Return false if we're at the end of the list of tokens
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // consumes the current token and returns it. Does not consume it if we are at end of token list
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    // returns true if the current token is the End of File token. AKA the last token in the list
    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }
    
    // returns the current token
    private Token peek() {
        return tokens.get(current);
    }
    
    // returns the token directly before the current token
    private Token previous() {
        return tokens.get(current - 1);
    }

    // custom error that prints the problematic token, the line it is at, and a general message
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        // returns a new ParseError so we can throw it
        return new ParseError();
    }

    // We don't use this method yet, just built it up for now
    private void synchronize() {
        advance();
    
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default: break;
            }
            advance();
        }
    }
    
}
