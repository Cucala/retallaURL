package org.cucala.snap.dashboard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ClickRepositoryTest {

    private Path dbFile;
    private ClickRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = Files.createTempFile("snap-test-", ".db");
        repo = new ClickRepository(dbFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(dbFile);
    }

    @Test
    void saveNoLanzaExcepcion() {
        assertDoesNotThrow(() -> repo.save("abc123"));
    }

    @Test
    void crearTablaEsIdempotente() {
        assertDoesNotThrow(() -> new ClickRepository(dbFile.toString()));
    }
}
