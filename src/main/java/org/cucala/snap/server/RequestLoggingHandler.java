package org.cucala.snap.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class RequestLoggingHandler implements HttpHandler {

    private final HttpHandler delegate;

    public RequestLoggingHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long start = System.currentTimeMillis();
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        try {
            delegate.handle(exchange);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            int status = exchange.getResponseCode();
            System.out.println(method + " " + path + " → " + status + " (" + elapsed + "ms)");
        }
    }
}
