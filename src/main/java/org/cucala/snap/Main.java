package org.cucala.snap;

import org.cucala.snap.config.AppConfig;
import org.cucala.snap.server.SnapServer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        AppConfig config = AppConfig.load();
        SnapServer server = new SnapServer(config);
        server.start();
        System.out.println("Snap escuchando en http://localhost:" + config.getPort()
                + "  [" + config.getEnv() + "]");
    }
}
