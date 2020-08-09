
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LexicalAnalyser {
    
    /**
     * Scans the given txt file (its 1st line) and produces a list of tokens for it.
     *
     * @param textFile file {@link File} containing string to be tokenised
     * @return list of tokens {@link Token}
     * @throws IOException
     * @throws InvalidCharException
     */
    public static List<Token> scan(File textFile) throws IOException, InvalidCharException {
        List<Token> result = new ArrayList<Token>();

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            int peekInt = reader.read();
            char peek = (char) peekInt;
            boolean isNum = false;

            while (peekInt != -1) {
                isNum = false;

                // operators
                if (peek == '+' || peek == '-' || peek == '*' || peek == '!') {
                    Token<String> token = new Token<>(TokenName.OP, String.valueOf(peek));
                    result.add(token);
                // cos operator
                } else if (peek == 'c') {
                    String lexeme = "c";
                    peek = (char) reader.read();
                    if (peek == 'o') {
                        lexeme += peek;
                        peek = (char) reader.read();
                        if (peek == 's') {
                            lexeme += peek;
                            Token<String> token = new Token<>(TokenName.OP, "cos");
                            result.add(token);
                        } else {
                            throw new InvalidCharException();
                        }
                    } else {
                        throw new InvalidCharException();
                    }
                    // numbers
                } else if (Character.isDigit(peek)) {
                    int v = 0;
                    while (Character.isDigit(peek)) {
                        v = v * 10 + Integer.parseInt(String.valueOf(peek));
                        peekInt = reader.read();
                        peek = (char) peekInt;
                    }
                    // float
                    if (peek == '.') {
                        peek = (char) reader.read();
                        int d = 10;
                        double vf = v;
                        while (Character.isDigit(peek)) {
                            vf += Integer.parseInt(String.valueOf(peek)) / (double) d;
                            peekInt = reader.read();
                            peek = (char) peekInt;
                            d *= 10;
                        }
                        Token<Double> token = new Token<>(TokenName.FLOAT, vf);
                        result.add(token);

                    } else {
                        Token<Integer> token = new Token<>(TokenName.INT, v);
                        result.add(token);
                    }

                    isNum = true;

                } else {
                    throw new InvalidCharException();
                }

                // update peek values for next iteration ONLY if token WAS NOT a num
                if (!isNum) {
                    peekInt = reader.read();
                    peek = (char) peekInt;
                }
            }

        } catch (IOException e) {
            throw new IOException("Can't access file " + textFile, e);
        }

        return result;
    }
}
