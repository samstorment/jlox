package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

// class for generating Syntax tree classes. When running this file, make the command line argument `lox` to generate the Java file "Expr.java" in the Lox directory
public class GenerateAst {
    public static void main(String[] args) throws IOException {

        // if there is not exactly 1 command line argument, tell the user how to run this file
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        // get the command line argument, this is the directory we output to
        String outputDir = args[0];

        // list of non-terminal productions that an expression can expand to
        List<String> expressionTypes = Arrays.asList(
            // the left side of the colon is the Class Name/type of the non-terminal. The right side is the comma separated list of constructor parameters
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Object value",
            "Unary    : Token operator, Expr right"
        );

        // define an Abstract Syntax tree file called Expr.java. Place that file in the outputDir. Create subclasses from the list of expressionTypes
        defineAst(outputDir, "Expr", expressionTypes);
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        
        // the path where the file will live
        String path = outputDir + "/" + baseName + ".java";
        // lets us write the file to the path
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        // create the top of the Expr.java file
        writer.println("package lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("// The abstract class all the nested classes inherit from");
        writer.println("abstract class " + baseName + " {\n");

        defineVisitor(writer, baseName, types);

        // loop through each subclass in the list of types
        for (String type : types) {
            // the class name is on the left side of the `:`. trim() to remvoe extra whitespace
            String className = type.split(":")[0].trim();
            // the fields are the comma separated list on the right side of the `:`. trim() to remove whitespace
            String fields = type.split(":")[1].trim(); 
            // create the code for each subclass
            defineType(writer, baseName, className, fields);
        }
        
        writer.println("    // Create an `accept` method in the Expr class that all subclasses must implement");
        writer.println("    abstract <R> R accept(Visitor<R> visitor);\n");

        // print the final closing bracket and close the file writer
        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    // Generic Visitor interface that has methods for each subclass");
        writer.println("    interface Visitor<R> {");

        writer.println("        // create a generic `visit` method for each expression subclass");
        for (String type : types) {
            // get the typeName, AKA the sublass name. This is on the left side of the colon in the list of types we made
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }

        // close the interface
        writer.println("    }\n");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        
        // each subclass is a static class that extends the base class we pass in
        // exmpty space at the start is to format this code to 4 space indentation 
        writer.println("    static class " + className + " extends " + baseName + " {");

        // create the constructor
        writer.println("        " + className + "(" + fieldList + ") {");

        // get all the parameters from the comma separated list
        String[] fields = fieldList.split(", ");
        // loop through the parameters to assign instance variables
        for (String field : fields) {
            // split by a space to get just the field name and ignore the type of the field
            String name = field.split(" ")[1];
            // this.name = name;
            writer.println("            this." + name + " = " + name + ";");
        }

        // close the constructor
        writer.println("        }");

        // override the accept method in each class
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }\n");

        // create constant instance variables for each field 
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }

        // close the subclass. add a newline for nice formatting
        writer.println("    }\n");
    }
}