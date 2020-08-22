import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InvalidCharException, ParsingError{

        try {
            File inputFile = new  File("./src/input.txt");
            List<Token> tokenStream = LexicalAnalyser.scan(inputFile);

            File specificationFile = new File("./src/grammar.txt");
            Parser parser = new Parser(specificationFile);
            parser.constructSLRparsingTable();

            parser.parse(tokenStream);

        }catch (IOException e) {
            throw new IOException("Can't access given file.", e);
        }
    }
}
