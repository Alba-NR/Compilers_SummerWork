package lexer;

public class InvalidCharException extends Exception {
    InvalidCharException(char invalidChar){
        super(" \" " + invalidChar + " \" is an invalid character in the input.");
    }
}
