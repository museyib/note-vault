package llc.devlab.app.service;

import jakarta.transaction.Transactional;
import llc.devlab.app.dto.*;
import llc.devlab.app.entity.AppUser;
import llc.devlab.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginFailService loginFailService;

    @Transactional
    public UserDto create(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));

        AppUser saved = userRepository.save(user);

        return new UserDto(saved.getId(), saved.getUsername());
    }

    public LoginResponse authenticate(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username());

        if(loginFailService.locked(request.username())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Too many failed login attempts. Please try again later.");
        }

        if(user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            if (user != null)
                loginFailService.saveLoginFail(request.username());

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        loginFailService.unlockUser(request.username());

        TokenInfo accessToken = tokenService.generateAccessToken(user.getUsername());
        TokenInfo refreshToken = tokenService.generateRefreshToken(user.getUsername());

        return new LoginResponse(
                user.getUsername(),
                accessToken.token(),
                accessToken.expiresIn(),
                refreshToken.token());
    }

    public RefreshTokenResponse refresh(TokenRequest request) {
        tokenService.verify(request.token());

        TokenInfo accessToken = tokenService.generateAccessTokenFromRefreshToken(request.token());

        return new RefreshTokenResponse(accessToken.token(), accessToken.expiresIn());
    }

    public void deleteRefreshToken(TokenRequest request) {
        tokenService.deleteRefreshToken(request.token());
    }
}
