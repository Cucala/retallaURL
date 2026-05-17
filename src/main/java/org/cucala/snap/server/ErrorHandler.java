package org.cucala.snap.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class ErrorHandler implements HttpHandler {

    private final HttpHandler delegate;
    private final boolean isDevelopment;

    public ErrorHandler(HttpHandler delegate, boolean isDevelopment) {
        this.delegate = delegate;
        this.isDevelopment = isDevelopment;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            delegate.handle(exchange);
        } catch (Exception e) {
            sendInternalError(exchange, e);
        }
    }

    private void sendInternalError(HttpExchange exchange, Exception e) {
        try {
            if (exchange.getResponseCode() == -1) {
                byte[] body = buildErrorBody(e);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, body.length);
                exchange.getResponseBody().write(body);
            }
        } catch (IOException ignored) {
            // conexión rota; no hay nada más que hacer
        } finally {
            exchange.close();
        }
    }

    private byte[] buildErrorBody(Exception e) {
        if (isDevelopment) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ("{\"error\":" + jsonString(msg) + "}").getBytes();
        }
        return "{\"error\":\"Internal server error\"}".getBytes();
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
