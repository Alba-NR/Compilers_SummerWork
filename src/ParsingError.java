import java.util.Stack;

public class ParsingError extends Exception {
    ParsingError(Stack<Integer> stack){
        super();
        System.out.println(stack); // not sure if this would be useful tbh - check
    }
}
