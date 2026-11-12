package com.emr.gds.features.kcd.adapter.out.persistence;

import com.emr.gds.features.kcd.domain.KCDRecord;
import com.emr.gds.features.kcd.domain.KcdRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Note: {@code kcd_database.db} is a bundled classpath resource
 * (packaged under {@code src/main/resources/database/}), not one of the
 * {@code app/db/*.db} files, so this deliberately does not use
 * {@code com.emr.gds.core.db.DbPaths} — see docs/architecture.md §
 * Persistence for why. The relative path below is resolved against the
 * working directory the app is launched from, same as before this class
 * existed (previously this constant lived on the UI class,
 * {@code KCDDatabaseManagerJavaFX}).
 */
public class JdbcKcdRepository implements KcdRepository {

    private static final String DB_PATH = "src/main/resources/database/kcd_database.db";
    private final Path dbPath;

    public JdbcKcdRepository() {
        this(Paths.get(DB_PATH));
    }

    /** Creates a repository backed by an explicit SQLite file, primarily for isolated tests. */
    public JdbcKcdRepository(Path dbPath) {
        this.dbPath = dbPath.toAbsolutePath().normalize();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    @Override
    public List<KCDRecord> getAllRecords() throws SQLException {
        List<KCDRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM kcd_codes ORDER BY disease_code";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.add(new KCDRecord(
                        rs.getString("classification"),
                        rs.getString("disease_code"),
                        rs.getString("check_field"),
                        rs.getString("korean_name"),
                        rs.getString("english_name"),
                        rs.getString("note")
                ));
            }
        }
        return records;
    }

    @Override
    public void addRecord(KCDRecord record) throws SQLException {
        String sql = "INSERT INTO kcd_codes(classification, disease_code, check_field, korean_name, english_name, note) VALUES(?,?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.getClassification());
            pstmt.setString(2, record.getDiseaseCode());
            pstmt.setString(3, record.getCheckField());
            pstmt.setString(4, record.getKoreanName());
            pstmt.setString(5, record.getEnglishName());
            pstmt.setString(6, record.getNote());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateRecord(String originalDiseaseCode, KCDRecord record) throws SQLException {
        String sql = "UPDATE kcd_codes SET classification=?, disease_code=?, check_field=?, korean_name=?, english_name=?, note=? WHERE disease_code=?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.getClassification());
            pstmt.setString(2, record.getDiseaseCode());
            pstmt.setString(3, record.getCheckField());
            pstmt.setString(4, record.getKoreanName());
            pstmt.setString(5, record.getEnglishName());
            pstmt.setString(6, record.getNote());
            pstmt.setString(7, originalDiseaseCode);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteRecord(String diseaseCode) throws SQLException {
        String sql = "DELETE FROM kcd_codes WHERE disease_code = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, diseaseCode);
            pstmt.executeUpdate();
        }
    }
}
