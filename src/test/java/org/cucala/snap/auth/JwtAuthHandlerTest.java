package org.cucala.snap.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.cucala.snap.config.AppConfig;
import org.cucala.snap.server.SnapServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthHandlerTest {

    private static final String SECRET = "test-jwt-middleware-secret-32bytes!!!";
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private SnapServer server;
    private HttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        // Handler protegido que devuelve el email del atributo adjuntado por el middleware
        var protectedHandler = (com.sun.net.httpserver.HttpHandler) exchange -> {
            String email = (String) exchange.getAttribute(JwtAuthHandler.ATTR_EMAIL);
            byte[] body = ("{\"email\":\"" + email + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        };

        server = new SnapServer(AppConfig.from(Map.of("PORT", "0", "DB_NAME", "test.db")));
        server.createContext("/protected", new JwtAuthHandler(protectedHandler, SECRET));
        server.start();

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void tokenValidoPasaAlDelegateConEmailAdjunto() throws IOException, InterruptedException {
        String token = buildToken("alice@example.com", 60_000);

        var response = get("/protected", token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("alice@example.com"));
    }

    @Test
    void sinHeaderAuthorizationDevuelve401() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri("/protected"))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void tokenExpiradoDevuelve401() throws IOException, InterruptedException {
        String token = buildToken("alice@example.com", -1_000);

        var response = get("/protected", token);

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void tokenMalformadoDevuelve401() throws IOException, InterruptedException {
        var response = get("/protected", "esto.no.es.un.token.valido");

        assertEquals(401, response.statusCode());
    }

    @Test
    void headerSinPrefixoBearerDevuelve401() throws IOException, InterruptedException {
        String token = buildToken("alice@example.com", 60_000);
        var request = HttpRequest.newBuilder()
                .uri(uri("/protected"))
                .header("Authorization", token)  // falta "Bearer "
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    @Test
    void tokenFirmadoConClaveDistintaDevuelve401() throws IOException, InterruptedException {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "otra-clave-completamente-diferente-32b!!".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("hacker@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        var response = get("/protected", token);

        assertEquals(401, response.statusCode());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String buildToken(String email, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(KEY)
                .compact();
    }

    private HttpResponse<String> get(String path, String bearerToken)
            throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Authorization", "Bearer " + bearerToken)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.getPort() + path);
    }
}
