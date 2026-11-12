package com.emr.gds.features.medication.adapter.out.persistence;

import com.emr.gds.features.medication.domain.MedicationGroup;
import com.emr.gds.features.medication.domain.MedicationItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MedicationDatabaseManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testPersistence() {
        // 1. Initial Load with TEST file
        Path testDb = tempDir.resolve("med_data.db");
        MedicationDatabaseManager db1 = new MedicationDatabaseManager(testDb);
        db1.createTables(); // Ensure tables exist
        
        // Seed Data since we don't load from XML anymore
        String testCat = "Test Category";
        String testGroup = "Test Group";
        db1.addCategory(testCat);
        db1.addGroup(testCat, testGroup);
        
        Map<String, List<MedicationGroup>> data1 = db1.getMedicationData();
        assertNotNull(data1, "Data should be loaded");
        assertFalse(data1.isEmpty(), "Data should not be empty after seeding");
        
        // Pick a category to add to
        String category = db1.getOrderedCategories().get(0);
        List<MedicationGroup> catGroups = data1.get(category);
        String group = catGroups.get(0).title();
        String newItemText = "Test Item 123";
        
        // 2. Add Item
        db1.addItem(category, group, new MedicationItem(newItemText));
        assertTrue(db1.hasPendingChanges(), "Should have pending changes");
        
        // 3. Commit
        db1.commitPending();
        assertFalse(db1.hasPendingChanges(), "Should not have pending changes after commit");
        
        // 4. Reload (Simulate App Restart)
        MedicationDatabaseManager db2 = new MedicationDatabaseManager(testDb);
        Map<String, List<MedicationGroup>> data2 = db2.getMedicationData();
        
        // 5. Verify
        List<MedicationGroup> groups = data2.get(category);
        assertNotNull(groups, "Groups should be loaded");
        
        boolean found = false;
        for (MedicationGroup g : groups) {
            if (g.title().equals(group)) {
                for (MedicationItem item : g.medications()) {
                    if (item.getText().equals(newItemText)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        
        assertTrue(found, "New item should be persisted and reloaded");
    }
}
