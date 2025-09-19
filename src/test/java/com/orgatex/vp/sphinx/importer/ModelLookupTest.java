package com.orgatex.vp.sphinx.importer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.model.IActor;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IProject;
import com.vp.plugin.model.IUseCase;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Unit tests for ModelLookup functionality. */
public class ModelLookupTest {

  private ModelLookup modelLookup;
  private ProjectManager mockProjectManager;
  private IProject mockProject;

  @BeforeEach
  public void setUp() {
    mockProjectManager = mock(ProjectManager.class);
    mockProject = mock(IProject.class);
    when(mockProjectManager.getProject()).thenReturn(mockProject);
  }

  @Test
  public void testFindModelById_ExistingModel() throws Exception {
    // Create mock models
    IUseCase mockUseCase = mock(IUseCase.class);
    when(mockUseCase.getId()).thenReturn("UC001");

    IActor mockActor = mock(IActor.class);
    when(mockActor.getId()).thenReturn("ACTOR001");

    // Mock iterator
    Iterator<IModelElement> mockIterator =
        Arrays.asList((IModelElement) mockUseCase, (IModelElement) mockActor).iterator();
    when(mockProject.allLevelModelElementIterator()).thenReturn(mockIterator);

    // Test with mocked ApplicationManager
    try (MockedStatic<ApplicationManager> appManagerMock = mockStatic(ApplicationManager.class)) {
      ApplicationManager mockAppManager = mock(ApplicationManager.class);
      appManagerMock.when(ApplicationManager::instance).thenReturn(mockAppManager);
      when(mockAppManager.getProjectManager()).thenReturn(mockProjectManager);

      modelLookup = new ModelLookup();

      // Test finding existing model
      IModelElement result = modelLookup.findModelById("UC001");
      assertNotNull(result);
      assertEquals("UC001", result.getId());
      assertTrue(result instanceof IUseCase);
    }
  }

  @Test
  public void testFindModelById_NonExistentModel() throws Exception {
    // Mock empty iterator
    Iterator<IModelElement> mockIterator = Arrays.<IModelElement>asList().iterator();
    when(mockProject.allLevelModelElementIterator()).thenReturn(mockIterator);

    try (MockedStatic<ApplicationManager> appManagerMock = mockStatic(ApplicationManager.class)) {
      ApplicationManager mockAppManager = mock(ApplicationManager.class);
      appManagerMock.when(ApplicationManager::instance).thenReturn(mockAppManager);
      when(mockAppManager.getProjectManager()).thenReturn(mockProjectManager);

      modelLookup = new ModelLookup();

      // Test finding non-existent model
      IModelElement result = modelLookup.findModelById("NONEXISTENT");
      assertNull(result);
    }
  }

  @Test
  public void testFindModelById_WithTypeChecking() throws Exception {
    // Create mock models
    IUseCase mockUseCase = mock(IUseCase.class);
    when(mockUseCase.getId()).thenReturn("UC001");

    IActor mockActor = mock(IActor.class);
    when(mockActor.getId()).thenReturn("ACTOR001");

    Iterator<IModelElement> mockIterator =
        Arrays.asList((IModelElement) mockUseCase, (IModelElement) mockActor).iterator();
    when(mockProject.allLevelModelElementIterator()).thenReturn(mockIterator);

    try (MockedStatic<ApplicationManager> appManagerMock = mockStatic(ApplicationManager.class)) {
      ApplicationManager mockAppManager = mock(ApplicationManager.class);
      appManagerMock.when(ApplicationManager::instance).thenReturn(mockAppManager);
      when(mockAppManager.getProjectManager()).thenReturn(mockProjectManager);

      modelLookup = new ModelLookup();

      // Test finding with correct type
      IUseCase useCaseResult = modelLookup.findModelById("UC001", IUseCase.class);
      assertNotNull(useCaseResult);
      assertEquals("UC001", useCaseResult.getId());

      // Test finding with wrong type
      IActor actorResult = modelLookup.findModelById("UC001", IActor.class);
      assertNull(actorResult); // Should return null because UC001 is not an Actor
    }
  }

  @Test
  public void testModelExists() throws Exception {
    IUseCase mockUseCase = mock(IUseCase.class);
    when(mockUseCase.getId()).thenReturn("UC001");

    Iterator<IModelElement> mockIterator = Arrays.asList((IModelElement) mockUseCase).iterator();
    when(mockProject.allLevelModelElementIterator()).thenReturn(mockIterator);

    try (MockedStatic<ApplicationManager> appManagerMock = mockStatic(ApplicationManager.class)) {
      ApplicationManager mockAppManager = mock(ApplicationManager.class);
      appManagerMock.when(ApplicationManager::instance).thenReturn(mockAppManager);
      when(mockAppManager.getProjectManager()).thenReturn(mockProjectManager);

      modelLookup = new ModelLookup();

      // Test existing model
      assertTrue(modelLookup.modelExists("UC001"));

      // Test non-existing model
      assertFalse(modelLookup.modelExists("NONEXISTENT"));
    }
  }

  @Test
  public void testFindModelById_NullOrEmptyId() throws Exception {
    try (MockedStatic<ApplicationManager> appManagerMock = mockStatic(ApplicationManager.class)) {
      ApplicationManager mockAppManager = mock(ApplicationManager.class);
      appManagerMock.when(ApplicationManager::instance).thenReturn(mockAppManager);
      when(mockAppManager.getProjectManager()).thenReturn(mockProjectManager);

      modelLookup = new ModelLookup();

      // Test null ID
      assertNull(modelLookup.findModelById(null));

      // Test empty ID
      assertNull(modelLookup.findModelById(""));
      assertNull(modelLookup.findModelById("   "));
    }
  }
}
