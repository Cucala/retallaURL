package org.cucala.snap.urls;

import org.cucala.snap.auth.AuthService;
import org.cucala.snap.auth.JwtVerifier;
import org.cucala.snap.auth.UserRepository;
import org.cucala.snap.config.AppConfig;
import org.cucala.snap.dashboard.ClickRepository;
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

class UrlsHandlerTest {

    private static final String SECRET      = "test-urls-handler-jwt-secret-32b!!";
    private static final String OWNER_EMAIL = "owner@test.com";
    private static final String OTHER_EMAIL = "other@test.com";

    private SnapServer server;
    private HttpClient client;
    private UrlShortener shortener;
    private Path dbFile;
    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("snap-test-", ".db");

        UserRepository userRepo = new UserRepository(dbFile.toString());
        AuthService authService = new AuthService(userRepo, SECRET);
        ownerToken = authService.register(OWNER_EMAIL, "password123", "Owner").token();
        otherToken = authService.register(OTHER_EMAIL, "password123", "Other").token();

        shortener = new UrlShortener(new UrlRepository(dbFile.toString()));
        JwtVerifier verifier = new JwtVerifier(SECRET);
        ClickRepository clickRepository = new ClickRepository(dbFile.toString());

        server = new SnapServer(AppConfig.from(Map.of("PORT", "0", "DB_NAME", "test.db")));
        server.createContext("/urls", new UrlsHandler(shortener, verifier));
        server.setFallbackHandler(new RedirectHandler(shortener, clickRepository));
        server.start();

        client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.stop();
        Files.deleteIfExists(dbFile);
    }

    // ── POST /urls ────────────────────────────────────────────────────────────

    @Test
    void postUrlsDevuelve201ConCodigo() throws IOException, InterruptedException {
        var response = postUrls("{\"url\":\"https://example.com/largo\"}", ownerToken);

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"code\""));
        assertTrue(response.body().contains("\"longUrl\""));
        assertTrue(response.body().contains("https://example.com/largo"));
    }

    @Test
    void postUrlsGeneraCodigoDeSeisCaracteres() throws IOException, InterruptedException {
        var response = postUrls("{\"url\":\"https://example.com\"}", ownerToken);

        String code = UrlsHandler.extractJsonField(response.body(), "code");
        assertNotNull(code);
        assertEquals(6, code.length());
    }

    @Test
    void postUrlsSinTokenDevuelve401() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/urls"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"url\":\"https://example.com\"}"))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    @Test
    void postUrlsSinCampoUrlDevuelve400() throws IOException, InterruptedException {
        var response = postUrls("{\"otra\":\"cosa\"}", ownerToken);

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void postUrlsConCuerpoVacioDevuelve400() throws IOException, InterruptedException {
        var response = postUrls("{}", ownerToken);

        assertEquals(400, response.statusCode());
    }

    @Test
    void putUrlsDevuelve405() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/urls"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    // ── GET /urls ─────────────────────────────────────────────────────────────

    @Test
    void getUrlsEsPublicoSinToken() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder().uri(uri("/urls")).GET().build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
    }

    @Test
    void getUrlsDevuelveListaVaciaSiNoHayNada() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder().uri(uri("/urls")).GET().build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void getUrlsDevuelveLasUrlsCreadas() throws IOException, InterruptedException {
        shortener.shorten("https://primero.com", OWNER_EMAIL);
        shortener.shorten("https://segundo.com", OWNER_EMAIL);

        var request = HttpRequest.newBuilder().uri(uri("/urls")).GET().build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("https://primero.com"));
        assertTrue(response.body().contains("https://segundo.com"));
    }

    // ── DELETE /urls/{code} ───────────────────────────────────────────────────

    @Test
    void deleteUrlDelPropietarioDevuelve204() throws IOException, InterruptedException {
        ShortUrl url = shortener.shorten("https://example.com", OWNER_EMAIL);

        var response = deleteUrl(url.code(), ownerToken);

        assertEquals(204, response.statusCode());
        assertTrue(shortener.resolve(url.code()).isEmpty());
    }

    @Test
    void deleteUrlDeOtroUsuarioDevuelve403() throws IOException, InterruptedException {
        ShortUrl url = shortener.shorten("https://example.com", OWNER_EMAIL);

        var response = deleteUrl(url.code(), otherToken);

        assertEquals(403, response.statusCode());
        assertTrue(shortener.resolve(url.code()).isPresent());
    }

    @Test
    void deleteUrlInexistenteDevuelve404() throws IOException, InterruptedException {
        var response = deleteUrl("noexiste", ownerToken);

        assertEquals(404, response.statusCode());
    }

    @Test
    void deleteUrlSinTokenDevuelve401() throws IOException, InterruptedException {
        ShortUrl url = shortener.shorten("https://example.com", OWNER_EMAIL);
        var request = HttpRequest.newBuilder()
                .uri(uri("/urls/" + url.code()))
                .DELETE()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(401, response.statusCode());
    }

    @Test
    void getEnCodigoUrlDevuelve405() throws IOException, InterruptedException {
        // PUT sobre /urls/{code} debería devolver 405
        var request = HttpRequest.newBuilder()
                .uri(uri("/urls/alguncod"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    // ── GET /{code} ───────────────────────────────────────────────────────────

    @Test
    void getCodigoExistenteRedirecciona302() throws IOException, InterruptedException {
        ShortUrl url = shortener.shorten("https://destino.com", OWNER_EMAIL);

        var request = HttpRequest.newBuilder().uri(uri("/" + url.code())).GET().build();

        var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(302, response.statusCode());
        assertEquals("https://destino.com", response.headers().firstValue("Location").orElse(""));
    }

    @Test
    void getCodigoInexistenteDevuelve404() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder().uri(uri("/noexiste")).GET().build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void postEnCodigoDevuelve405() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/algun-codigo"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpResponse<String> postUrls(String json, String token)
            throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/urls"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<Void> deleteUrl(String code, String token)
            throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/urls/" + code))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.getPort() + path);
    }
}
