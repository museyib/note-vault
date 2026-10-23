package llc.devlab.app.repository;

import llc.devlab.app.entity.LoginFail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginFailRepository extends JpaRepository<LoginFail, Long> {
    void deleteByUsername(String username);

    Optional<LoginFail> findByUsername(String username);
}
