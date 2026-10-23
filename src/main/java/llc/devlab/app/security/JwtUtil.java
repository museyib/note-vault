package llc.devlab.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import llc.devlab.app.dto.TokenInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {
    private final TokenProperties tokenProperties;

    public TokenInfo generateAccessToken(String username) {
        long expiration = System.currentTimeMillis() + tokenProperties.getAccessTokenExpiration();
        String token = Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(expiration))
                .signWith(Keys.hmacShaKeyFor(tokenProperties.getSecretKey().getBytes()))
                .compact();
        return new TokenInfo(token, expiration);
    }

    public TokenInfo generateRefreshToken(String username) {
        long expiration = System.currentTimeMillis() + tokenProperties.getRefreshTokenExpiration();
        return new TokenInfo(Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(expiration))
                .signWith(Keys.hmacShaKeyFor(tokenProperties.getSecretKey().getBytes()))
                .compact(), expiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(tokenProperties.getSecretKey().getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }
}
