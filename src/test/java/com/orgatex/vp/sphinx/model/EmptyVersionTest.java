package com.orgatex.vp.sphinx.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import org.junit.jupiter.api.Test;

public class EmptyVersionTest {

  @Test
  void testParseFileWithEmptyVersion() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    File testFile = new File("src/test/resources/test-empty-version.json");

    NeedsFile needsFile = objectMapper.readValue(testFile, NeedsFile.class);

    assertNotNull(needsFile);
    assertEquals("", needsFile.getCurrentVersion());
    assertEquals("Test Empty Version", needsFile.getProject());

    assertNotNull(needsFile.getVersions());
    assertTrue(needsFile.getVersions().containsKey(""));

    NeedsFile.VersionData versionData = needsFile.getVersions().get("");
    assertNotNull(versionData);
    assertNotNull(versionData.getNeeds());
    assertEquals(2, versionData.getNeeds().size());

    assertTrue(versionData.getNeeds().containsKey("UC001"));
    assertTrue(versionData.getNeeds().containsKey("AC001"));

    NeedsFile.Need useCase = versionData.getNeeds().get("UC001");
    assertEquals("Test Use Case", useCase.getTitle());
    assertEquals("uc", useCase.getType());

    NeedsFile.Need actor = versionData.getNeeds().get("AC001");
    assertEquals("Test Actor", actor.getTitle());
    assertEquals("actor", actor.getType());
  }
}
