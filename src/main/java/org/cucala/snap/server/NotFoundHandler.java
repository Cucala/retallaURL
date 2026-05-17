package org.cucala.snap.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class NotFoundHandler implements HttpHandler {

    private static final byte[] BODY = "{\"error\":\"Not found\"}".getBytes();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(404, BODY.length);
        exchange.getResponseBody().write(BODY);
        exchange.close();
    }
}
