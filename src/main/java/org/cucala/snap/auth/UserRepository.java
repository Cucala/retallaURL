package org.cucala.snap.auth;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;

public class UserRepository {

    private final String jdbcUrl;

    public UserRepository(String dbName) {
        this.jdbcUrl = "jdbc:sqlite:" + dbName;
        createTable();
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    email         TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    name          TEXT    NOT NULL,
                    created_at    TEXT    NOT NULL
                )
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la tabla users", e);
        }
    }

    public User save(String email, String passwordHash, String name) {
        String sql = "INSERT INTO users (email, password_hash, name, created_at) VALUES (?, ?, ?, ?)";
        String createdAt = Instant.now().toString();
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, email);
            stmt.setString(2, passwordHash);
            stmt.setString(3, name);
            stmt.setString(4, createdAt);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                long id = keys.getLong(1);
                return new User(id, email, passwordHash, name, Instant.parse(createdAt));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el usuario", e);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, name, created_at FROM users WHERE email = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getString("name"),
                            Instant.parse(rs.getString("created_at"))
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al buscar el usuario: " + email, e);
        }
    }
}
