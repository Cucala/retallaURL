package org.cucala.snap.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.cucala.snap.config.AppConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class SnapServer {

    private final HttpServer http;
    private final boolean isDevelopment;

    public SnapServer(AppConfig config) throws IOException {
        this(config.getPort(), config.isDevelopment());
    }

    // Constructor para tests: puerto aleatorio, modo desarrollo
    SnapServer(int port) throws IOException {
        this(port, true);
    }

    private SnapServer(int port, boolean isDevelopment) throws IOException {
        this.isDevelopment = isDevelopment;
        http = HttpServer.create(new InetSocketAddress(port), 0);
        http.createContext("/health", wrap(new HealthHandler()));
        http.createContext("/", wrap(new NotFoundHandler()));
        http.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public void createContext(String path, HttpHandler handler) {
        http.createContext(path, wrap(handler));
    }

    public void setFallbackHandler(HttpHandler handler) {
        http.removeContext("/");
        http.createContext("/", wrap(handler));
    }

    public void start() {
        http.start();
    }

    public void stop() {
        http.stop(0);
    }

    public int getPort() {
        return http.getAddress().getPort();
    }

    private HttpHandler wrap(HttpHandler handler) {
        return new RequestLoggingHandler(new ErrorHandler(handler, isDevelopment));
    }
}
