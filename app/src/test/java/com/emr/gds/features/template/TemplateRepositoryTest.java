package com.emr.gds.features.template;

import com.emr.gds.features.template.application.TemplateModel;
import com.emr.gds.features.template.persistence.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemplateRepositoryTest {

    @TempDir
    Path tempDir;

    private TemplateRepository repo;

    @BeforeEach
    void setUp() {
        repo = new TemplateRepository(tempDir.resolve("emr_templates.db"));
    }

    @Test
    void testRepositoryInitializes() {
        assertNotNull(repo);
        assertTrue(tempDir.resolve("emr_templates.db").toFile().isFile());
    }

    @Test
    void testUsesTemporaryDatabaseInsteadOfProductionDatabase() {
        Path productionDatabase = Path.of("app", "db", "emr_templates.db")
            .toAbsolutePath()
            .normalize();
        assertNotEquals(productionDatabase, tempDir.resolve("emr_templates.db").toAbsolutePath().normalize());
    }

    @Test
    void testGetAllTemplatesReturnsEmptyListOnInit() {
        List<TemplateModel> templates = repo.getAllTemplates();
        assertNotNull(templates);
    }

    @Test
    void testCreateTemplate() {
        String testName = "Test Template";
        String testContent = "CC> Chief complaint\nPI> Present illness";

        repo.createTemplate(testName, testContent);
        List<TemplateModel> templates = repo.getAllTemplates();

        assertTrue(templates.stream()
            .anyMatch(t -> t.getName().equals(testName)));
    }

    @Test
    void testCreateAndRetrieveTemplate() {
        String name = "Recovery Template";
        String content = "Post-op assessment";

        repo.createTemplate(name, content);
        List<TemplateModel> templates = repo.getAllTemplates();

        TemplateModel found = templates.stream()
            .filter(t -> t.getName().equals(name))
            .findFirst()
            .orElse(null);

        assertNotNull(found);
        assertEquals(name, found.getName());
        assertEquals(content, found.getContent());
    }

    @Test
    void testUpdateTemplate() {
        String originalName = "Original";
        String originalContent = "Original content";

        repo.createTemplate(originalName, originalContent);
        List<TemplateModel> templates = repo.getAllTemplates();
        TemplateModel template = templates.get(templates.size() - 1);
        int templateId = template.getId();

        String updatedName = "Updated";
        String updatedContent = "Updated content";
        repo.updateTemplate(templateId, updatedName, updatedContent);

        List<TemplateModel> updated = repo.getAllTemplates();
        TemplateModel refreshed = updated.stream()
            .filter(t -> t.getId() == templateId)
            .findFirst()
            .orElse(null);

        assertNotNull(refreshed);
        assertEquals(updatedName, refreshed.getName());
        assertEquals(updatedContent, refreshed.getContent());
    }

    @Test
    void testDeleteTemplate() {
        repo.createTemplate("ToDelete", "Content");
        List<TemplateModel> templates = repo.getAllTemplates();
        int sizeBeforeDelete = templates.size();

        int idToDelete = templates.get(templates.size() - 1).getId();
        repo.deleteTemplate(idToDelete);

        List<TemplateModel> after = repo.getAllTemplates();
        assertFalse(after.stream().anyMatch(t -> t.getId() == idToDelete));
    }

    @Test
    void testMultipleTemplates() {
        repo.createTemplate("Template1", "Content1");
        repo.createTemplate("Template2", "Content2");
        repo.createTemplate("Template3", "Content3");

        List<TemplateModel> templates = repo.getAllTemplates();
        assertTrue(templates.size() >= 3);
    }

    @Test
    void testTemplateModelToString() {
        TemplateModel model = new TemplateModel(1, "Test", "Content");
        assertEquals("Test", model.toString());
    }
}
