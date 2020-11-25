package lox;

import java.util.List;
import lox.Expr.Binary;
import lox.Expr.Grouping;
import lox.Expr.Literal;
import lox.Expr.Unary;
import lox.Stmt.Expression;
import lox.Stmt.Print;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {
        // just returns the literal value
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) {
        // returns the evaluated expression by recursively calling self
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {
        // the expression the operator acts on
        Object right = evaluate(expr.right);

        // switch on the type to determine returned value
        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                return -(double) right;
            default:
                break;
        }

        // return null if expr.operator.type is not `!` or `-`
        return null;
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case PLUS:
                // use plus to add numbers or concatenate strings
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                // concatenate strings together with plus. This also allows concatenation of numbers and bools
                if (left instanceof String || right instanceof String) {
                    StringBuilder builder = new StringBuilder();

                    builder.append(stringify(left));
                    builder.append(stringify(right));

                    return builder.toString();
                    // this is the original, basic implementation
                    // return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0.0)
                    throw new RuntimeError(expr.operator, "Can not divide by 0");
                return (double) left / (double) right;
            case STAR:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left * (double) right;
                }
                
                if (left instanceof Double && right instanceof String) {
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < (double)left; i++) {
                        builder.append(stringify((String)right));
                        if (!(i >= (double)left - 1)){
                            builder.append(" ");
                        }
                    }
                    return builder.toString();
                }

                if (left instanceof String && right instanceof Double) {
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < (double)right; i++) {
                        builder.append(stringify((String)left));
                        if (!(i >= (double)right - 1)){
                            builder.append(" ");
                        }
                    }
                    return builder.toString();
                }
                
                throw new RuntimeError(expr.operator, "Operands must be two numbers or a string and a number.");
                // original
                // checkNumberOperands(expr.operator, left, right);
                // return (double) left * (double) right;
            case GREATER:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left > (double) right;
                }
                if (left instanceof String || right instanceof String) {
                    return ((String) left).compareTo((String) right) > 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be strings or numbers.");
            case GREATER_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left >= (double) right;
                }
                if (left instanceof String || right instanceof String) {
                    return ((String) left).compareTo((String) right) >= 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be strings or numbers.");
            case LESS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left < (double) right;
                }
                if (left instanceof String || right instanceof String) {
                    return ((String) left).compareTo((String) right) < 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be strings or numbers.");
            case LESS_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left <= (double) right;
                }
                if (left instanceof String || right instanceof String) {
                    return ((String) left).compareTo((String) right) <= 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be strings or numbers.");
            // we don't need to cast left and right for `==` and `!=` because we can do
            // equality comparisons across types. These comparisons just always return false for different types
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            default:
                break;
        }

        return null;
    }


    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    // validates UNARY expression operand to ensure its a double, throws error if its not
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    // validates BINARY expression operands to ensure they are both doubles, throws
    // error if either one is not a double
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    // evaluates an expression by calling the expression's accept method
    private Object evaluate(Expr expr) {
        // pass in `this` Interpreter class so accept knows what visitor to use
        return expr.accept(this);
    }

    // right now this return false if the for a boolean false or nil value, true for
    // everything else
    // i'd like to change this in the future to return false for empty arrays
    private boolean isTruthy(Object object) {

        // make null, 0, and empty strings equal false. return the value of the boolean
        // if its a boolean
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean)
            return (boolean) object;
        if (object instanceof Double && (double) object == 0.0)
            return false;
        if (object instanceof String && ((String) object).equals(""))
            return false;

        // return true if none of the cases above are met
        return true;
    }

    // We have to handle nil/null in a special way because Java's .equals() throws a
    // NullPointerException when comparing to null
    private boolean isEqual(Object a, Object b) {

        // if they're both null they are equal
        if (a == null && b == null)
            return true;
        // if just a is null, they're not equal because we eliminated b being null with
        // the check above
        // we check `a` being null because we can't call .equals() on `a`, a null value
        if (a == null)
            return false;

        // use Java's `equals()` method to check if a is equivalent to b
        return a.equals(b);
    }

    private String stringify(Object object) {
        // show null values as nil to the user
        if (object == null)
            return "nil";

        // Lox stores all numbers as doubles, but we still want to show integers as ints
        // without the fractional part
        if (object instanceof Double) {
            String text = object.toString();
            // if the double ends with a .0, we shoudl display it without the `.0`
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }
}
