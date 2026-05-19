package org.cucala.snap.server;

import org.cucala.snap.config.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlingTest {

    private SnapServer server;
    private HttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new SnapServer(0); // desarrollo, puerto aleatorio
        server.createContext("/bomb", exchange -> {
            throw new RuntimeException("error de prueba");
        });
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void rutaNoRegistradaDevuelve404() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/ruta-inexistente"))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void excepcionNoControladaDevuelve500() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/bomb"))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(500, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void servidorSigueFuncionandoTrasError() throws IOException, InterruptedException {
        var bomb = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/bomb"))
                .GET()
                .build();
        client.send(bomb, HttpResponse.BodyHandlers.discarding());

        var health = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/health"))
                .GET()
                .build();
        var response = client.send(health, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
    }

    @Test
    void modoDesarrolloIncluyeMensajeDeError() throws IOException, InterruptedException {
        // SnapServer(0) arranca en modo desarrollo, el mensaje del error es visible
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/bomb"))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(500, response.statusCode());
        assertTrue(response.body().contains("error de prueba"));
    }

    @Test
    void modoProduccionNoExponeMensajeDeError() throws IOException, InterruptedException {
        var prodConfig = AppConfig.from(Map.of(
                "APP_ENV", "production",
                "DB_NAME", "test.db",
                "PORT", "0",
                "JWT_SECRET", "test-prod-jwt-secret-must-32bytes!!"
        ));
        var prodServer = new SnapServer(prodConfig);
        prodServer.createContext("/bomb", exchange -> {
            throw new RuntimeException("error de prueba");
        });
        prodServer.start();
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + prodServer.getPort() + "/bomb"))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(500, response.statusCode());
            assertFalse(response.body().contains("error de prueba"));
            assertTrue(response.body().contains("Internal server error"));
        } finally {
            prodServer.stop();
        }
    }
}
