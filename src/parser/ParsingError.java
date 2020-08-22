package parser;

import lexer.Token;

import java.util.Stack;

public class ParsingError extends Exception {
    ParsingError(Stack<Integer> stack, Token nextToken){
        super();
        System.out.println("Stack contents: " + stack + " | Next lexer.Token: " + nextToken);
    }
}
