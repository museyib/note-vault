package llc.devlab.app.dto;

public record RefreshTokenResponse(String accessToken, long expiresIn) {
}
