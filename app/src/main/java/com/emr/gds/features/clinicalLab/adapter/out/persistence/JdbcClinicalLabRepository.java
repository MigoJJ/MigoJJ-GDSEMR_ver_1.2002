package com.emr.gds.features.clinicalLab.adapter.out.persistence;

import com.emr.gds.core.db.DbPaths;
import com.emr.gds.features.clinicalLab.domain.ClinicalLabItem;
import com.emr.gds.features.clinicalLab.domain.ClinicalLabRepository;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcClinicalLabRepository implements ClinicalLabRepository {

    private static final String DEFAULT_DB_FILE_NAME = "ClinicalLabItemsSqlite3.db";

    private final Path dbFile;

    public JdbcClinicalLabRepository() {
        this(DbPaths.resolveDbPath(DEFAULT_DB_FILE_NAME));
    }

    public JdbcClinicalLabRepository(String dbFileName) {
        this(DbPaths.resolveDbPath(dbFileName));
    }

    /** Creates a repository backed by an explicit SQLite file, primarily for isolated tests. */
    public JdbcClinicalLabRepository(Path dbFile) {
        this.dbFile = dbFile.toAbsolutePath().normalize();
        initializeDatabase();
    }

    private String getDbUrl() {
        return "jdbc:sqlite:" + dbFile;
    }

    private void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS clinical_lab_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT,
                test_name TEXT,
                unit TEXT,
                male_range_low REAL,
                male_range_high REAL,
                female_range_low REAL,
                female_range_high REAL,
                male_reference_range TEXT,
                female_reference_range TEXT,
                codes TEXT,
                comments TEXT
            )
            """;
        try (Connection conn = DriverManager.getConnection(getDbUrl());
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing clinical lab schema: " + e.getMessage());
        }
    }

    @Override
    public List<ClinicalLabItem> getAllItems() {
        List<ClinicalLabItem> items = new ArrayList<>();
        String sql = "SELECT * FROM clinical_lab_items";

        try (Connection conn = DriverManager.getConnection(getDbUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching lab items: " + e.getMessage());
        }
        return items;
    }

    @Override
    public List<ClinicalLabItem> searchItems(String query) {
        List<ClinicalLabItem> items = new ArrayList<>();
        String sql = """
            SELECT *
            FROM clinical_lab_items
            WHERE COALESCE(LOWER(test_name), '') LIKE ?
               OR COALESCE(LOWER(category), '') LIKE ?
               OR COALESCE(LOWER(unit), '') LIKE ?
               OR COALESCE(LOWER(male_reference_range), '') LIKE ?
               OR COALESCE(LOWER(female_reference_range), '') LIKE ?
               OR COALESCE(LOWER(codes), '') LIKE ?
               OR COALESCE(LOWER(comments), '') LIKE ?
               OR COALESCE(CAST(male_range_low AS TEXT), '') LIKE ?
               OR COALESCE(CAST(male_range_high AS TEXT), '') LIKE ?
               OR COALESCE(CAST(female_range_low AS TEXT), '') LIKE ?
               OR COALESCE(CAST(female_range_high AS TEXT), '') LIKE ?
            """;

        try (Connection conn = DriverManager.getConnection(getDbUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String param = "%" + query.toLowerCase() + "%";
            for (int i = 1; i <= 11; i++) {
                pstmt.setString(i, param);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching lab items: " + e.getMessage());
        }
        return items;
    }

    @Override
    public void updateItem(ClinicalLabItem item) {
        String sql = "UPDATE clinical_lab_items SET category = ?, test_name = ?, unit = ?, male_range_low = ?, male_range_high = ?, female_range_low = ?, female_range_high = ?, male_reference_range = ?, female_reference_range = ?, codes = ?, comments = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(getDbUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getCategory());
            pstmt.setString(2, item.getTestName());
            pstmt.setString(3, item.getUnit());
            pstmt.setObject(4, item.getMaleRangeLow());
            pstmt.setObject(5, item.getMaleRangeHigh());
            pstmt.setObject(6, item.getFemaleRangeLow());
            pstmt.setObject(7, item.getFemaleRangeHigh());
            pstmt.setString(8, item.getMaleReferenceRange());
            pstmt.setString(9, item.getFemaleReferenceRange());
            pstmt.setString(10, item.getCodes());
            pstmt.setString(11, item.getComments());
            pstmt.setInt(12, item.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating lab item: " + e.getMessage());
        }
    }

    @Override
    public void insertItem(ClinicalLabItem item) {
        String sql = "INSERT INTO clinical_lab_items (category, test_name, unit, male_range_low, male_range_high, female_range_low, female_range_high, male_reference_range, female_reference_range, codes, comments) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(getDbUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, item.getCategory());
            pstmt.setString(2, item.getTestName());
            pstmt.setString(3, item.getUnit());
            pstmt.setObject(4, item.getMaleRangeLow());
            pstmt.setObject(5, item.getMaleRangeHigh());
            pstmt.setObject(6, item.getFemaleRangeLow());
            pstmt.setObject(7, item.getFemaleRangeHigh());
            pstmt.setString(8, item.getMaleReferenceRange());
            pstmt.setString(9, item.getFemaleReferenceRange());
            pstmt.setString(10, item.getCodes());
            pstmt.setString(11, item.getComments());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        item.setId(rs.getInt(1)); // Set the generated ID back to the item
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting lab item: " + e.getMessage());
        }
    }

    @Override
    public void deleteItem(int id) {
        String sql = "DELETE FROM clinical_lab_items WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(getDbUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting lab item: " + e.getMessage());
        }
    }

    private ClinicalLabItem mapResultSetToItem(ResultSet rs) throws SQLException {
        return new ClinicalLabItem(
            rs.getInt("id"),
            rs.getString("category"),
            rs.getString("test_name"),
            rs.getString("unit"),
            getObjectOrNull(rs, "male_range_low"),
            getObjectOrNull(rs, "male_range_high"),
            getObjectOrNull(rs, "female_range_low"),
            getObjectOrNull(rs, "female_range_high"),
            rs.getString("male_reference_range"),
            rs.getString("female_reference_range"),
            rs.getString("codes"),
            rs.getString("comments")
        );
    }

    private Double getObjectOrNull(ResultSet rs, String column) throws SQLException {
        double val = rs.getDouble(column);
        return rs.wasNull() ? null : val;
    }
}
