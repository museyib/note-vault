package llc.devlab.app;

import llc.devlab.app.dto.LoginRequest;
import llc.devlab.app.dto.LoginResponse;
import llc.devlab.app.dto.NoteDto;
import llc.devlab.app.dto.NoteRequest;
import llc.devlab.app.dto.RegisterRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NoteIsolationIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void usersCannotReadUpdateDeleteOrListAnotherUsersNotes() throws Exception {
        register("alice", "alice-password");
        register("bob", "bob-password");

        String aliceToken = login("alice", "alice-password");
        String bobToken = login("bob", "bob-password");

        NoteDto alicesNote = createNote(aliceToken,
                new NoteRequest("Alice's note", "private Alice content", List.of("alice")));
        NoteDto bobsNote = createNote(bobToken,
                new NoteRequest("Bob's note", "private Bob content", List.of("bob")));

        HttpResponse<String> readByBob = send(
                "GET", "/api/notes/" + alicesNote.id(), bobToken, null);
        assertThat(readByBob.statusCode()).isEqualTo(404);

        HttpResponse<String> updateByBob = send(
                "PUT", "/api/notes/" + alicesNote.id(), bobToken,
                new NoteRequest("Bob's replacement", "changed by Bob", List.of("bob")));
        assertThat(updateByBob.statusCode()).isEqualTo(404);

        HttpResponse<String> deleteByBob = send(
                "DELETE", "/api/notes/" + alicesNote.id(), bobToken, null);
        assertThat(deleteByBob.statusCode()).isEqualTo(404);

        HttpResponse<String> bobNotesResponse = send(
                "GET", "/api/notes?page=0&size=20", bobToken, null);
        assertThat(bobNotesResponse.statusCode()).isEqualTo(200);
        List<NoteDto> bobNotes = objectMapper.readValue(
                bobNotesResponse.body(), new TypeReference<>() {});
        assertThat(bobNotes)
                .extracting(NoteDto::id)
                .containsExactly(bobsNote.id())
                .doesNotContain(alicesNote.id());

        HttpResponse<String> aliceAfterAttempts = send(
                "GET", "/api/notes/" + alicesNote.id(), aliceToken, null);
        assertThat(aliceAfterAttempts.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readValue(aliceAfterAttempts.body(), NoteDto.class))
                .isEqualTo(alicesNote);
    }

    private void register(String username, String password) throws Exception {
        HttpResponse<String> response = send(
                "POST", "/api/auth/register", null, new RegisterRequest(username, password));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private String login(String username, String password) throws Exception {
        HttpResponse<String> response = send(
                "POST", "/api/auth/login", null, new LoginRequest(username, password));
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readValue(response.body(), LoginResponse.class).accessToken();
    }

    private NoteDto createNote(String token, NoteRequest request) throws Exception {
        HttpResponse<String> response = send("POST", "/api/notes", token, request);
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readValue(response.body(), NoteDto.class);
    }

    private HttpResponse<String> send(String method, String path, String token, Object body)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body)));
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
