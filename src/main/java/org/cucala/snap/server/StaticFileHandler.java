package org.cucala.snap.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".html", "text/html; charset=utf-8",
            ".css",  "text/css",
            ".js",   "application/javascript"
    );

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String uriPath = exchange.getRequestURI().getPath();
        String filePath = uriPath.substring("/app".length());
        if (filePath.isEmpty() || filePath.equals("/")) {
            filePath = "/login.html";
        }

        if (filePath.contains("..")) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        InputStream in = StaticFileHandler.class.getResourceAsStream("/static" + filePath);
        if (in == null) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        String ext = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf('.')) : "";
        String contentType = CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");

        byte[] body = in.readAllBytes();
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
