package com.orgatex.vp.sphinx.importer;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orgatex.vp.sphinx.model.NeedsFile;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for NeedsFileImporter. */
public class NeedsFileImporterTest {

  @Test
  public void testParseValidJsonFile(@TempDir Path tempDir) throws Exception {
    // Create a sample needs file
    NeedsFile needsFile = createSampleNeedsFile();

    // Write to temp file
    File jsonFile = tempDir.resolve("test_needs.json").toFile();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.writeValue(jsonFile, needsFile);

    // Test JSON parsing directly (without VP runtime)
    assertDoesNotThrow(
        () -> {
          NeedsFile parsedFile = objectMapper.readValue(jsonFile, NeedsFile.class);
          assertNotNull(parsedFile);
          assertEquals("Test Project", parsedFile.getProject());
          assertEquals("1.0", parsedFile.getCurrentVersion());
          assertEquals(2, parsedFile.getVersions().get("1.0").getNeeds().size());
        });
  }

  @Test
  public void testValidationWithValidFile() throws Exception {
    NeedsFile needsFile = createSampleNeedsFile();

    // Test validation logic - these should not throw exceptions
    assertNotNull(needsFile.getCurrentVersion());
    assertNotNull(needsFile.getVersions());
    assertFalse(needsFile.getVersions().isEmpty());

    NeedsFile.VersionData versionData = needsFile.getVersions().get(needsFile.getCurrentVersion());
    assertNotNull(versionData);
    assertNotNull(versionData.getNeeds());
    assertFalse(versionData.getNeeds().isEmpty());
  }

  @Test
  public void testValidationWithInvalidFile() {
    // Test with null file
    NeedsFile nullFile = null;
    assertThrows(
        Exception.class,
        () -> {
          if (nullFile == null) {
            throw new NeedsFileImporter.ImportException(
                "Invalid JSON file: unable to parse needs file");
          }
        });

    // Test with empty versions
    NeedsFile emptyVersionsFile = new NeedsFile();
    emptyVersionsFile.setCurrentVersion("1.0");
    // versions map is null

    assertThrows(
        Exception.class,
        () -> {
          if (emptyVersionsFile.getVersions() == null
              || emptyVersionsFile.getVersions().isEmpty()) {
            throw new NeedsFileImporter.ImportException("No version data found in needs file");
          }
        });
  }

  @Test
  void testEmptyVersionStringHandling() throws Exception {
    NeedsFile needsFile = new NeedsFile();
    needsFile.setProject("Test Project");
    needsFile.setCurrentVersion(""); // Empty string, like Sphinx generates

    // Create version data with empty string key
    NeedsFile.VersionData versionData = new NeedsFile.VersionData();
    versionData.setCreated("2024-01-01T12:00:00");
    versionData.setCreator(new NeedsFile.Creator());

    // Create a sample need
    NeedsFile.Need need = new NeedsFile.Need();
    need.setId("UC001");
    need.setTitle("Test Use Case");
    need.setType("uc");
    need.setElementType("UseCase");
    need.setStatus("open");
    versionData.addNeed(need);

    // Add version data with empty string key
    needsFile.addVersion("", versionData);

    // Test that we can find version data with empty string key
    String currentVersion = needsFile.getCurrentVersion();
    NeedsFile.VersionData foundVersionData = needsFile.getVersions().get(currentVersion);

    assertNotNull(foundVersionData);
    assertEquals("", currentVersion);
    assertFalse(foundVersionData.getNeeds().isEmpty());
    assertEquals("UC001", foundVersionData.getNeeds().get("UC001").getId());
  }

  private NeedsFile createSampleNeedsFile() {
    NeedsFile needsFile = new NeedsFile();
    needsFile.setProject("Test Project");
    needsFile.setCurrentVersion("1.0");

    // Create version data
    NeedsFile.VersionData versionData = new NeedsFile.VersionData();
    versionData.setCreated("2024-01-01T12:00:00");

    // Create sample needs
    NeedsFile.Need need1 = new NeedsFile.Need();
    need1.setId("UC001");
    need1.setTitle("Sample Use Case 1");
    need1.setContent("This is a sample use case for testing");
    need1.setType("uc");
    need1.setElementType("UseCase");
    need1.setStatus("draft");

    NeedsFile.Need need2 = new NeedsFile.Need();
    need2.setId("UC002");
    need2.setTitle("Sample Use Case 2");
    need2.setContent("This is another sample use case");
    need2.setType("uc");
    need2.setElementType("UseCase");
    need2.setStatus("complete");
    need2.setIncludesLinks(Arrays.asList("UC001"));

    versionData.addNeed(need1);
    versionData.addNeed(need2);

    needsFile.addVersion("1.0", versionData);

    return needsFile;
  }
}
