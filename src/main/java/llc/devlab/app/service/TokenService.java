package llc.devlab.app.service;

import jakarta.transaction.Transactional;
import llc.devlab.app.dto.TokenInfo;
import llc.devlab.app.entity.RefreshToken;
import llc.devlab.app.repository.RefreshTokenRepository;
import llc.devlab.app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenInfo generateAccessToken(String username) {
        return jwtUtil.generateAccessToken(username);
    }

    public TokenInfo generateAccessTokenFromRefreshToken(String refreshToken) {
        String username = jwtUtil.extractUsername(refreshToken);
        return jwtUtil.generateAccessToken(username);
    }

    @Transactional
    public TokenInfo generateRefreshToken(String username) {
        TokenInfo tokenInfo = jwtUtil.generateRefreshToken(username);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenInfo.token());
        refreshToken.setExpirationDate(Instant.ofEpochMilli(tokenInfo.expiresIn()));
        refreshTokenRepository.save(refreshToken);
        return new TokenInfo(tokenInfo.token(), tokenInfo.expiresIn());
    }

    @Transactional
    public void verify(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token);
        if(refreshToken == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");

        if (refreshToken.getExpirationDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired. Please login again");
        }
    }

    @Transactional
    public void deleteRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token);
        if(refreshToken == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");

        refreshTokenRepository.delete(refreshToken);
    }
}
