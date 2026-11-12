package com.emr.gds.features.clinicalLab.adapter.out.persistence;

import com.emr.gds.features.clinicalLab.domain.ClinicalLabItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcClinicalLabRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void insertUpdateSearchDeleteRoundTrip() {
        JdbcClinicalLabRepository repo = new JdbcClinicalLabRepository(tempDir.resolve("clinical_lab.db"));

        ClinicalLabItem item = new ClinicalLabItem(0, "Chemistry", "Test Glucose", "mg/dL",
                70.0, 100.0, 70.0, 100.0, "70-100", "70-100", "GLU", "fasting");
        repo.insertItem(item);
        assertTrue(item.getId() > 0, "Insert should assign a generated id");

        List<ClinicalLabItem> found = repo.searchItems("glucose");
        assertEquals(1, found.size(), "Search should find the inserted item by substring");
        assertEquals("Test Glucose", found.get(0).getTestName());

        item.setComments("updated");
        repo.updateItem(item);
        List<ClinicalLabItem> all = repo.getAllItems();
        assertTrue(all.stream().anyMatch(i -> i.getId() == item.getId() && "updated".equals(i.getComments())),
                "Update should be visible via getAllItems");

        repo.deleteItem(item.getId());
        assertTrue(repo.getAllItems().stream().noneMatch(i -> i.getId() == item.getId()),
                "Deleted item should no longer be present");
    }
}
