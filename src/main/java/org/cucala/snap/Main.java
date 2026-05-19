package org.cucala.snap;

import org.cucala.snap.auth.AuthHandler;
import org.cucala.snap.auth.AuthService;
import org.cucala.snap.auth.JwtVerifier;
import org.cucala.snap.auth.UserRepository;
import org.cucala.snap.config.AppConfig;
import org.cucala.snap.dashboard.ClickRepository;
import org.cucala.snap.dashboard.DashboardHandler;
import org.cucala.snap.dashboard.DashboardRepository;
import org.cucala.snap.server.SnapServer;
import org.cucala.snap.urls.RedirectHandler;
import org.cucala.snap.urls.UrlRepository;
import org.cucala.snap.urls.UrlShortener;
import org.cucala.snap.urls.UrlsHandler;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        AppConfig config = AppConfig.load();

        UserRepository userRepository = new UserRepository(config.getDbName());
        AuthService authService = new AuthService(userRepository, config.getJwtSecret());

        UrlRepository repository = new UrlRepository(config.getDbName());
        UrlShortener shortener = new UrlShortener(repository);

        ClickRepository clickRepository = new ClickRepository(config.getDbName());
        DashboardRepository dashboardRepository = new DashboardRepository(config.getDbName());

        SnapServer server = new SnapServer(config);
        JwtVerifier verifier = new JwtVerifier(config.getJwtSecret());

        server.createContext("/auth", new AuthHandler(authService));
        server.createContext("/urls", new UrlsHandler(shortener, verifier));
        server.createContext("/dashboard", new DashboardHandler(dashboardRepository, verifier));
        server.setFallbackHandler(new RedirectHandler(shortener, clickRepository));
        server.start();
        System.out.println("Snap escuchando en http://localhost:" + config.getPort()
                + "  [" + config.getEnv() + "]");
    }
}
