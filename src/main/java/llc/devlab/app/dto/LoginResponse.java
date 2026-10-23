package llc.devlab.app.dto;

public record LoginResponse(String username, String accessToken, Long expiresIn, String refreshToken) {
}
