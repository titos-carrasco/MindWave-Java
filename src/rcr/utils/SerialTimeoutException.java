package rcr.utils;

/**
 * Clase de apoyo para representar TimeOut
 *
 * @author Roberto Carrasco
 */
public class SerialTimeoutException extends Exception {
    public SerialTimeoutException() {

    }

    public SerialTimeoutException(String message) {
        super( message );
    }

    public SerialTimeoutException(Throwable cause) {
        super( cause );
    }

    public SerialTimeoutException(String message, Throwable cause) {
        super( message, cause );
    }
}
