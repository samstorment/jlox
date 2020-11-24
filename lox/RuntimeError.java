package lox;

public class RuntimeError extends RuntimeException {

    // track the token that caused the runtime error
    final Token token;

    // RuntimeException with the passed in message
    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
