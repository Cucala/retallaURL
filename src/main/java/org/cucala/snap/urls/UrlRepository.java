package org.cucala.snap.urls;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UrlRepository {

    private final String jdbcUrl;

    public UrlRepository(String dbName) {
        this.jdbcUrl = "jdbc:sqlite:" + dbName;
        createTable();
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS short_urls (
                    code        TEXT PRIMARY KEY,
                    long_url    TEXT NOT NULL,
                    created_at  TEXT NOT NULL,
                    owner_email TEXT NOT NULL
                )
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la tabla short_urls", e);
        }
    }

    public void save(ShortUrl url) {
        String sql = "INSERT INTO short_urls (code, long_url, created_at, owner_email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, url.code());
            stmt.setString(2, url.longUrl());
            stmt.setString(3, url.createdAt().toString());
            stmt.setString(4, url.ownerEmail());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la URL", e);
        }
    }

    public Optional<ShortUrl> findByCode(String code) {
        String sql = "SELECT code, long_url, created_at, owner_email FROM short_urls WHERE code = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar la URL con código: " + code, e);
        }
    }

    public void deleteByCode(String code) {
        String sql = "DELETE FROM short_urls WHERE code = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error al borrar la URL con código: " + code, e);
        }
    }

    public List<ShortUrl> findAll() {
        String sql = "SELECT code, long_url, created_at, owner_email FROM short_urls ORDER BY created_at DESC";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<ShortUrl> urls = new ArrayList<>();
            while (rs.next()) {
                urls.add(map(rs));
            }
            return urls;
        } catch (SQLException e) {
            throw new IllegalStateException("Error al listar las URLs", e);
        }
    }

    private static ShortUrl map(ResultSet rs) throws SQLException {
        return new ShortUrl(
                rs.getString("code"),
                rs.getString("long_url"),
                Instant.parse(rs.getString("created_at")),
                rs.getString("owner_email")
        );
    }
}
