
public class Token<T> {
    /**
     * Represents a token (of the form: < name , value > ) whose value is of type T
     */

    private TokenName name;
    private T value;

    Token(TokenName name, T value){
        this.name = name;
        this.value = value;
    }

    public TokenName getName() {
        return this.name;
    }

    public T getValue(){
        return this.value;
    }

    @Override
    public String toString() {
        return "< " + name + ", " + value + " >";
    }
}
