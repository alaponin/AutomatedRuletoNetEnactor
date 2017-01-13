package converter.petrinet;

/**
 * Created by arnelaponin on 13/01/2017.
 */
public class CanNotConvertPNToAutomatonException extends Exception {

    public CanNotConvertPNToAutomatonException() {
    }

    public CanNotConvertPNToAutomatonException(String message) {
        super(message);
    }
}
