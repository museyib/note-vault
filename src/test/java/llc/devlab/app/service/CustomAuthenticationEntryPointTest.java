package llc.devlab.app.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CustomAuthenticationEntryPointTest {

    @Test
    void commenceSendsUnauthorized() throws IOException {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        InsufficientAuthenticationException exception =
                new InsufficientAuthenticationException("Full authentication is required");

        entryPoint.commence(request, response, exception);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
    }
}
