package com.orgatex.vp.sphinx.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orgatex.vp.sphinx.generator.JsonExporter;
import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.shape.IUseCaseUIModel;
import com.vp.plugin.diagram.shape.IActorUIModel;
import com.vp.plugin.model.IModelElement;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests to verify that the generated JSON matches the expected schema format.
 */
public class SchemaValidationTest {

    @Test
    public void testGeneratedJsonStructure() throws IOException {
        // Mock diagram with multiple use cases
        IDiagramUIModel diagram = mock(IDiagramUIModel.class);
        when(diagram.getName()).thenReturn("Complex Use Case Diagram");

        // Mock multiple use cases
        IUseCaseUIModel[] useCases = new IUseCaseUIModel[5];
        IModelElement[] useCaseModels = new IModelElement[5];

        for (int i = 0; i < 5; i++) {
            useCases[i] = mock(IUseCaseUIModel.class);
            useCaseModels[i] = mock(IModelElement.class);
            when(useCaseModels[i].getId()).thenReturn("UC" + String.format("%03d", i + 1));
            when(useCaseModels[i].getName()).thenReturn("Use Case " + (i + 1));
            when(useCaseModels[i].getDescription()).thenReturn("Description for use case " + (i + 1));
            when(useCases[i].getModelElement()).thenReturn(useCaseModels[i]);
        }

        // Mock actors
        IActorUIModel[] actors = new IActorUIModel[3];
        IModelElement[] actorModels = new IModelElement[3];
        String[] actorNames = {"User", "Admin", "System"};

        for (int i = 0; i < 3; i++) {
            actors[i] = mock(IActorUIModel.class);
            actorModels[i] = mock(IModelElement.class);
            when(actorModels[i].getId()).thenReturn("ACTOR" + String.format("%03d", i + 1));
            when(actorModels[i].getName()).thenReturn(actorNames[i]);
            when(actors[i].getModelElement()).thenReturn(actorModels[i]);
        }

        // Combine all elements
        IDiagramElement[] allElements = new IDiagramElement[8];
        System.arraycopy(useCases, 0, allElements, 0, 5);
        System.arraycopy(actors, 0, allElements, 5, 3);

        when(diagram.toDiagramElementArray()).thenReturn(allElements);

        // Extract
        NeedsFile result = UseCaseDiagramExtractor.extractDiagram(diagram);

        // Verify basic structure
        assertNotNull(result);
        assertNotNull(result.getCreated());
        assertEquals("Complex Use Case Diagram", result.getProject());
        assertEquals("1.0", result.getCurrentVersion());
        assertNotNull(result.getVersions());
        assertTrue(result.getVersions().containsKey("1.0"));

        NeedsFile.VersionData versionData = result.getVersions().get("1.0");
        assertNotNull(versionData);
        assertNotNull(versionData.getCreated());
        assertNotNull(versionData.getCreator());
        assertEquals("Visual Paradigm Sphinx-Needs Plugin", versionData.getCreator().getName());
        assertEquals("1.0.0", versionData.getCreator().getVersion());
        assertEquals(5, versionData.getNeedsAmount());
        assertEquals(5, versionData.getNeeds().size());

        // Verify all use cases were extracted
        for (int i = 1; i <= 5; i++) {
            boolean found = false;
            for (NeedsFile.Need need : versionData.getNeeds().values()) {
                if (("Use Case " + i).equals(need.getTitle())) {
                    found = true;
                    assertEquals("Description for use case " + i, need.getContent());
                    assertEquals("req", need.getType());
                    assertEquals("open", need.getStatus());
                    assertEquals("UC" + String.format("%03d", i), need.getSourceId());
                    assertEquals("UseCase", need.getElementType());
                    assertNotNull(need.getTags());
                    assertTrue(need.getTags().contains("usecase"));
                    assertTrue(need.getTags().contains("functional"));
                    assertEquals("", need.getLinks()); // No relationships in this test
                    break;
                }
            }
            assertTrue(found, "Use Case " + i + " should be extracted");
        }

        // Export to JSON and verify structure
        File tempFile = File.createTempFile("test_needs", ".json");
        tempFile.deleteOnExit();

        JsonExporter.exportToFile(result, tempFile);

        // Read back and verify JSON structure
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonData = mapper.readValue(tempFile, Map.class);

        assertTrue(jsonData.containsKey("created"));
        assertTrue(jsonData.containsKey("current_version"));
        assertTrue(jsonData.containsKey("project"));
        assertTrue(jsonData.containsKey("versions"));

        @SuppressWarnings("unchecked")
        Map<String, Object> versions = (Map<String, Object>) jsonData.get("versions");
        assertTrue(versions.containsKey("1.0"));

        @SuppressWarnings("unchecked")
        Map<String, Object> version1 = (Map<String, Object>) versions.get("1.0");
        assertTrue(version1.containsKey("created"));
        assertTrue(version1.containsKey("creator"));
        assertTrue(version1.containsKey("needs"));
        assertTrue(version1.containsKey("needs_amount"));

        assertEquals(5, version1.get("needs_amount"));

        @SuppressWarnings("unchecked")
        Map<String, Object> needs = (Map<String, Object>) version1.get("needs");
        assertEquals(5, needs.size());

        // Verify each need has required fields
        for (Object needObj : needs.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> need = (Map<String, Object>) needObj;
            assertTrue(need.containsKey("id"));
            assertTrue(need.containsKey("title"));
            assertTrue(need.containsKey("content"));
            assertTrue(need.containsKey("type"));
            assertTrue(need.containsKey("status"));
            assertTrue(need.containsKey("tags"));
            assertTrue(need.containsKey("links"));
            assertTrue(need.containsKey("source_id"));
            assertTrue(need.containsKey("element_type"));
        }
    }
}
