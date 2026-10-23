package llc.devlab.app.service;

import llc.devlab.app.dto.NoteDto;
import llc.devlab.app.dto.NoteRequest;
import llc.devlab.app.entity.Note;
import llc.devlab.app.mapper.NoteMapper;
import llc.devlab.app.repository.NoteRepository;
import llc.devlab.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteRepository, userRepository, new NoteMapper());
    }

    @Test
    void listExcludesDeletedNotesAndAppliesCaseInsensitiveContentAndTagFilters() {
        Note matching = note(1L, "Discuss project", "A BLUE meeting", List.of("work", "private"), false);
        Note contentMismatch = note(2L, "Shopping", "Buy groceries", List.of("private"), false);
        Note tagMismatch = note(3L, "Discuss later", "Blue skies", List.of("public"), false);
        Note deletedMatch = note(4L, "Old", "Blue meeting", List.of("private"), true);
        when(noteRepository.findAllFiltered(any(Pageable.class), eq("alice")))
                .thenReturn(List.of(matching, contentMismatch, tagMismatch, deletedMatch));

        List<NoteDto> notes = noteService.getNotesByUser(0, 20, "blue", "private", "alice");

        assertThat(notes).containsExactly(
                new NoteDto(3L, "Discuss later", "Blue skies", List.of()),
                new NoteDto(1L, "Discuss project", "A BLUE meeting", List.of("private")));
    }

    @Test
    void getUpdateAndDeleteReturnNotFoundWhenNoteIsNotOwnedOrMissing() {
        when(noteRepository.findByIdAndAuthor_UsernameAndDeletedFalse(8L, "alice"))
                .thenReturn(Optional.empty());
        when(noteRepository.findByIdAndAuthor_Username(8L, "alice"))
                .thenReturn(Optional.empty());

        assertNotFound(() -> noteService.getNote(8L, "alice"));
        assertNotFound(() -> noteService.updateNote(8L, new NoteRequest("Title", "Content", List.of()), "alice"));
        assertNotFound(() -> noteService.deleteNote(8L, "alice"));
    }

    private void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private Note note(Long id, String title, String content, List<String> tags, boolean deleted) {
        Note note = new Note();
        note.setId(id);
        note.setTitle(title);
        note.setContent(content);
        note.setTags(tags);
        note.setDeleted(deleted);
        return note;
    }
}
