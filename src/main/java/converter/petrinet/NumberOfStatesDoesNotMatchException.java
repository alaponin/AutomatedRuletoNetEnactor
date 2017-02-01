package converter.petrinet;

/**
 * Created by arnelaponin on 14/01/2017.
 */
public class NumberOfStatesDoesNotMatchException extends Exception {

    public NumberOfStatesDoesNotMatchException() {
    }

    public NumberOfStatesDoesNotMatchException(String message) {
        super(message);
    }
}
