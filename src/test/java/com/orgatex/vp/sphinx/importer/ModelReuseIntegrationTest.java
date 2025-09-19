package com.orgatex.vp.sphinx.importer;

import static org.junit.jupiter.api.Assertions.*;

import com.orgatex.vp.sphinx.model.NeedsFile;
import org.junit.jupiter.api.Test;

/** Integration tests for model reuse functionality. */
public class ModelReuseIntegrationTest {

  @Test
  public void testNeedWithVpModelId() {
    // Test that Need objects can store and retrieve VP model IDs
    NeedsFile.Need need = new NeedsFile.Need("REQ001", "Test Use Case", "uc");

    // Set VP model ID
    need.setVpModelId("vp_model_123456");

    // Verify it can be retrieved
    assertEquals("vp_model_123456", need.getVpModelId());
  }

  @Test
  public void testNeedWithNullVpModelId() {
    // Test behavior with null VP model ID
    NeedsFile.Need need = new NeedsFile.Need("REQ001", "Test Use Case", "uc");

    // Should be null by default
    assertNull(need.getVpModelId());

    // Should handle null assignment
    need.setVpModelId(null);
    assertNull(need.getVpModelId());
  }

  @Test
  public void testNeedWithEmptyVpModelId() {
    // Test behavior with empty VP model ID
    NeedsFile.Need need = new NeedsFile.Need("REQ001", "Test Use Case", "uc");

    // Should handle empty string
    need.setVpModelId("");
    assertEquals("", need.getVpModelId());

    // Should handle whitespace
    need.setVpModelId("   ");
    assertEquals("   ", need.getVpModelId());
  }

  @Test
  public void testModelReuseScenario() {
    // Test the scenario where we have needs with VP model IDs
    NeedsFile needsFile = createNeedsFileWithVpModelIds();

    // Verify the needs have VP model IDs
    NeedsFile.VersionData versionData = needsFile.getVersions().get("1.0");
    assertNotNull(versionData);

    NeedsFile.Need useCase = versionData.getNeeds().get("UC001");
    assertNotNull(useCase);
    assertEquals("vp_model_uc_001", useCase.getVpModelId());

    NeedsFile.Need actor = versionData.getNeeds().get("ACTOR001");
    assertNotNull(actor);
    assertEquals("vp_model_actor_001", actor.getVpModelId());
  }

  /** Create a sample needs file with VP model IDs for testing. */
  private NeedsFile createNeedsFileWithVpModelIds() {
    NeedsFile needsFile = new NeedsFile();
    needsFile.setProject("Test Project");
    needsFile.setCurrentVersion("1.0");
    needsFile.setCreated("2024-01-01T00:00:00");

    NeedsFile.VersionData versionData = new NeedsFile.VersionData();
    versionData.setCreated("2024-01-01T00:00:00");
    versionData.setCreator(new NeedsFile.Creator());

    // Create use case with VP model ID
    NeedsFile.Need useCase = new NeedsFile.Need("UC001", "Login User", "uc");
    useCase.setContent("User can login to the system");
    useCase.setStatus("identify");
    useCase.setElementType("UseCase");
    useCase.setVpModelId("vp_model_uc_001");
    versionData.addNeed(useCase);

    // Create actor with VP model ID
    NeedsFile.Need actor = new NeedsFile.Need("ACTOR001", "User", "actor");
    actor.setContent("System user");
    actor.setStatus("identify");
    actor.setElementType("Actor");
    actor.setVpModelId("vp_model_actor_001");
    versionData.addNeed(actor);

    needsFile.addVersion("1.0", versionData);
    return needsFile;
  }
}
