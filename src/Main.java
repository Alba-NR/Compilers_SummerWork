import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InvalidCharException{
        File inputFile = new  File("./src/input.txt");
        List<Token> tokenStream = LexicalAnalyser.scan(inputFile);

        System.out.println(tokenStream);
    }
}
