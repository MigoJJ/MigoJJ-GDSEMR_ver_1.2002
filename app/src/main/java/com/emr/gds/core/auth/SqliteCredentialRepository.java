package com.emr.gds.core.auth;

import com.emr.gds.core.db.AppDatabaseManager;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqliteCredentialRepository implements CredentialRepository {

    private static final Logger LOGGER = Logger.getLogger(SqliteCredentialRepository.class.getName());
    private static final int SINGLETON_ROW_ID = 1;
    private final AppDatabaseManager dbManager;

    public SqliteCredentialRepository() {
        this(AppDatabaseManager.getInstance());
    }

    public SqliteCredentialRepository(Path dbFile) {
        this(new AppDatabaseManager(dbFile.toAbsolutePath().normalize().getParent()));
    }

    public SqliteCredentialRepository(AppDatabaseManager dbManager) {
        this.dbManager = dbManager;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS credential (" +
                     "id INTEGER PRIMARY KEY, " +
                     "password_hash TEXT NOT NULL)";
        try (Connection conn = dbManager.getAuthConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize auth database.", e);
            throw new IllegalStateException("Failed to initialize auth database.", e);
        }
    }

    @Override
    public boolean hasCredential() {
        String sql = "SELECT 1 FROM credential WHERE id = ?";
        try (Connection conn = dbManager.getAuthConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, SINGLETON_ROW_ID);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to check for existing credential.", e);
            throw new IllegalStateException("Failed to check for existing credential.", e);
        }
    }

    @Override
    public void setPassword(char[] password) {
        String hash = PasswordHasher.hash(password);
        String sql = "INSERT INTO credential (id, password_hash) VALUES (?, ?) " +
                     "ON CONFLICT(id) DO UPDATE SET password_hash = excluded.password_hash";
        try (Connection conn = dbManager.getAuthConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, SINGLETON_ROW_ID);
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save credential.", e);
            throw new IllegalStateException("Failed to save credential.", e);
        }
    }

    @Override
    public boolean verifyPassword(char[] password) {
        String sql = "SELECT password_hash FROM credential WHERE id = ?";
        try (Connection conn = dbManager.getAuthConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, SINGLETON_ROW_ID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHash = rs.getString("password_hash");
                return PasswordHasher.verify(password, storedHash);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to verify credential.", e);
            throw new IllegalStateException("Failed to verify credential.", e);
        }
    }
}
