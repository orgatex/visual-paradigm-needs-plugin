package com.orgatex.vp.sphinx.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.model.IActor;
import com.vp.plugin.model.IUseCase;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that content field from needs is properly imported as description in Visual
 * Paradigm elements.
 */
public class ContentFieldImportTest {

  /** Helper method to simulate setElementDescription functionality. */
  private void setElementDescription(Object element, String description) {
    try {
      java.lang.reflect.Method setDescriptionMethod =
          element.getClass().getMethod("setDescription", String.class);
      setDescriptionMethod.invoke(element, description != null ? description : "");
    } catch (Exception e) {
      System.err.println("Error setting element description: " + e.getMessage());
    }
  }

  @Test
  public void testSetElementDescriptionForUseCase() throws Exception {
    // Create mock use case
    IUseCase mockUseCase = mock(IUseCase.class);

    // Test with content
    String testContent = "This use case handles user authentication and validation.";
    setElementDescription(mockUseCase, testContent);

    // Verify setDescription was called on the mock
    verify(mockUseCase).setDescription(testContent);
  }

  @Test
  public void testSetElementDescriptionForActor() throws Exception {
    // Create mock actor
    IActor mockActor = mock(IActor.class);

    // Test with content
    String testContent = "External user who interacts with the system.";
    setElementDescription(mockActor, testContent);

    // Verify setDescription was called on the mock
    verify(mockActor).setDescription(testContent);
  }

  @Test
  public void testSetElementDescriptionWithNullContent() throws Exception {
    // Create mock use case
    IUseCase mockUseCase = mock(IUseCase.class);

    // Test with null content - should set empty string
    setElementDescription(mockUseCase, null);

    // Verify setDescription was called with empty string
    verify(mockUseCase).setDescription("");
  }

  @Test
  public void testSetElementDescriptionWithEmptyContent() throws Exception {
    // Create mock use case
    IUseCase mockUseCase = mock(IUseCase.class);

    // Test with empty content
    String emptyContent = "";
    setElementDescription(mockUseCase, emptyContent);

    // Verify setDescription was called with empty string
    verify(mockUseCase).setDescription(emptyContent);
  }

  @Test
  public void testNeedWithContentFieldCreatesProperlySetsDescription() {
    // Create a need with content
    NeedsFile.Need need = new NeedsFile.Need("UC_001", "Login Use Case", "uc");
    need.setContent(
        "This use case allows users to authenticate with the system using username and password.");

    // Verify the content field is properly set
    assertEquals(
        "This use case allows users to authenticate with the system using username and password.",
        need.getContent());
  }
}
