package lox;

import java.beans.Expression;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

class Lox {

    // tracks whether or not our source code had an error, this gets set to true when a `error()` is called
    static boolean hadError = false;

    public static void main(String [] args) throws IOException {
        // if there is more than one command line arg, tell the user how to use the Lox and exit
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length ==  1) {
            // if there is exactly one argument, we assume that one argument is a file path so we try to run that file
            runFile(args[0]);
        } else {
            // this last case runs with no arguments, this is the (R)ead (E)valuate (P)rint (L)oop
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        
        // read the passed in filepath and save the contents as one big string
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String fileText = new String(bytes, Charset.defaultCharset());

        // run the file
        run(fileText);

        // Indicate an error in the exit code
        if (hadError) System.exit(65);
    }

    // wrapper around the run function. for use when running single line lox code at the command line
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        // just keep waiting for user input
        while (true) {
            // print a greater than sign as an indicator for Lox code
            System.out.print("> ");
            // read the user's line of input
            String line = reader.readLine();
            // the line is null when ctrl+d (the "end-of-file" signal) is entered, or when 'exit' is typed
            if (line == null || line.equals("exit")) break;
            // run the text the user inputs on the line
            run(line);
            // reset the error state after each line of code is run
            hadError = false;
        }
    }

    // the main run function
    private static void run(String source) {

        // instantiate the Scanner, passing in the source code string
        Scanner scanner = new Scanner(source);
        // scan all of the tokens and save them in the List `tokens`
        List<Token> tokens = scanner.scanTokens();

        // test the parser
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();
        if (hadError) return;   // for syntax errors caught by the parser
        System.out.println(new AstPrinter().print(expression));


        // print each token to visualize them. This prints the type, lexeme, and literal value
        // this is just for testing and visualizing
        // for (Token token : tokens) {
        //     System.out.println(token);
        // }

    }

    // Scanner error
    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    // Parser error
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }
}