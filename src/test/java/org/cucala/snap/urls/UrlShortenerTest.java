package org.cucala.snap.urls;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerTest {

    private static final String OWNER = "owner@test.com";
    private static final String OTHER = "other@test.com";

    private Path dbFile;
    private UrlShortener shortener;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("snap-test-", ".db");
        shortener = new UrlShortener(new UrlRepository(dbFile.toString()));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(dbFile);
    }

    @Test
    void shortenGeneraCodigoDeSeisCaracteres() {
        ShortUrl url = shortener.shorten("https://example.com", OWNER);

        assertEquals(6, url.code().length());
    }

    @Test
    void shortenGuardaLaUrlLarga() {
        ShortUrl url = shortener.shorten("https://example.com/muy/largo", OWNER);

        assertEquals("https://example.com/muy/largo", url.longUrl());
    }

    @Test
    void shortenAsociaElPropietario() {
        ShortUrl url = shortener.shorten("https://example.com", OWNER);

        assertEquals(OWNER, url.ownerEmail());
    }

    @Test
    void shortenPersistsEnBaseDeDatos() {
        ShortUrl created = shortener.shorten("https://example.com", OWNER);

        Optional<ShortUrl> found = shortener.resolve(created.code());

        assertTrue(found.isPresent());
        assertEquals(created.longUrl(), found.get().longUrl());
        assertEquals(OWNER, found.get().ownerEmail());
    }

    @Test
    void dosAcortamientosGeneranCodigosDiferentes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            codes.add(shortener.shorten("https://example.com/" + i, OWNER).code());
        }

        assertEquals(10, codes.size());
    }

    @Test
    void resolveDevuelveEmptyParaCodigoInexistente() {
        Optional<ShortUrl> found = shortener.resolve("noexiste");

        assertTrue(found.isEmpty());
    }

    @Test
    void listAllDevuelveTodosLosAcortamientos() {
        shortener.shorten("https://a.com", OWNER);
        shortener.shorten("https://b.com", OWNER);
        shortener.shorten("https://c.com", OWNER);

        assertEquals(3, shortener.listAll().size());
    }

    @Test
    void listAllDevuelveListaVaciaSiNoHayNada() {
        assertTrue(shortener.listAll().isEmpty());
    }

    @Test
    void deleteDelPropietarioDevuelveOk() {
        ShortUrl url = shortener.shorten("https://example.com", OWNER);

        DeleteResult result = shortener.delete(url.code(), OWNER);

        assertEquals(DeleteResult.OK, result);
        assertTrue(shortener.resolve(url.code()).isEmpty());
    }

    @Test
    void deleteDeOtroUsuarioDevuelveForbidden() {
        ShortUrl url = shortener.shorten("https://example.com", OWNER);

        DeleteResult result = shortener.delete(url.code(), OTHER);

        assertEquals(DeleteResult.FORBIDDEN, result);
        assertTrue(shortener.resolve(url.code()).isPresent());
    }

    @Test
    void deleteDeCodigoInexistenteDevuelveNotFound() {
        DeleteResult result = shortener.delete("noexiste", OWNER);

        assertEquals(DeleteResult.NOT_FOUND, result);
    }
}
