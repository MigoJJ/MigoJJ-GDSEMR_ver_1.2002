package com.emr.gds.features.history.adapter.out.persistence;

import com.emr.gds.core.db.AppDatabaseManager;
import com.emr.gds.features.history.domain.ConditionCategory;
import com.emr.gds.features.history.domain.HistoryPersistenceException;
import com.emr.gds.features.history.domain.HistoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcHistoryRepository implements HistoryRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcHistoryRepository.class.getName());
    private final AppDatabaseManager dbManager;

    public JdbcHistoryRepository() {
        this(AppDatabaseManager.getInstance());
    }

    public JdbcHistoryRepository(Path dbFile) {
        this(new AppDatabaseManager(dbFile.toAbsolutePath().normalize().getParent()));
    }

    public JdbcHistoryRepository(AppDatabaseManager dbManager) {
        this.dbManager = dbManager;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS conditions (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "category TEXT NOT NULL, " +
                     "name TEXT NOT NULL, " +
                     "UNIQUE(category, name))";
        try (Connection conn = dbManager.getHistoryConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize history database.", e);
            throw new HistoryPersistenceException("Failed to initialize history database.", e);
        }
    }

    @Override
    public List<String> getConditionsByCategory(ConditionCategory category) {
        List<String> results = new ArrayList<>();
        String sql = "SELECT name FROM conditions WHERE category = ? ORDER BY name";
        
        try (Connection conn = dbManager.getHistoryConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, category.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load conditions for " + category + ".", e);
            throw new HistoryPersistenceException("Failed to load conditions for " + category + ".", e);
        }
        return results;
    }

    @Override
    public void addCondition(ConditionCategory category, String conditionName) {
        String sql = "INSERT OR IGNORE INTO conditions (category, name) VALUES (?, ?)";
        
        try (Connection conn = dbManager.getHistoryConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, category.name());
            pstmt.setString(2, conditionName);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to add condition '" + conditionName + "' for " + category + ".", e);
            throw new HistoryPersistenceException("Failed to add condition '" + conditionName + "' for " + category + ".", e);
        }
    }

    @Override
    public void updateCondition(ConditionCategory category, String oldName, String newName) {
        String sql = "UPDATE conditions SET name = ? WHERE category = ? AND name = ?";
        try (Connection conn = dbManager.getHistoryConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newName);
            pstmt.setString(2, category.name());
            pstmt.setString(3, oldName);
            int rows = pstmt.executeUpdate();
            
            if (rows == 0) {
                throw new HistoryPersistenceException("Condition '" + oldName + "' not found in category " + category);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update condition '" + oldName + "' to '" + newName + "'.", e);
            throw new HistoryPersistenceException("Failed to update condition.", e);
        }
    }

    @Override
    public void deleteCondition(ConditionCategory category, String conditionName) {
        String sql = "DELETE FROM conditions WHERE category = ? AND name = ?";
        try (Connection conn = dbManager.getHistoryConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, category.name());
            pstmt.setString(2, conditionName);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete condition '" + conditionName + "'.", e);
            throw new HistoryPersistenceException("Failed to delete condition.", e);
        }
    }
}
