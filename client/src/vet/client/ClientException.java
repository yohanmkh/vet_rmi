package vet.client;

/**
 * Error reported by the client logic layer. It hides the underlying RMI
 * exception so that the console only deals with a message to display.
 */
public class ClientException extends Exception {

    private static final long serialVersionUID = 1L;

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
