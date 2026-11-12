package com.emr.gds.core.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized manager for application SQLite connections.
 * Currently manages the abbreviations database connection and provides a hook
 * to close shared resources on shutdown.
 */
public class AppDatabaseManager {

    private static final AppDatabaseManager INSTANCE = new AppDatabaseManager();

    private final Path databaseDirectory;

    private Connection abbreviationConnection;
    private Connection historyConnection;
    private Connection referenceConnection;
    private Connection authConnection;

    private AppDatabaseManager() {
        this(null);
    }

    /** Creates a manager rooted at an explicit directory, primarily for isolated tests. */
    public AppDatabaseManager(Path databaseDirectory) {
        this.databaseDirectory = databaseDirectory == null
                ? null
                : databaseDirectory.toAbsolutePath().normalize();
    }

    public static AppDatabaseManager getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a shared connection to the abbreviations database.
     */
    public synchronized Connection getAbbreviationConnection() throws SQLException {
        if (abbreviationConnection == null || abbreviationConnection.isClosed()) {
            abbreviationConnection = openConnection("abbreviations.db");
        }
        return abbreviationConnection;
    }

    /**
     * Returns a shared connection to the history database.
     */
    public synchronized Connection getHistoryConnection() throws SQLException {
        if (historyConnection == null || historyConnection.isClosed()) {
            historyConnection = openConnection("history.db");
        }
        return historyConnection;
    }

    /**
     * Returns a shared connection to the references database.
     */
    public synchronized Connection getReferenceConnection() throws SQLException {
        if (referenceConnection == null || referenceConnection.isClosed()) {
            referenceConnection = openConnection("references.db");
        }
        return referenceConnection;
    }

    /**
     * Returns a shared connection to the local auth database (the single
     * application password's hash).
     */
    public synchronized Connection getAuthConnection() throws SQLException {
        if (authConnection == null || authConnection.isClosed()) {
            authConnection = openConnection("auth.db");
        }
        return authConnection;
    }

    private Connection openConnection(String dbFileName) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found", e);
        }
        Path dbPath = databaseDirectory == null
                ? DbPaths.resolveDbPath(dbFileName)
                : databaseDirectory.resolve(dbFileName);
        try {
            Files.createDirectories(dbPath.getParent());
        } catch (Exception e) {
            throw new SQLException("Failed to create database directory: " + dbPath.getParent(), e);
        }
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        return DriverManager.getConnection(url);
    }

    /**
     * Closes all managed connections. Intended to be called on application shutdown.
     */
    public synchronized void closeAll() {
        closeQuietly(abbreviationConnection);
        abbreviationConnection = null;
        closeQuietly(historyConnection);
        historyConnection = null;
        closeQuietly(referenceConnection);
        referenceConnection = null;
        closeQuietly(authConnection);
        authConnection = null;
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // best-effort close
            }
        }
    }
}
