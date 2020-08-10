
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LexicalAnalyser {

    /**
     * Scans the given txt file (its 1st line) and produces a list of tokens for it.
     *
     * @param textFile file {@link File} containing input string to be tokenised
     * @return list {@link List} of tokens {@link Token} for the input string
     * @throws IOException -- cannot open given file
     * @throws InvalidCharException -- encounters an invalid char in input
     */
    public static List<Token> scan(File textFile) throws IOException, InvalidCharException {
        List<Token> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            int peekInt = reader.read();  // read next char from input str
            char peek = (char) peekInt;
            boolean isNum;

            while (peekInt != -1) {
                isNum = false;

                // operators
                if (peek == '+' || peek == '-' || peek == '*' || peek == '!') {
                    Token<String> token = new Token<>(TokenName.OP, String.valueOf(peek));
                    result.add(token);
                // cos operator
                } else if (peek == 'c') {
                    peek = (char) reader.read();
                    if (peek == 'o') {
                        peek = (char) reader.read();
                        if (peek == 's') {
                            Token<String> token = new Token<>(TokenName.OP, "cos");
                            result.add(token);
                        } else {
                            throw new InvalidCharException(peek);
                        }
                    } else {
                        throw new InvalidCharException(peek);
                    }
                // numbers
                } else if (Character.isDigit(peek)) {
                    int v = 0;  // calc value of int num
                    while (Character.isDigit(peek)) {
                        v = v * 10 + Integer.parseInt(String.valueOf(peek));
                        peekInt = reader.read();
                        peek = (char) peekInt;
                    }
                    // float
                    if (peek == '.') {
                        peek = (char) reader.read();
                        int d = 10;
                        double vf = v;  // calc value of float num
                        while (Character.isDigit(peek)) {
                            vf += Integer.parseInt(String.valueOf(peek)) / (double) d;
                            peekInt = reader.read();
                            peek = (char) peekInt;
                            d *= 10;
                        }
                        Token<Double> token = new Token<>(TokenName.FLOAT, vf);
                        result.add(token);

                    } else { // was an integer
                        Token<Integer> token = new Token<>(TokenName.INT, v);
                        result.add(token);
                    }

                    isNum = true;

                } else {
                    throw new InvalidCharException(peek);
                }

                // update peek values for next iteration ONLY if token WAS NOT a num (bc they're already updated if num)
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
