package llc.devlab.app.repository;

import llc.devlab.app.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("SELECT n FROM Note n WHERE n.author.username = :username")
    List<Note> findAllFiltered(Pageable pageable,
                               @Param("username") String username);

    Optional<Note> findByIdAndAuthor_Username(Long id, String authorUsername);

    Optional<Note> findByIdAndAuthor_UsernameAndDeletedFalse(Long id, String authorUsername);
}
