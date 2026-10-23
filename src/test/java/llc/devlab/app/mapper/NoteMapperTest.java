package llc.devlab.app.mapper;

import llc.devlab.app.dto.NoteDto;
import llc.devlab.app.dto.NoteRequest;
import llc.devlab.app.entity.Note;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoteMapperTest {

    private final NoteMapper noteMapper = new NoteMapper();

    @Test
    void mapsNoteRequestsToEntitiesAndEntitiesToDtos() {
        List<String> tags = List.of("work", "ideas");
        Note entity = noteMapper.toEntity(new NoteRequest("Title", "Content", tags));

        assertThat(entity.getId()).isNull();
        assertThat(entity.getTitle()).isEqualTo("Title");
        assertThat(entity.getContent()).isEqualTo("Content");
        assertThat(entity.getTags()).containsExactlyElementsOf(tags);

        entity.setId(42L);
        assertThat(noteMapper.toDto(entity))
                .isEqualTo(new NoteDto(42L, "Title", "Content", tags));
    }
}
