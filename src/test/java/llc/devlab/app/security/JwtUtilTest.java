package llc.devlab.app.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(new TokenProperties(
            "a-test-secret-key-that-is-long-enough-for-hmac-signing",
            60_000L,
            120_000L));

    @Test
    void generatedAccessAndRefreshTokensContainTheUsernameAndExpectedExpiry() {
        long beforeGeneration = System.currentTimeMillis();

        var accessToken = jwtUtil.generateAccessToken("alice");
        var refreshToken = jwtUtil.generateRefreshToken("alice");

        assertThat(jwtUtil.extractUsername(accessToken.token())).isEqualTo("alice");
        assertThat(jwtUtil.extractUsername(refreshToken.token())).isEqualTo("alice");
        assertThat(accessToken.expiresIn()).isBetween(beforeGeneration + 60_000L,
                System.currentTimeMillis() + 60_001L);
        assertThat(refreshToken.expiresIn()).isBetween(beforeGeneration + 120_000L,
                System.currentTimeMillis() + 120_001L);
    }
}
