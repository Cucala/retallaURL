package org.cucala.snap.urls;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Optional;

public class RedirectHandler implements HttpHandler {

    private static final byte[] NOT_FOUND_BODY = "{\"error\":\"Not found\"}".getBytes();

    private final UrlShortener shortener;

    public RedirectHandler(UrlShortener shortener) {
        this.shortener = shortener;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String code = exchange.getRequestURI().getPath().substring(1);
        if (code.isBlank()) {
            sendNotFound(exchange);
            return;
        }

        Optional<ShortUrl> url = shortener.resolve(code);
        if (url.isPresent()) {
            exchange.getResponseHeaders().set("Location", url.get().longUrl());
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        } else {
            sendNotFound(exchange);
        }
    }

    private static void sendNotFound(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(404, NOT_FOUND_BODY.length);
        exchange.getResponseBody().write(NOT_FOUND_BODY);
        exchange.close();
    }
}