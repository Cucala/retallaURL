package org.cucala.snap.dashboard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.cucala.snap.auth.JwtVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class DashboardHandler implements HttpHandler {

    private final DashboardRepository repository;
    private final JwtVerifier verifier;

    public DashboardHandler(DashboardRepository repository, JwtVerifier verifier) {
        this.repository = repository;
        this.verifier = verifier;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String email = requireAuth(exchange);
        if (email == null) return;

        int totalUrls    = repository.totalUrlsByOwner(email);
        int totalClicks  = repository.totalClicksByOwner(email);
        int activeUrls   = repository.activeUrlsByOwner(email);
        List<DailyCount> clicksLast7Days       = repository.clicksPerDayForOwner(email);
        List<DailyCount> urlsCreatedLast30Days  = repository.urlsCreatedPerDayForOwner(email);

        String json = "{"
                + "\"totalUrls\":"   + totalUrls  + ","
                + "\"totalClicks\":" + totalClicks + ","
                + "\"activeUrls\":"  + activeUrls  + ","
                + "\"clicksLast7Days\":"      + toJsonArray(clicksLast7Days)      + ","
                + "\"urlsCreatedLast30Days\":" + toJsonArray(urlsCreatedLast30Days)
                + "}";

        sendJson(exchange, 200, json);
    }

    private String requireAuth(HttpExchange exchange) throws IOException {
        Optional<String> email = verifier.extractEmail(exchange);
        if (email.isEmpty()) {
            sendJson(exchange, 401, "{\"error\":\"Token de autenticación ausente o inválido\"}");
            return null;
        }
        return email.get();
    }

    private static String toJsonArray(List<DailyCount> counts) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < counts.size(); i++) {
            if (i > 0) sb.append(",");
            DailyCount c = counts.get(i);
            sb.append("{\"date\":\"").append(c.date())
              .append("\",\"count\":").append(c.count()).append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
