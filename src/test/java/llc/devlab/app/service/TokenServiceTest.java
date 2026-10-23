package llc.devlab.app.service;

import llc.devlab.app.entity.RefreshToken;
import llc.devlab.app.repository.RefreshTokenRepository;
import llc.devlab.app.security.JwtUtil;
import llc.devlab.app.dto.TokenInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void unknownRefreshTokenIsRejected() {
        when(repository.findByToken("unknown")).thenReturn(null);

        assertUnauthorized(() -> tokenService.verify("unknown"));

        verify(repository, never()).delete(any(RefreshToken.class));
    }

    @Test
    void expiredRefreshTokenIsDeletedAndRejected() {
        RefreshToken token = refreshToken(Instant.now().minusSeconds(1));
        when(repository.findByToken("expired")).thenReturn(token);

        assertUnauthorized(() -> tokenService.verify("expired"));

        verify(repository).delete(token);
    }

    @Test
    void refreshTokenGenerationPersistsTheTokenAndExpiration() {
        TokenInfo generated = new TokenInfo("refresh", System.currentTimeMillis() + 60_000);
        when(jwtUtil.generateRefreshToken("alice")).thenReturn(generated);

        TokenInfo result = tokenService.generateRefreshToken("alice");

        assertThat(result).isEqualTo(generated);
        verify(repository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getToken().equals("refresh")
                        && saved.getExpirationDate().equals(Instant.ofEpochMilli(generated.expiresIn()))));
    }

    private void assertUnauthorized(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private RefreshToken refreshToken(Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setToken("expired");
        token.setExpirationDate(expiresAt);
        return token;
    }
}
