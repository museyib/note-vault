package llc.devlab.app.dto;

import java.util.List;

public record NoteDto(Long id, String title, String content, List<String> tags) {
}
