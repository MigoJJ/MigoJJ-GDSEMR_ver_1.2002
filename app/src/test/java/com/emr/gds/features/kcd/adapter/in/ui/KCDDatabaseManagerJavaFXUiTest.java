package com.emr.gds.features.kcd.adapter.in.ui;

import com.emr.gds.features.kcd.domain.KCDRecord;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the real, code-built (no FXML) kcd scene through TestFX rather than
 * a throwaway harness Application, replacing the manual verification step
 * described in docs/architecture.md § "Verifying UI changes" for this
 * feature. Uses only a synthetic database in {@link TempDir}; the bundled
 * KCD seed database is never opened by this test.
 *
 * Uses {@code FxRobot.interact(Runnable)} rather than
 * {@code clickOn}/{@code write}/{@code type} for the same reason as
 * ClinicalLabControllerUiTest / AllergyControllerUiTest: OS-level synthetic
 * input via the xdg-desktop-portal is unreliable in this dev environment.
 */
@ExtendWith(ApplicationExtension.class)
class KCDDatabaseManagerJavaFXUiTest {

    @TempDir
    Path tempDir;

    private TableView<KCDRecord> table;
    private TextField searchField;

    @Start
    private void start(Stage stage) throws Exception {
        Path database = tempDir.resolve("kcd-test.db");
        createSyntheticDatabase(database);
        KCDDatabaseManagerJavaFX manager = new KCDDatabaseManagerJavaFX(
                new com.emr.gds.features.kcd.adapter.out.persistence.JdbcKcdRepository(database));
        manager.start(stage);

        table = manager.getTable();
        searchField = manager.getSearchField();
    }

    @Test
    void loadsRealDataOnStartup() throws InterruptedException {
        // loadInitialData() runs the DB read on a background Task; give it a moment to finish.
        waitUntil(() -> !table.getItems().isEmpty(), 3000);
        assertFalse(table.getItems().isEmpty(), "Table should load real rows from the bundled KCD database");
        assertTrue(table.getItems().size() >= 3, "Expected the synthetic KCD dataset to load, got " + table.getItems().size());
    }

    @Test
    void searchFiltersToMatchingRowsOnly(FxRobot robot) throws InterruptedException {
        waitUntil(() -> !table.getItems().isEmpty(), 3000);
        int totalRows = table.getItems().size();

        robot.interact(() -> searchField.setText("콜레라"));
        waitUntil(() -> table.getItems().size() < totalRows, 2000);

        assertFalse(table.getItems().isEmpty(), "Expected at least one '콜레라' (cholera) match in the real dataset");
        assertTrue(table.getItems().size() < totalRows, "Search should narrow the result set");

        robot.interact(() -> searchField.setText(""));
        waitUntil(() -> table.getItems().size() == totalRows, 2000);

        assertEquals(totalRows, table.getItems().size(), "Clearing the search should restore the full list");
    }

    private void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
    }

    private void createSyntheticDatabase(Path database) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE kcd_codes (classification TEXT, disease_code TEXT PRIMARY KEY, check_field TEXT, korean_name TEXT, english_name TEXT, note TEXT)");
            String sql = "INSERT INTO kcd_codes(classification, disease_code, check_field, korean_name, english_name, note) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insert = connection.prepareStatement(sql)) {
                insertRow(insert, "A", "A00.0", "Y", "콜레라", "Cholera", "synthetic");
                insertRow(insert, "A", "A00.1", "N", "콜레라 기타", "Other cholera", "synthetic");
                insertRow(insert, "B", "B00.0", "N", "합성 질환", "Synthetic disease", "synthetic");
            }
        }
    }

    private void insertRow(PreparedStatement insert, String classification, String code,
                           String checkField, String koreanName, String englishName, String note)
            throws Exception {
        insert.setString(1, classification);
        insert.setString(2, code);
        insert.setString(3, checkField);
        insert.setString(4, koreanName);
        insert.setString(5, englishName);
        insert.setString(6, note);
        insert.executeUpdate();
    }
}
