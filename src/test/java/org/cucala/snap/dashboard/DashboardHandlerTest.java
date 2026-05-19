package org.cucala.snap.dashboard;

import org.cucala.snap.auth.AuthService;
import org.cucala.snap.auth.JwtVerifier;
import org.cucala.snap.auth.UserRepository;
import org.cucala.snap.config.AppConfig;
import org.cucala.snap.server.SnapServer;
import org.cucala.snap.urls.RedirectHandler;
import org.cucala.snap.urls.UrlRepository;
import org.cucala.snap.urls.UrlShortener;
import org.cucala.snap.urls.UrlsHandler;
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

class DashboardHandlerTest {

    private static final String SECRET = "test-dashboard-handler-jwt-secret!!";
    private static final String EMAIL  = "user@test.com";
    private static final String OTHER  = "other@test.com";

    private SnapServer server;
    private HttpClient client;
    private UrlShortener shortener;
    private ClickRepository clickRepository;
    private Path dbFile;
    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("snap-dashboard-test-", ".db");

        UserRepository userRepo = new UserRepository(dbFile.toString());
        AuthService authService = new AuthService(userRepo, SECRET);
        token      = authService.register(EMAIL, "pass123", "User").token();
        otherToken = authService.register(OTHER, "pass123", "Other").token();

        UrlRepository urlRepo = new UrlRepository(dbFile.toString());
        shortener = new UrlShortener(urlRepo);
        clickRepository = new ClickRepository(dbFile.toString());
        DashboardRepository dashRepo = new DashboardRepository(dbFile.toString());
        JwtVerifier verifier = new JwtVerifier(SECRET);

        server = new SnapServer(AppConfig.from(Map.of("PORT", "0", "DB_NAME", "test.db")));
        server.createContext("/urls",      new UrlsHandler(shortener, verifier));
        server.createContext("/dashboard", new DashboardHandler(dashRepo, verifier));
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
        // Borrar también el archivo WAL que SQLite puede crear
        Files.deleteIfExists(Path.of(dbFile + "-wal"));
        Files.deleteIfExists(Path.of(dbFile + "-shm"));
    }

    // ── Sin autenticación ─────────────────────────────────────────────────────

    @Test
    void sinTokenDevuelve401() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder().uri(uri("/dashboard")).GET().build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void conTokenInvalidoDevuelve401() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/dashboard"))
                .header("Authorization", "Bearer token.invalido.xyz")
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    // ── Usuario sin URLs ──────────────────────────────────────────────────────

    @Test
    void usuarioSinUrlsDevuelveTodoACero() throws IOException, InterruptedException {
        var response = getDashboard(token);

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"totalUrls\":0"));
        assertTrue(body.contains("\"totalClicks\":0"));
        assertTrue(body.contains("\"activeUrls\":0"));
        assertTrue(body.contains("\"clicksLast7Days\":[]"));
        assertTrue(body.contains("\"urlsCreatedLast30Days\":[]"));
    }

    // ── Usuario con URLs y clicks ─────────────────────────────────────────────

    @Test
    void totalUrlsRefleja2UrlsCreadas() throws IOException, InterruptedException {
        shortener.shorten("https://uno.com", EMAIL);
        shortener.shorten("https://dos.com", EMAIL);

        var response = getDashboard(token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"totalUrls\":2"));
    }

    @Test
    void totalClicksRefleja3Clicks() throws IOException, InterruptedException {
        var url = shortener.shorten("https://ejemplo.com", EMAIL);
        clickRepository.save(url.code());
        clickRepository.save(url.code());
        clickRepository.save(url.code());

        var response = getDashboard(token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"totalClicks\":3"));
    }

    @Test
    void activeUrlsRefleja1UrlConClicksYOtrasSin() throws IOException, InterruptedException {
        var urlActiva  = shortener.shorten("https://activa.com", EMAIL);
        shortener.shorten("https://inactiva.com", EMAIL);
        clickRepository.save(urlActiva.code());

        var response = getDashboard(token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"totalUrls\":2"));
        assertTrue(response.body().contains("\"activeUrls\":1"));
    }

    @Test
    void clicksDeOtroUsuarioNoContaminanDashboard() throws IOException, InterruptedException {
        var urlOtro = shortener.shorten("https://otro.com", OTHER);
        clickRepository.save(urlOtro.code());
        clickRepository.save(urlOtro.code());

        var response = getDashboard(token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"totalUrls\":0"));
        assertTrue(response.body().contains("\"totalClicks\":0"));
    }

    @Test
    void clicksLast7DaysContieneEntradasDelDiaDeHoy() throws IOException, InterruptedException {
        var url = shortener.shorten("https://hoy.com", EMAIL);
        clickRepository.save(url.code());
        clickRepository.save(url.code());

        var response = getDashboard(token);

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertFalse(body.contains("\"clicksLast7Days\":[]"),
                "Se esperaban entradas en clicksLast7Days");
        assertTrue(body.contains("\"count\":2"));
    }

    @Test
    void urlsCreatedLast30DaysContieneEntradasDelDiaDeHoy() throws IOException, InterruptedException {
        shortener.shorten("https://nueva.com", EMAIL);

        var response = getDashboard(token);

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertFalse(body.contains("\"urlsCreatedLast30Days\":[]"),
                "Se esperaban entradas en urlsCreatedLast30Days");
    }

    @Test
    void metodoPOSTDevuelve405() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/dashboard"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpResponse<String> getDashboard(String bearerToken)
            throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/dashboard"))
                .header("Authorization", "Bearer " + bearerToken)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.getPort() + path);
    }
}
