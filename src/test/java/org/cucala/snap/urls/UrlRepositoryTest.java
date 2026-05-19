package org.cucala.snap.urls;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UrlRepositoryTest {

    private Path dbFile;
    private UrlRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("snap-test-", ".db");
        repo = new UrlRepository(dbFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(dbFile);
    }

    @Test
    void guardaYRecuperaPorCodigo() {
        ShortUrl url = new ShortUrl("abc123", "https://example.com/largo",
                Instant.parse("2024-01-01T10:00:00Z"), "owner@test.com");
        repo.save(url);

        Optional<ShortUrl> found = repo.findByCode("abc123");

        assertTrue(found.isPresent());
        assertEquals("abc123", found.get().code());
        assertEquals("https://example.com/largo", found.get().longUrl());
        assertEquals(Instant.parse("2024-01-01T10:00:00Z"), found.get().createdAt());
        assertEquals("owner@test.com", found.get().ownerEmail());
    }

    @Test
    void findByCodigoInexistenteDevuelveEmpty() {
        Optional<ShortUrl> found = repo.findByCode("noexiste");

        assertTrue(found.isEmpty());
    }

    @Test
    void findAllDevuelveTodas() {
        repo.save(new ShortUrl("aaa111", "https://a.com", Instant.parse("2024-01-01T10:00:00Z"), "a@test.com"));
        repo.save(new ShortUrl("bbb222", "https://b.com", Instant.parse("2024-01-01T11:00:00Z"), "b@test.com"));
        repo.save(new ShortUrl("ccc333", "https://c.com", Instant.parse("2024-01-01T12:00:00Z"), "c@test.com"));

        List<ShortUrl> all = repo.findAll();

        assertEquals(3, all.size());
    }

    @Test
    void findAllDevuelveOrdenDescendentePorFecha() {
        repo.save(new ShortUrl("primero", "https://primero.com", Instant.parse("2024-01-01T08:00:00Z"), "a@test.com"));
        repo.save(new ShortUrl("segundo", "https://segundo.com", Instant.parse("2024-01-01T09:00:00Z"), "a@test.com"));
        repo.save(new ShortUrl("tercero", "https://tercero.com", Instant.parse("2024-01-01T10:00:00Z"), "a@test.com"));

        List<ShortUrl> all = repo.findAll();

        assertEquals("tercero", all.get(0).code());
        assertEquals("primero", all.get(2).code());
    }

    @Test
    void findAllDevuelveListaVaciaSiNoHayUrls() {
        List<ShortUrl> all = repo.findAll();

        assertTrue(all.isEmpty());
    }

    @Test
    void deleteByCodeBorraLaUrl() {
        repo.save(new ShortUrl("tobedeleted", "https://example.com",
                Instant.parse("2024-01-01T10:00:00Z"), "owner@test.com"));

        repo.deleteByCode("tobedeleted");

        assertTrue(repo.findByCode("tobedeleted").isEmpty());
    }

    @Test
    void crearTablaEsIdempotente() {
        assertDoesNotThrow(() -> new UrlRepository(dbFile.toString()));
    }
}
