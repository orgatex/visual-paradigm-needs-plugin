package com.orgatex.vp.sphinx.extractor;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.shape.IUseCaseUIModel;
import com.vp.plugin.diagram.shape.IActorUIModel;
import com.vp.plugin.model.IModelElement;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UseCaseDiagramExtractor.
 */
public class UseCaseDiagramExtractorTest {

    @Test
    public void testExtractDiagramWithNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            UseCaseDiagramExtractor.extractDiagram(null);
        });
    }

    @Test
    public void testExtractEmptyDiagram() {
        // Mock diagram
        IDiagramUIModel diagram = mock(IDiagramUIModel.class);
        when(diagram.getName()).thenReturn("Test Diagram");
        when(diagram.toDiagramElementArray()).thenReturn(new IDiagramElement[0]);

        // Extract
        NeedsFile result = UseCaseDiagramExtractor.extractDiagram(diagram);

        // Verify
        assertNotNull(result);
        assertEquals("Test Diagram", result.getProject());
        assertEquals("1.0", result.getCurrentVersion());
        assertNotNull(result.getVersions());
        assertTrue(result.getVersions().containsKey("1.0"));

        NeedsFile.VersionData versionData = result.getVersions().get("1.0");
        assertNotNull(versionData);
        assertEquals(0, versionData.getNeedsAmount());
        assertTrue(versionData.getNeeds().isEmpty());
    }

    @Test
    public void testExtractDiagramWithUseCases() {
        // Mock diagram
        IDiagramUIModel diagram = mock(IDiagramUIModel.class);
        when(diagram.getName()).thenReturn("Use Case Diagram");

        // Mock use case elements
        IUseCaseUIModel useCase1 = mock(IUseCaseUIModel.class);
        IModelElement useCaseModel1 = mock(IModelElement.class);
        when(useCaseModel1.getId()).thenReturn("UC001");
        when(useCaseModel1.getName()).thenReturn("Login");
        when(useCaseModel1.getDescription()).thenReturn("User authentication process");
        when(useCase1.getModelElement()).thenReturn(useCaseModel1);

        IUseCaseUIModel useCase2 = mock(IUseCaseUIModel.class);
        IModelElement useCaseModel2 = mock(IModelElement.class);
        when(useCaseModel2.getId()).thenReturn("UC002");
        when(useCaseModel2.getName()).thenReturn("View Profile");
        when(useCaseModel2.getDescription()).thenReturn("Display user profile information");
        when(useCase2.getModelElement()).thenReturn(useCaseModel2);

        // Mock actor
        IActorUIModel actor = mock(IActorUIModel.class);
        IModelElement actorModel = mock(IModelElement.class);
        when(actorModel.getId()).thenReturn("ACTOR001");
        when(actorModel.getName()).thenReturn("User");
        when(actor.getModelElement()).thenReturn(actorModel);

        IDiagramElement[] elements = {useCase1, useCase2, actor};
        when(diagram.toDiagramElementArray()).thenReturn(elements);

        // Extract
        NeedsFile result = UseCaseDiagramExtractor.extractDiagram(diagram);

        // Verify
        assertNotNull(result);
        assertEquals("Use Case Diagram", result.getProject());

        NeedsFile.VersionData versionData = result.getVersions().get("1.0");
        assertNotNull(versionData);
        assertEquals(2, versionData.getNeedsAmount());
        assertEquals(2, versionData.getNeeds().size());

        // Verify use case 1
        boolean foundLogin = false;
        boolean foundViewProfile = false;

        for (NeedsFile.Need need : versionData.getNeeds().values()) {
            if ("Login".equals(need.getTitle())) {
                foundLogin = true;
                assertEquals("User authentication process", need.getContent());
                assertEquals("req", need.getType());
                assertEquals("open", need.getStatus());
                assertEquals("UC001", need.getSourceId());
                assertEquals("UseCase", need.getElementType());
                assertTrue(need.getTags().contains("usecase"));
                assertTrue(need.getTags().contains("functional"));
            } else if ("View Profile".equals(need.getTitle())) {
                foundViewProfile = true;
                assertEquals("Display user profile information", need.getContent());
                assertEquals("req", need.getType());
                assertEquals("open", need.getStatus());
                assertEquals("UC002", need.getSourceId());
                assertEquals("UseCase", need.getElementType());
                assertTrue(need.getTags().contains("usecase"));
                assertTrue(need.getTags().contains("functional"));
            }
        }

        assertTrue(foundLogin, "Login use case should be extracted");
        assertTrue(foundViewProfile, "View Profile use case should be extracted");
    }

    @Test
    public void testSanitizeId() {
        // Test via package-private access using reflection
        try {
            java.lang.reflect.Method sanitizeIdMethod =
                UseCaseDiagramExtractor.class.getDeclaredMethod("sanitizeId", String.class);
            sanitizeIdMethod.setAccessible(true);

            assertEquals("UC_LOGIN", sanitizeIdMethod.invoke(null, "UC_Login"));
            assertEquals("UC_USER_MANAGEMENT", sanitizeIdMethod.invoke(null, "UC_User Management"));
            assertEquals("UNKNOWN", sanitizeIdMethod.invoke(null, (String) null));
            assertEquals("UC_TEST", sanitizeIdMethod.invoke(null, "UC_Test!!!"));
        } catch (Exception e) {
            fail("Reflection test failed: " + e.getMessage());
        }
    }

    @Test
    public void testSanitizeTag() {
        // Test via package-private access using reflection
        try {
            java.lang.reflect.Method sanitizeTagMethod =
                UseCaseDiagramExtractor.class.getDeclaredMethod("sanitizeTag", String.class);
            sanitizeTagMethod.setAccessible(true);

            assertEquals("user", sanitizeTagMethod.invoke(null, "User"));
            assertEquals("admin_user", sanitizeTagMethod.invoke(null, "Admin User"));
            assertEquals("unknown", sanitizeTagMethod.invoke(null, (String) null));
            assertEquals("test_user", sanitizeTagMethod.invoke(null, "Test User!!!"));
        } catch (Exception e) {
            fail("Reflection test failed: " + e.getMessage());
        }
    }
}
