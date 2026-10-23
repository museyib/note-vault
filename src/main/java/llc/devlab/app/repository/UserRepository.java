package llc.devlab.app.repository;

import llc.devlab.app.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByUsername(String username);

    AppUser findByUsername(String username);
}
