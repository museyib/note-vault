package llc.devlab.app.service;

import jakarta.transaction.Transactional;
import llc.devlab.app.dto.NoteDto;
import llc.devlab.app.dto.NoteRequest;
import llc.devlab.app.entity.Note;
import llc.devlab.app.entity.AppUser;
import llc.devlab.app.mapper.NoteMapper;
import llc.devlab.app.repository.NoteRepository;
import llc.devlab.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteMapper noteMapper;

    @Transactional
    public NoteDto createNote(NoteRequest request, String username) {
        AppUser user = userRepository.findByUsername(username);
        Note note = noteMapper.toEntity(request);
        note.setAuthor(user);
        Note saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    @Transactional
    public NoteDto updateNote(Long id, NoteRequest request, String username) {
        Note note = noteRepository.findByIdAndAuthor_Username(id, username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));

        note.setTitle(request.title());
        note.setContent(request.content());
        note.setTags(request.tags());
        Note saved = noteRepository.save(note);
        return noteMapper.toDto(saved);
    }

    public List<NoteDto> getNotesByUser(int page, int size, String contentSearch, String tagSearch, String username) {
        List<Note> notes = noteRepository.findAllFiltered(PageRequest.of(page, size), username);
        Stream<NoteDto> stream = notes.stream().filter(note -> !note.isDeleted()).map(noteMapper::toDto);
        if (!isEmpty(contentSearch)) {
            stream = stream.filter(noteDto -> noteDto.content().toLowerCase().contains(contentSearch.toLowerCase()));
        }
        if (!isEmpty(tagSearch)) {
            stream = stream.map(noteDto -> new NoteDto(
                    noteDto.id(),
                    noteDto.title(),
                    noteDto.content(),
                    noteDto.tags().stream().filter(tag -> tag.toLowerCase().contains(tagSearch.toLowerCase())).collect(Collectors.toList())
            ));
        }

        return stream.sorted(Comparator.comparing(NoteDto::id).reversed()).collect(Collectors.toList());
    }

    public NoteDto getNote(Long id, String username) {
        Note note = noteRepository.findByIdAndAuthor_UsernameAndDeletedFalse(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
        return noteMapper.toDto(note);
    }

    @Transactional
    public void deleteNote(Long id, String username) {
        Note note = noteRepository.findByIdAndAuthor_UsernameAndDeletedFalse(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
        note.setDeleted(true);
    }
}
