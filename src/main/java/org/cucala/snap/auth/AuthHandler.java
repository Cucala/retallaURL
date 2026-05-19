package org.cucala.snap.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AuthHandler implements HttpHandler {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AuthService service;

    public AuthHandler(AuthService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        switch (exchange.getRequestURI().getPath()) {
            case "/auth/register" -> handleRegister(exchange);
            case "/auth/login"    -> handleLogin(exchange);
            default               -> { exchange.sendResponseHeaders(404, -1); exchange.close(); }
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String email    = extractJsonField(body, "email");
        String password = extractJsonField(body, "password");
        String name     = extractJsonField(body, "name");

        String validationError = validateRegister(email, password, name);
        if (validationError != null) {
            sendJson(exchange, 400, errorJson(validationError));
            return;
        }

        try {
            AuthResult result = service.register(email, password, name);
            sendJson(exchange, 201, toJson(result));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 409, errorJson(e.getMessage()));
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String email    = extractJsonField(body, "email");
        String password = extractJsonField(body, "password");

        if (isBlank(email) || isBlank(password)) {
            sendJson(exchange, 400, errorJson("Los campos email y password son obligatorios"));
            return;
        }

        try {
            AuthResult result = service.login(email, password);
            sendJson(exchange, 200, toJson(result));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 401, errorJson("Credenciales incorrectas"));
        }
    }

    private static String validateRegister(String email, String password, String name) {
        if (isBlank(email))    return "El campo email es obligatorio";
        if (!isValidEmail(email)) return "El email no tiene un formato válido";
        if (isBlank(password)) return "El campo password es obligatorio";
        if (password.length() < MIN_PASSWORD_LENGTH)
            return "El password debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres";
        if (isBlank(name))     return "El campo name es obligatorio";
        return null;
    }

    private static boolean isValidEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 && at < email.length() - 1;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String toJson(AuthResult result) {
        User u = result.user();
        return "{\"token\":" + jsonString(result.token())
             + ",\"user\":{\"id\":" + u.id()
             + ",\"email\":" + jsonString(u.email())
             + ",\"name\":" + jsonString(u.name())
             + ",\"createdAt\":" + jsonString(u.createdAt().toString()) + "}}";
    }

    private static String errorJson(String message) {
        return "{\"error\":" + jsonString(message) + "}";
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
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

    static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
