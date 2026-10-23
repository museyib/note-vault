package llc.devlab.app.dto;

import java.util.List;

public record NoteRequest(String title, String content, List<String> tags) {
}
