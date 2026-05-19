package org.cucala.snap.dashboard;

import java.sql.*;
import java.time.Instant;

public class ClickRepository {

    private final String jdbcUrl;

    public ClickRepository(String dbName) {
        this.jdbcUrl = "jdbc:sqlite:" + dbName;
        createTable();
    }

    private void createTable() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS clicks (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        code       TEXT    NOT NULL,
                        clicked_at TEXT    NOT NULL
                    )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clicks_code       ON clicks(code)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clicks_clicked_at ON clicks(clicked_at)");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la tabla clicks", e);
        }
    }

    public void save(String code) {
        String sql = "INSERT INTO clicks (code, clicked_at) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            stmt.setString(2, Instant.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el click para el código: " + code, e);
        }
    }
}
