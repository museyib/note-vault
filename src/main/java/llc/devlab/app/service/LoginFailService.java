package llc.devlab.app.service;

import jakarta.transaction.Transactional;
import llc.devlab.app.entity.LoginFail;
import llc.devlab.app.repository.LoginFailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class LoginFailService {

    private static final int MAX_LOGIN_FAILS = 5;
    private static final int LOCK_MINUTES = 15;

    private final LoginFailRepository loginFailRepository;

    @Transactional
    public void saveLoginFail(String username) {
        LoginFail loginFail = loginFailRepository.findByUsername(username).orElseGet(() -> {
            LoginFail loginFailNew = new LoginFail();
            loginFailNew.setUsername(username);
            loginFailNew.setTimestamp(System.currentTimeMillis());
            loginFailNew.setAttempts(0);
            return loginFailNew;
        });

        if (loginFail.isLocked()) {
            throw new RuntimeException("User is locked until " + loginFail.getLockedUntil());
        } else {
            loginFail.setAttempts(loginFail.getAttempts() + 1);
            if (loginFail.getAttempts() >= MAX_LOGIN_FAILS) {
                loginFail.setLocked(true);
                loginFail.setLockedUntil(Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES));
            }
        }
        loginFailRepository.save(loginFail);
    }

    @Transactional
    public void unlockUser(String username) {
        loginFailRepository.deleteByUsername(username);
    }

    public boolean locked(String username) {
        LoginFail loginFail = loginFailRepository.findByUsername(username).orElse(null);
        return loginFail != null && loginFail.isLocked();
    }
}
