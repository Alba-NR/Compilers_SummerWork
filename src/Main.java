import lexer.InvalidCharException;
import lexer.LexicalAnalyser;
import lexer.Token;
import parser.ParseTreeNode;
import parser.Parser;
import parser.ParsingError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Main {
    private static void runProgram(String inputFilePathname, String grammarFilePathname) throws IOException, InvalidCharException, ParsingError {
        // --- lex the input ---
        File inputFile = new  File(inputFilePathname);
        List<Token> tokenStream = LexicalAnalyser.scan(inputFile);

        // get String repr of contents of input file
        StringBuilder source = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String nextLine = reader.readLine();
            while (nextLine != null) {
                source.append(nextLine).append("\n");
                nextLine = reader.readLine();
            }
        }
        // output input file contents
        System.out.println("------------------");
        System.out.println("Input from " + inputFile + " : \n---\n" + source.toString());

        // output result of lexer
        System.out.println("------------------");
        System.out.println("Token stream produced by lexer: \n---\n" + tokenStream +"\n");


        // --- parse the input (using the stream of tokens produced) ---
        File specificationFile = new File(grammarFilePathname);

        // get String repr of contents of grammar specification file
        source = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(specificationFile))) {
            String nextLine = reader.readLine();
            int n = 1;
            while (nextLine != null) {
                if(n == 1) source.append("Non-terminals: ").append(nextLine).append("\n");
                else if(n == 2) source.append("Terminals: ").append(nextLine).append("\nProductions:\n");
                else source.append(nextLine).append("\n");
                nextLine = reader.readLine();
                n++;
            }
        }
        // output grammar file contents
        System.out.println("------------------");
        System.out.println("Grammar specification (in " + grammarFilePathname + ") : \n---\n" + source.toString());

        // create parser, construct parsing table & parse input
        Parser parser = new Parser(specificationFile);
        parser.constructSLRparsingTable();
        ParseTreeNode root = parser.parse(tokenStream);

        System.out.println("\n------------------");
        System.out.println("Parse tree produced: \n---\n" + root);

    }

    public static void main(String[] args) throws IOException, InvalidCharException, ParsingError {
        runProgram("./src/input.txt", "./src/grammar.txt");
    }
}
