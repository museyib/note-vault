package llc.devlab.app.service;

import llc.devlab.app.entity.LoginFail;
import llc.devlab.app.repository.LoginFailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginFailServiceTest {

    @Mock
    private LoginFailRepository repository;

    @Test
    void fifthFailedAttemptLocksAccountForFifteenMinutes() {
        LoginFailService service = new LoginFailService(repository);
        AtomicReference<LoginFail> storedLoginFail = new AtomicReference<>();
        when(repository.findByUsername("alice")).thenAnswer(invocation ->
                Optional.ofNullable(storedLoginFail.get())
                        .filter(loginFail -> loginFail.getUsername().equals(invocation.getArgument(0))));
        doAnswer(invocation -> {
            storedLoginFail.set(invocation.getArgument(0));
            return storedLoginFail.get();
        }).when(repository).save(any(LoginFail.class));

        for (int attempt = 0; attempt < 4; attempt++) {
            service.saveLoginFail("alice");
        }
        assertThat(storedLoginFail.get().getAttempts()).isEqualTo(4);
        assertThat(service.locked("alice")).isFalse();

        Instant beforeLock = Instant.now();
        service.saveLoginFail("alice");

        assertThat(storedLoginFail.get().getAttempts()).isEqualTo(5);
        assertThat(storedLoginFail.get().isLocked()).isTrue();
        assertThat(storedLoginFail.get().getLockedUntil())
                .isBetween(beforeLock.plusSeconds(14 * 60L), Instant.now().plusSeconds(16 * 60L));
        assertThat(service.locked("alice")).isTrue();
    }

    @Test
    void alreadyLockedAccountDoesNotAccumulateMoreFailures() {
        LoginFail locked = new LoginFail();
        locked.setUsername("alice");
        locked.setTimestamp(System.currentTimeMillis());
        locked.setAttempts(5);
        locked.setLocked(true);
        locked.setLockedUntil(Instant.now().plusSeconds(60));
        when(repository.findByUsername("alice")).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> new LoginFailService(repository).saveLoginFail("alice"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User is locked");

        assertThat(locked.getAttempts()).isEqualTo(5);
    }

    @Test
    void unlockingDeletesTheStoredFailureState() {
        LoginFailService service = new LoginFailService(repository);

        service.unlockUser("alice");

        verify(repository).deleteByUsername("alice");
    }
}
