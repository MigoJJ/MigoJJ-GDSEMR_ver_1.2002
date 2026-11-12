package com.emr.gds.repository.sqlite;

import com.emr.gds.core.db.AppDatabaseManager;
import com.emr.gds.repository.AbbreviationRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * SQLite-backed repository for abbreviations stored in the app database.
 */
public class SqliteAbbreviationRepository implements AbbreviationRepository {

    private final AppDatabaseManager dbManager;

    public SqliteAbbreviationRepository() {
        this(AppDatabaseManager.getInstance());
    }

    public SqliteAbbreviationRepository(AppDatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Map<String, String> findAll() throws SQLException {
        Map<String, String> abbreviations = new HashMap<>();
        Connection conn = dbManager.getAbbreviationConnection();
        ensureTable(conn);

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT short, full FROM abbreviations ORDER BY short")) {
            while (rs.next()) {
                abbreviations.put(rs.getString("short"), rs.getString("full"));
            }
        }
        return abbreviations;
    }

    @Override
    public void insert(String shortForm, String fullForm) throws SQLException {
        Connection conn = dbManager.getAbbreviationConnection();
        ensureTable(conn);

        String sql = "INSERT INTO abbreviations (short, full) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shortForm);
            pstmt.setString(2, fullForm);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void update(String originalShortForm, String newShortForm, String newFullForm) throws SQLException {
        Connection conn = dbManager.getAbbreviationConnection();
        ensureTable(conn);

        String sql = "UPDATE abbreviations SET short = ?, full = ? WHERE short = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newShortForm);
            pstmt.setString(2, newFullForm);
            pstmt.setString(3, originalShortForm);
            pstmt.executeUpdate();
        }
    }

    @Override
    public boolean delete(String shortForm) throws SQLException {
        Connection conn = dbManager.getAbbreviationConnection();
        ensureTable(conn);

        String sql = "DELETE FROM abbreviations WHERE short = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shortForm);
            return pstmt.executeUpdate() > 0;
        }
    }

    private void ensureTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS abbreviations (short TEXT PRIMARY KEY, full TEXT NOT NULL)");
        }
    }
}
