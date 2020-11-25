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

    // tracks whether or not our source code had an error when scanning/parsing, this gets set to true when a `error()` is called
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

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
        if (hadRuntimeError) System.exit(70);
    }

    // wrapper around the run function. for use when running single line lox code at the command line
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        // just keep waiting for user input
        while (true) {

            String promptArrow = ConsoleColors.YELLOW_BOLD_BRIGHT + ">> " + ConsoleColors.RESET;
            String resultArrow = ConsoleColors.GREEN_BOLD_BRIGHT + "-> " + ConsoleColors.RESET;

            // print two greater than signs as the command prompt
            System.out.print(promptArrow);

            // read the user's line of input. re-read if the input is empty
            String line = reader.readLine();
            // the line is null when ctrl+d (the "end-of-file" signal) is entered, or when 'exit' is typed
            if (line == null || line.equals("exit")) break;

            // keep re-reading input if its empty
            while (line.equals("") ) {
                System.out.print(promptArrow);
                line = reader.readLine();
            }
            // print a green arrow for the result
            System.out.print(resultArrow);

            // run the text the user inputs on the line
            run(line);
            System.out.println();
            // reset the error state after each line of code is run
            hadError = false;
        }
    }

    // the main run function
    private static void run(String source) {

        // instantiate the Scanner, passing in the source code string
        // the scanner generates a list of tokens from the source file when we call scanTokens()
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // Get the list of statements from the Parser
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();


        if (hadError) return;   // for syntax errors caught by the parser

        // run the interpreter by handing the statements to the interpreter
        interpreter.interpret(statements);
    }

    // Scanner error
    static void error(int line, String message) {
        report(line, "", message);
    }

    // Parser error
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    // for reporting errors before runtime
    private static void report(int line, String where, String message) {
        System.err.println(ConsoleColors.RED_BRIGHT + "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    // for reporting runtime errors
    static void runtimeError(RuntimeError error) {
        System.err.println(ConsoleColors.RED_BRIGHT + "[line " + error.token.line + "] " + error.getMessage());
        hadRuntimeError = true;
    }

}