package llc.devlab.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import llc.devlab.app.dto.LoginRequest;
import llc.devlab.app.dto.LoginResponse;
import llc.devlab.app.dto.NoteDto;
import llc.devlab.app.dto.NoteRequest;
import llc.devlab.app.dto.RefreshTokenResponse;
import llc.devlab.app.dto.RegisterRequest;
import llc.devlab.app.dto.TokenRequest;
import llc.devlab.app.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthAndNotesIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void authAndNoteHttpFlowsCoverRegistrationTokensAndSoftDeletion() throws Exception {
        String username = "user-" + UUID.randomUUID();
        String password = "test-password";

        HttpResponse<String> register = send(
                "POST", "/api/auth/register", null, new RegisterRequest(username, password));
        assertThat(register.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readValue(register.body(), UserDto.class).username()).isEqualTo(username);

        HttpResponse<String> duplicateRegister = send(
                "POST", "/api/auth/register", null, new RegisterRequest(username, password));
        assertThat(duplicateRegister.statusCode()).isEqualTo(409);

        HttpResponse<String> unauthenticatedNotes = send("GET", "/api/notes?page=0&size=20", null, null);
        assertThat(unauthenticatedNotes.statusCode()).isEqualTo(401);

        HttpResponse<String> login = send(
                "POST", "/api/auth/login", null, new LoginRequest(username, password));
        assertThat(login.statusCode()).isEqualTo(200);
        LoginResponse loginResponse = objectMapper.readValue(login.body(), LoginResponse.class);
        assertThat(loginResponse.username()).isEqualTo(username);

        HttpResponse<String> refresh = send(
                "POST", "/api/auth/refresh", null, new TokenRequest(loginResponse.refreshToken()));
        assertThat(refresh.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readValue(refresh.body(), RefreshTokenResponse.class).accessToken()).isNotBlank();

        NoteDto firstNote = createNote(loginResponse.accessToken(),
                new NoteRequest("Meeting notes", "Discuss BLUE project", List.of("work", "private")));
        NoteDto shoppingNote = createNote(loginResponse.accessToken(),
                new NoteRequest("Shopping", "Buy groceries", List.of("personal")));

        List<NoteDto> contentMatches = readNotes(loginResponse.accessToken(),
                "/api/notes?page=0&size=20&q=blue");
        assertThat(contentMatches).extracting(NoteDto::id).containsExactly(firstNote.id());

        List<NoteDto> tagMatches = readNotes(loginResponse.accessToken(),
                "/api/notes?page=0&size=20&tag=private");
        assertThat(tagMatches).extracting(NoteDto::id).containsExactly(shoppingNote.id(), firstNote.id());
        assertThat(tagMatches)
                .extracting(NoteDto::tags)
                .containsExactly(List.of(), List.of("private"));

        HttpResponse<String> readNote = send("GET", "/api/notes/" + firstNote.id(),
                loginResponse.accessToken(), null);
        assertThat(readNote.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readValue(readNote.body(), NoteDto.class)).isEqualTo(firstNote);

        HttpResponse<String> update = send("PUT", "/api/notes/" + firstNote.id(), loginResponse.accessToken(),
                new NoteRequest("Updated meeting", "Final BLUE decisions", List.of("work")));
        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readValue(update.body(), NoteDto.class).title()).isEqualTo("Updated meeting");

        HttpResponse<String> delete = send("DELETE", "/api/notes/" + firstNote.id(),
                loginResponse.accessToken(), null);
        assertThat(delete.statusCode()).isEqualTo(204);
        HttpResponse<String> readDeleted = send("GET", "/api/notes/" + firstNote.id(),
                loginResponse.accessToken(), null);
        assertThat(readDeleted.statusCode()).isEqualTo(404);

        HttpResponse<String> logout = send("POST", "/api/auth/logout", loginResponse.accessToken(),
                new TokenRequest(loginResponse.refreshToken()));
        assertThat(logout.statusCode()).isEqualTo(200);
        HttpResponse<String> refreshAfterLogout = send("POST", "/api/auth/refresh", null,
                new TokenRequest(loginResponse.refreshToken()));
        assertThat(refreshAfterLogout.statusCode()).isEqualTo(401);
    }

    private NoteDto createNote(String accessToken, NoteRequest request) throws Exception {
        HttpResponse<String> response = send("POST", "/api/notes", accessToken, request);
        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readValue(response.body(), NoteDto.class);
    }

    private List<NoteDto> readNotes(String accessToken, String path) throws Exception {
        HttpResponse<String> response = send("GET", path, accessToken, null);
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readValue(response.body(), new TypeReference<>() {
        });
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
                    .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
