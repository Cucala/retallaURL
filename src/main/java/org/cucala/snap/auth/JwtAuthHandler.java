package org.cucala.snap.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class JwtAuthHandler implements HttpHandler {

    public static final String ATTR_EMAIL = "authenticatedEmail";

    private static final byte[] UNAUTHORIZED_BODY =
            "{\"error\":\"Token de autenticación ausente o inválido\"}".getBytes(StandardCharsets.UTF_8);

    private final HttpHandler delegate;
    private final JwtVerifier verifier;

    public JwtAuthHandler(HttpHandler delegate, String jwtSecret) {
        this.delegate = delegate;
        this.verifier = new JwtVerifier(jwtSecret);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Optional<String> email = verifier.extractEmail(exchange);
        if (email.isEmpty()) {
            sendUnauthorized(exchange);
            return;
        }
        exchange.setAttribute(ATTR_EMAIL, email.get());
        delegate.handle(exchange);
    }

    private static void sendUnauthorized(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(401, UNAUTHORIZED_BODY.length);
        exchange.getResponseBody().write(UNAUTHORIZED_BODY);
        exchange.close();
    }
}
