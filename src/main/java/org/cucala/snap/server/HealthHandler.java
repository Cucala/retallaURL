package org.cucala.snap.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class HealthHandler implements HttpHandler {

    private static final byte[] BODY = "{\"status\":\"ok\"}".getBytes();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, BODY.length);
        exchange.getResponseBody().write(BODY);
        exchange.close();
    }
}
