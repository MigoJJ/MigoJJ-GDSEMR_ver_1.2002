package com.emr.gds.core.db;

import com.emr.gds.core.auth.SqliteCredentialRepository;
import com.emr.gds.features.history.adapter.out.persistence.JdbcHistoryRepository;
import com.emr.gds.features.history.domain.ConditionCategory;
import com.emr.gds.repository.sqlite.SqliteAbbreviationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteRepositoryIsolationTest {

    @TempDir
    Path tempDir;

    @Test
    void abbreviationRepositoryUsesInjectedDatabaseRoot() throws Exception {
        AppDatabaseManager databaseManager = new AppDatabaseManager(tempDir);
        SqliteAbbreviationRepository repository = new SqliteAbbreviationRepository(databaseManager);

        repository.insert("synthetic", "synthetic expansion");

        assertEquals(Map.of("synthetic", "synthetic expansion"), repository.findAll());
        assertTrue(tempDir.resolve("abbreviations.db").toFile().isFile());
        assertFalse(Path.of("app", "db", "abbreviations.db").toAbsolutePath().normalize()
                .equals(tempDir.resolve("abbreviations.db").toAbsolutePath().normalize()));
        databaseManager.closeAll();
    }

    @Test
    void historyRepositoryUsesInjectedDatabaseFile() {
        Path database = tempDir.resolve("history.db");
        JdbcHistoryRepository repository = new JdbcHistoryRepository(database);

        repository.addCondition(ConditionCategory.ENDOCRINE, "Synthetic condition");

        assertEquals(java.util.List.of("Synthetic condition"),
                repository.getConditionsByCategory(ConditionCategory.ENDOCRINE));
        assertTrue(database.toFile().isFile());
    }

    @Test
    void credentialRepositoryUsesInjectedDatabaseFile() {
        Path database = tempDir.resolve("auth.db");
        SqliteCredentialRepository repository = new SqliteCredentialRepository(database);

        repository.setPassword("synthetic-password".toCharArray());

        assertTrue(repository.hasCredential());
        assertTrue(repository.verifyPassword("synthetic-password".toCharArray()));
        assertFalse(repository.verifyPassword("wrong-password".toCharArray()));
        assertTrue(database.toFile().isFile());
    }
}
