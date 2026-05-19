package org.cucala.snap.auth;

import org.cucala.snap.config.AppConfig;
import org.cucala.snap.server.SnapServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthHandlerTest {

    private static final String SECRET = "test-handler-secret-must-be-32bytes!!";

    private SnapServer server;
    private HttpClient client;
    private Path dbFile;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("snap-auth-handler-", ".db");
        UserRepository repo = new UserRepository(dbFile.toString());
        AuthService authService = new AuthService(repo, SECRET);

        server = new SnapServer(AppConfig.from(Map.of("PORT", "0", "DB_NAME", "test.db")));
        server.createContext("/auth", new AuthHandler(authService));
        server.start();

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.stop();
        Files.deleteIfExists(dbFile);
    }

    // ── POST /auth/register ───────────────────────────────────────────────────

    @Test
    void registerExitosoDevuelve201ConTokenYUsuario() throws IOException, InterruptedException {
        var response = post("/auth/register",
                "{\"email\":\"alice@example.com\",\"password\":\"secreta123\",\"name\":\"Alice\"}");

        assertEquals(201, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"token\""));
        assertTrue(body.contains("\"user\""));
        assertTrue(body.contains("alice@example.com"));
        assertTrue(body.contains("\"Alice\""));
        assertFalse(body.contains("password_hash"), "No debe filtrarse el hash");
        assertFalse(body.contains("passwordHash"), "No debe filtrarse el hash");
    }

    @Test
    void registerConEmailSinArrobaDevuelve400() throws IOException, InterruptedException {
        var response = post("/auth/register",
                "{\"email\":\"noesemail\",\"password\":\"secreta123\",\"name\":\"Alice\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void registerConPasswordCortoDevuelve400() throws IOException, InterruptedException {
        var response = post("/auth/register",
                "{\"email\":\"alice@example.com\",\"password\":\"corto\",\"name\":\"Alice\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("8"));
    }

    @Test
    void registerSinNameDevuelve400() throws IOException, InterruptedException {
        var response = post("/auth/register",
                "{\"email\":\"alice@example.com\",\"password\":\"secreta123\"}");

        assertEquals(400, response.statusCode());
    }

    @Test
    void registerConEmailDuplicadoDevuelve409() throws IOException, InterruptedException {
        post("/auth/register",
                "{\"email\":\"dup@example.com\",\"password\":\"secreta123\",\"name\":\"Alice\"}");

        var response = post("/auth/register",
                "{\"email\":\"dup@example.com\",\"password\":\"otrapass\",\"name\":\"Bob\"}");

        assertEquals(409, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void getEnRegisterDevuelve405() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/auth/register"))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Test
    void loginExitosoDevuelve200ConTokenYUsuario() throws IOException, InterruptedException {
        post("/auth/register",
                "{\"email\":\"bob@example.com\",\"password\":\"secreta123\",\"name\":\"Bob\"}");

        var response = post("/auth/login",
                "{\"email\":\"bob@example.com\",\"password\":\"secreta123\"}");

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"token\""));
        assertTrue(body.contains("\"user\""));
        assertTrue(body.contains("bob@example.com"));
    }

    @Test
    void loginConPasswordIncorrectoDevuelve401() throws IOException, InterruptedException {
        post("/auth/register",
                "{\"email\":\"carol@example.com\",\"password\":\"correcta123\",\"name\":\"Carol\"}");

        var response = post("/auth/login",
                "{\"email\":\"carol@example.com\",\"password\":\"incorrecta\"}");

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void loginConEmailInexistenteDevuelve401() throws IOException, InterruptedException {
        var response = post("/auth/login",
                "{\"email\":\"noexiste@example.com\",\"password\":\"cualquier\"}");

        assertEquals(401, response.statusCode());
    }

    @Test
    void loginConEmailEnMayusculasDevuelve200() throws IOException, InterruptedException {
        post("/auth/register",
                "{\"email\":\"dave@example.com\",\"password\":\"secreta123\",\"name\":\"Dave\"}");

        var response = post("/auth/login",
                "{\"email\":\"DAVE@EXAMPLE.COM\",\"password\":\"secreta123\"}");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("dave@example.com"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpResponse<String> post(String path, String json) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.getPort() + path);
    }
}
