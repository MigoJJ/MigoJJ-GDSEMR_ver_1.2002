package com.emr.gds.features.template.persistence;

import com.emr.gds.features.template.application.TemplateModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TemplateRepository {
    private static final String DB_FILENAME = "emr_templates.db";

    private final Path dbPath;

    public TemplateRepository() {
        this(defaultDbPath());
    }

    /** Creates a repository backed by the supplied SQLite file. */
    public TemplateRepository(Path dbPath) {
        this.dbPath = dbPath.toAbsolutePath().normalize();
        try {
            createTableIfNotExists();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize template database", e);
        }
    }

    private Connection openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found", e);
        }
        try {
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            System.err.println("Failed to create DB directory: " + e.getMessage());
        }

        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    private static Path defaultDbPath() {
        Path p = Paths.get("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("gradlew")) && !Files.exists(p.resolve(".git"))) {
            p = p.getParent();
        }
        if (p == null) p = Paths.get("").toAbsolutePath();
        return p.resolve("app").resolve("db").resolve(DB_FILENAME);
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS templates (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, content TEXT);";
        try (Connection conn = openConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public List<TemplateModel> getAllTemplates() {
        List<TemplateModel> list = new ArrayList<>();
        String sql = "SELECT id, name, content FROM templates ORDER BY name;";
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new TemplateModel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("content")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load templates: " + e.getMessage());
        }
        return list;
    }

    public void createTemplate(String name, String content) {
        String sql = "INSERT INTO templates (name, content) VALUES (?, ?);";
        try (Connection conn = openConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, content);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create the template.", e);
        }
    }

    public void updateTemplate(int id, String name, String content) {
        String sql = "UPDATE templates SET name = ?, content = ? WHERE id = ?;";
        try (Connection conn = openConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, content);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update the template.", e);
        }
    }

    public void deleteTemplate(int id) {
        String sql = "DELETE FROM templates WHERE id = ?;";
        try (Connection conn = openConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete the template.", e);
        }
    }

    public void close() {
        // Connections are opened per operation and closed by try-with-resources.
    }
}
