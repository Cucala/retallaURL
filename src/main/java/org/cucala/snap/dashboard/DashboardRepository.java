package org.cucala.snap.dashboard;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardRepository {

    private final String jdbcUrl;

    public DashboardRepository(String dbName) {
        this.jdbcUrl = "jdbc:sqlite:" + dbName;
        ensureIndex();
    }

    private void ensureIndex() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_short_urls_owner ON short_urls(owner_email)");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear el índice de short_urls", e);
        }
    }

    public int totalUrlsByOwner(String ownerEmail) {
        String sql = "SELECT COUNT(*) FROM short_urls WHERE owner_email = ?";
        return queryCount(sql, ownerEmail);
    }

    public int totalClicksByOwner(String ownerEmail) {
        String sql = """
                SELECT COUNT(*)
                FROM clicks c
                JOIN short_urls u ON c.code = u.code
                WHERE u.owner_email = ?
                """;
        return queryCount(sql, ownerEmail);
    }

    public int activeUrlsByOwner(String ownerEmail) {
        String sql = """
                SELECT COUNT(DISTINCT c.code)
                FROM clicks c
                JOIN short_urls u ON c.code = u.code
                WHERE u.owner_email = ?
                """;
        return queryCount(sql, ownerEmail);
    }

    public List<DailyCount> clicksPerDayForOwner(String ownerEmail) {
        String sql = """
                SELECT DATE(c.clicked_at) AS day, COUNT(*) AS count
                FROM clicks c
                JOIN short_urls u ON c.code = u.code
                WHERE u.owner_email = ?
                  AND c.clicked_at >= DATE('now', '-6 days')
                GROUP BY day
                ORDER BY day ASC
                """;
        return queryDailyCounts(sql, ownerEmail);
    }

    public List<DailyCount> urlsCreatedPerDayForOwner(String ownerEmail) {
        String sql = """
                SELECT DATE(created_at) AS day, COUNT(*) AS count
                FROM short_urls
                WHERE owner_email = ?
                  AND created_at >= DATE('now', '-29 days')
                GROUP BY day
                ORDER BY day ASC
                """;
        return queryDailyCounts(sql, ownerEmail);
    }

    private int queryCount(String sql, String ownerEmail) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerEmail);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al ejecutar query de conteo", e);
        }
    }

    private List<DailyCount> queryDailyCounts(String sql, String ownerEmail) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerEmail);
            try (ResultSet rs = stmt.executeQuery()) {
                List<DailyCount> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new DailyCount(rs.getString("day"), rs.getInt("count")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al ejecutar query de serie temporal", e);
        }
    }
}
