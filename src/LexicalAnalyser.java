
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LexicalAnalyser {
    public static List<Token> scan(File textFile) throws IOException, InvalidCharException {
        List<Token> result = new ArrayList<Token>();

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            char peek = (char) reader.read();
            // operators
            if(peek == '+' || peek == '-' || peek == '*' || peek == '!'){
                Token<String> token = new Token<>(TokenName.OP, String.valueOf(peek));
            // cos operator
            } else if (peek == 'c'){
                String lexeme = "c";
                peek = (char) reader.read();
                if (peek == 'o'){
                    lexeme += peek;
                    peek = (char) reader.read();
                    if (peek == 's'){
                        lexeme += peek;
                        Token<String> token = new Token<>(TokenName.OP, "cos");
                        result.add(token);
                    } else{
                        throw new InvalidCharException();
                    }
                } else{
                    throw new InvalidCharException();
                }
            // numbers
            } else if (Character.isDigit(peek)){
                int v = 0;
                while(Character.isDigit(peek)){
                    v = v * 10 + Integer.parseInt(String.valueOf(peek));
                    peek = (char) reader.read();
                }
                // float
                if (peek == '.'){
                    peek = (char) reader.read();
                    int d = 10;
                    double vf = v;
                    while(Character.isDigit(peek)){
                        vf += Integer.parseInt(String.valueOf(peek)) / (double) d;
                        peek = (char) reader.read();
                        d *= 10;
                    }
                    Token<Double> token = new Token<>(TokenName.FLOAT, vf);
                    result.add(token);

                } else{
                    Token<Integer> token = new Token<>(TokenName.INT, v);
                    result.add(token);
                }
            } else{
                throw new InvalidCharException();
            }

        } catch (IOException e) {
            throw new IOException("Can't access file " + textFile, e);
        }

        return result;
    }

}
