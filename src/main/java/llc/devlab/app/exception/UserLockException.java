package llc.devlab.app.exception;

public class UserLockException extends RuntimeException {
    public UserLockException(String message) {
        super(message);
    }
}
