package llc.devlab.app.controller;

import llc.devlab.app.dto.NoteDto;
import llc.devlab.app.dto.NoteRequest;
import llc.devlab.app.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Slf4j
public class NoteController {
    private final NoteService noteService;

    @GetMapping
    public ResponseEntity<List<NoteDto>> getNotes(@RequestParam int page,
                                                  @RequestParam int size,
                                                  @RequestParam(required = false, name = "q") String contentSearch,
                                                  @RequestParam(required = false, name = "tag") String tagSearch,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(noteService.getNotesByUser(page, size, contentSearch, tagSearch, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteDto> getNote(@PathVariable long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(noteService.getNote(id, userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<NoteDto> createNote(@RequestBody NoteRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity
                .status(CREATED)
                .body(noteService.createNote(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteDto> updateNote(@PathVariable long id, @RequestBody NoteRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(noteService.updateNote(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        noteService.deleteNote(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
