package org.cucala.snap.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private static final String SECRET = "clave-secreta-de-prueba-minimo-32-bytes!!";

    private AuthService service;
    private Path dbFile;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("snap-auth-test-", ".db");
        service = new AuthService(new UserRepository(dbFile.toString()), SECRET);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(dbFile);
    }

    @Test
    void registroExitosoDevuelveJwtConEmailComoSubject() {
        AuthResult result = service.register("alice@example.com", "pass123", "Alice");

        assertNotNull(result.token());
        Claims claims = parseClaims(result.token());
        assertEquals("alice@example.com", claims.getSubject());
        assertTrue(claims.getExpiration().after(new Date()));
        assertEquals("alice@example.com", result.user().email());
    }

    @Test
    void registroConEmailDuplicadoLanzaExcepcion() {
        service.register("dup@example.com", "pass1", "Alice");

        assertThrows(IllegalArgumentException.class,
                () -> service.register("dup@example.com", "pass2", "Bob"));
    }

    @Test
    void loginExitosoDevuelveJwtConEmailComoSubject() {
        service.register("bob@example.com", "secreta", "Bob");

        AuthResult result = service.login("bob@example.com", "secreta");

        assertNotNull(result.token());
        assertEquals("bob@example.com", parseClaims(result.token()).getSubject());
    }

    @Test
    void loginConPasswordIncorrectoLanzaExcepcion() {
        service.register("carol@example.com", "correcta", "Carol");

        assertThrows(IllegalArgumentException.class,
                () -> service.login("carol@example.com", "incorrecta"));
    }

    @Test
    void loginConEmailInexistenteLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> service.login("noexiste@example.com", "cualquier"));
    }

    @Test
    void loginConEmailEnMayusculasFunciona() {
        service.register("dave@example.com", "mipass", "Dave");

        AuthResult result = service.login("DAVE@EXAMPLE.COM", "mipass");

        assertNotNull(result.token());
        assertEquals("dave@example.com", parseClaims(result.token()).getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
