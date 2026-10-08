package vet.client;

/**
 * Exception levee par la couche logique client lors d'une erreur
 * de communication ou d'appel distant.
 */
public class ClientException extends Exception {

    private static final long serialVersionUID = 1L;

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
