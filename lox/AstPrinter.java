package lox;

// this is a heper class to debug and visualize expressions
// It implements the Visitor interface in the Expression class
// To me, this seems like a weird place to implement to visitor methods, but I believe its just for testing
class AstPrinter implements Expr.Visitor<String> {

    // this main method just lets us test test that we can infact print expressions
    public static void main(String[] args) {
        // the expression: -123 * (66.7 + 33.3)
        Expr expression = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.MINUS, "-", null, 1),
                new Expr.Literal(123)
            ),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(66.7),
                    new Token(TokenType.PLUS, "+", null, 1),
                    new Expr.Literal(33.3)
                )
            )
        );

        // sohuld output (* (- 123) (group 45.67))
        System.out.println(new AstPrinter().print(expression));
    }

    // accept the visitor, aka run the visit<Type>Expr() method
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    // accepts the name of the expression to parenthesize and 0 or more expressions
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
    
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");
    
        return builder.toString();
    }
}
