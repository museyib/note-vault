package llc.devlab.app.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void mapsStatusExceptionsToTheirStatusAndReason() {
        var response = exceptionHandler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Note not found");
    }

    @Test
    void mapsIllegalArgumentsAndLockExceptionsToClientErrorStatuses() {
        var argumentResponse = exceptionHandler.handleIllegalArgumentException(
                new IllegalArgumentException("bad page"));
        var lockResponse = exceptionHandler.handleUserLockException(
                new UserLockException("account locked"));

        assertThat(argumentResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(argumentResponse.getBody()).isEqualTo("Invalid argument: bad page");
        assertThat(lockResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(lockResponse.getBody()).isEqualTo("account locked");
    }

    @Test
    void mapsUnexpectedExceptionsToInternalServerError() {
        var response = exceptionHandler.handleException(new Exception("failure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("An error occurred: failure");
    }
}
