package llc.devlab.app.service;

import llc.devlab.app.dto.LoginRequest;
import llc.devlab.app.dto.LoginResponse;
import llc.devlab.app.dto.RegisterRequest;
import llc.devlab.app.dto.TokenInfo;
import llc.devlab.app.entity.AppUser;
import llc.devlab.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginFailService loginFailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registrationRejectsAnExistingUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.create(new RegisterRequest("alice", "password")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(userRepository, never()).save(any());
    }

    @Test
    void invalidPasswordRecordsAFailedAttempt() {
        AppUser user = user();
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(loginFailService.locked("alice")).thenReturn(false);
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(new LoginRequest("alice", "wrong")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(loginFailService).saveLoginFail("alice");
        verify(tokenService, never()).generateAccessToken("alice");
    }

    @Test
    void lockedUserIsRejectedWithoutRecordingAnotherFailure() {
        when(userRepository.findByUsername("alice")).thenReturn(user());
        when(loginFailService.locked("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.authenticate(new LoginRequest("alice", "password")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(loginFailService, never()).saveLoginFail("alice");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void successfulAuthenticationClearsFailuresAndReturnsBothTokens() {
        AppUser user = user();
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(loginFailService.locked("alice")).thenReturn(false);
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(tokenService.generateAccessToken("alice")).thenReturn(new TokenInfo("access", 123L));
        when(tokenService.generateRefreshToken("alice")).thenReturn(new TokenInfo("refresh", 456L));

        LoginResponse response = authService.authenticate(new LoginRequest("alice", "password"));

        assertThat(response).isEqualTo(new LoginResponse("alice", "access", 123L, "refresh"));
        verify(loginFailService).unlockUser("alice");
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setUsername("alice");
        user.setPassword("encoded-password");
        return user;
    }
}
