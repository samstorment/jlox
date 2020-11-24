package lox;

import lox.Expr.Binary;
import lox.Expr.Grouping;
import lox.Expr.Literal;
import lox.Expr.Unary;

public class Interpreter implements Expr.Visitor<Object> {
    
    void interpret(Expr expression) { 
        try {
          Object value = evaluate(expression);
          System.out.println(stringify(value));
        } catch (RuntimeError error) {
          Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                return -(double)right;
            default:
                break;
        }

        return null;
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            // use plus to add numbers or concatenate strings
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
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
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case GREATER:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left > (double)right;
                }
                if (left instanceof String || right instanceof String) {
                    return ((String)left).compareTo((String)right) > 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be strings or numbers.");
            case GREATER_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left >= (double)right;
                }
                if (left instanceof String || right instanceof String) {
                    return ((String)left).compareTo((String)right) >= 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be strings or numbers.");
            case LESS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left < (double)right;
                }
                if (left instanceof String || right instanceof String) {
                    return ((String)left).compareTo((String)right) < 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be strings or numbers.");
            case LESS_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left <= (double)right;
                }
                if (left instanceof String || right instanceof String) {
                    return ((String)left).compareTo((String)right) <= 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be strings or numbers.");
            // we don't need to cast left and right for `==` and `!=` because we can do equality comparisons across types 
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            default:
                break;
        }

        return null;
    }

    // validates UNARY expression operand to ensure its a double, throws error if its not
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    // validates BINARY expression operands to ensure they are both doubles, throws error if either one is not a double
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // right now this return false if the for a boolean false or nil value, true for everything else
    // i'd like to change this in the future to return false for empty arrays
    private boolean isTruthy(Object object) {
        
        // make null, 0, and empty strings equal false. return the value of the boolean if its a boolean
        if (object == null) { return false; }
        if (object instanceof Boolean) return (boolean)object;
        if (object instanceof Double && (double)object == 0.0) return false;
        if (object instanceof String && ((String)object).equals("")) return false;

        // return true if none of the cases above are met
        return true;
    }

    // We have to handle nil/null in a special way because Java's .equals() throws a NullPointerException when comparing to null
    private boolean isEqual(Object a, Object b) {

        // if they're both null they are equal
        if (a == null && b == null) return true;
        // if just a is null, they're not equal because we eliminated b being null with the check above
        // we check `a` being null because we can't call .equals() on `a`, a null value
        if (a == null) return false;
    
        // use Java's `equals()` method to check if a is equivalent to b
        return a.equals(b);
    }

    private String stringify(Object object) {
        // show null values as nil to the user
        if (object == null) return "nil";
    
        // Lox stores all numbers as doubles, but we still want to show integers as ints without the fractional part
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
}
