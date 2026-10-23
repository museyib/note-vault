package llc.devlab.app.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return buildResponse(500, "An error occurred: " + e.getMessage(), e);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return buildResponse(400, "Invalid argument: " + e.getMessage(), e);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return buildResponse(e.getStatusCode().value(), e.getReason(), e);
    }

    @ExceptionHandler(UserLockException.class)
    public ResponseEntity<String> handleUserLockException(UserLockException e) {
        return buildResponse(403, e.getMessage(), e);
    }

    private ResponseEntity<String> buildResponse(int statusCode, String message, Exception e) {
        log.error("Error occurred: {}", message, e);
        return ResponseEntity.status(statusCode).body(message);
    }
}
