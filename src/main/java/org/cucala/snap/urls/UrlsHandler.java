package org.cucala.snap.urls;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.cucala.snap.auth.JwtVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class UrlsHandler implements HttpHandler {

    private final UrlShortener shortener;
    private final JwtVerifier verifier;

    public UrlsHandler(UrlShortener shortener, JwtVerifier verifier) {
        this.shortener = shortener;
        this.verifier = verifier;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/urls".equals(path)) {
            switch (method) {
                case "POST" -> handlePost(exchange);
                case "GET"  -> handleGet(exchange);
                default     -> { exchange.sendResponseHeaders(405, -1); exchange.close(); }
            }
        } else if (path.startsWith("/urls/")) {
            String code = path.substring("/urls/".length());
            if ("GET".equals(method) && "mine".equals(code)) {
                handleGetMine(exchange);
            } else if ("DELETE".equals(method)) {
                handleDelete(exchange, code);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String email = requireAuth(exchange);
        if (email == null) return;

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String longUrl = extractJsonField(body, "url");

        if (longUrl == null || longUrl.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"Falta el campo url\"}");
            return;
        }

        String alias = extractJsonField(body, "alias");
        ShortUrl created;
        if (alias != null && !alias.isBlank()) {
            if (!alias.matches("[a-zA-Z0-9-]{3,30}")) {
                sendJson(exchange, 400, "{\"error\":\"El alias solo puede contener letras, números y guiones (3-30 caracteres)\"}");
                return;
            }
            if (shortener.resolve(alias).isPresent()) {
                sendJson(exchange, 409, "{\"error\":\"El alias ya está en uso\"}");
                return;
            }
            created = shortener.shorten(longUrl, email, alias);
        } else {
            created = shortener.shorten(longUrl, email);
        }
        sendJson(exchange, 201, toJson(created));
    }

    private void handleGetMine(HttpExchange exchange) throws IOException {
        String email = requireAuth(exchange);
        if (email == null) return;
        sendJson(exchange, 200, toJsonArray(shortener.listByOwner(email)));
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        List<ShortUrl> urls = shortener.listAll();
        sendJson(exchange, 200, toJsonArray(urls));
    }

    private void handleDelete(HttpExchange exchange, String code) throws IOException {
        String email = requireAuth(exchange);
        if (email == null) return;

        switch (shortener.delete(code, email)) {
            case OK        -> { exchange.sendResponseHeaders(204, -1); exchange.close(); }
            case NOT_FOUND -> sendJson(exchange, 404, "{\"error\":\"URL no encontrada\"}");
            case FORBIDDEN -> sendJson(exchange, 403, "{\"error\":\"No tienes permiso para borrar esta URL\"}");
        }
    }

    private String requireAuth(HttpExchange exchange) throws IOException {
        Optional<String> email = verifier.extractEmail(exchange);
        if (email.isEmpty()) {
            sendJson(exchange, 401, "{\"error\":\"Token de autenticación ausente o inválido\"}");
            return null;
        }
        return email.get();
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx + key.length());
        if (colon == -1) return null;
        int quote1 = json.indexOf('"', colon + 1);
        if (quote1 == -1) return null;
        int quote2 = json.indexOf('"', quote1 + 1);
        if (quote2 == -1) return null;
        return json.substring(quote1 + 1, quote2);
    }

    static String toJson(ShortUrl url) {
        return "{\"code\":" + jsonString(url.code())
             + ",\"longUrl\":" + jsonString(url.longUrl())
             + ",\"createdAt\":" + jsonString(url.createdAt().toString()) + "}";
    }

    private static String toJsonArray(List<ShortUrl> urls) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < urls.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(urls.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
