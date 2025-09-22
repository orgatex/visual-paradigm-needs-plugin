package com.orgatex.vp.sphinx.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Unit tests for NeedsFile model with requirements relationship extensions. */
class NeedsFileRequirementsTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testRequirementRelationshipFieldsSerialization() throws Exception {
    // Given
    NeedsFile.Need need = new NeedsFile.Need("REQ_001", "Test Requirement", "req");
    need.getDeriveLinks().add("REQ_002");
    need.getDeriveLinks().add("REQ_003");
    need.getContainsLinks().add("REQ_004");
    need.getRefinesLinks().add("UC_001");

    // When
    String json = objectMapper.writeValueAsString(need);

    // Then
    assertAll(
        () -> assertTrue(json.contains("\"derive\":[\"REQ_002\",\"REQ_003\"]")),
        () -> assertTrue(json.contains("\"contains\":[\"REQ_004\"]")),
        () -> assertTrue(json.contains("\"refines\":[\"UC_001\"]")));
  }

  @Test
  void testRequirementRelationshipFieldsDeserialization() throws Exception {
    // Given
    String json =
        "{"
            + "\"id\":\"REQ_001\","
            + "\"title\":\"Test Requirement\","
            + "\"type\":\"req\","
            + "\"derive\":[\"REQ_002\",\"REQ_003\"],"
            + "\"contains\":[\"REQ_004\"],"
            + "\"refines\":[\"UC_001\"]"
            + "}";

    // When
    NeedsFile.Need need = objectMapper.readValue(json, NeedsFile.Need.class);

    // Then
    assertAll(
        () -> assertEquals("REQ_001", need.getId()),
        () -> assertEquals("Test Requirement", need.getTitle()),
        () -> assertEquals("req", need.getType()),
        () -> assertEquals(Arrays.asList("REQ_002", "REQ_003"), need.getDeriveLinks()),
        () -> assertEquals(Arrays.asList("REQ_004"), need.getContainsLinks()),
        () -> assertEquals(Arrays.asList("UC_001"), need.getRefinesLinks()));
  }

  @Test
  void testCompleteRequirementWithAllRelationships() throws Exception {
    // Given
    NeedsFile needsFile = new NeedsFile();
    needsFile.setProject("Test Requirements Project");
    needsFile.setCurrentVersion("1.0");

    NeedsFile.VersionData versionData = new NeedsFile.VersionData();
    versionData.setCreated("2024-01-01T00:00:00");
    versionData.setCreator(new NeedsFile.Creator());

    // Create parent requirement
    NeedsFile.Need parentReq = new NeedsFile.Need("REQ_PARENT", "Parent Requirement", "req");
    parentReq.setContent("This is a parent requirement that contains child requirements");
    parentReq.setPriority("high");
    parentReq.getContainsLinks().add("REQ_CHILD1");
    parentReq.getContainsLinks().add("REQ_CHILD2");

    // Create child requirement that derives from parent
    NeedsFile.Need childReq = new NeedsFile.Need("REQ_CHILD1", "Child Requirement", "req");
    childReq.setContent("This is a child requirement derived from parent");
    childReq.setPriority("medium");
    childReq.getDeriveLinks().add("REQ_PARENT");

    // Create requirement that refines a use case
    NeedsFile.Need refinementReq =
        new NeedsFile.Need("REQ_REFINE", "Refinement Requirement", "req");
    refinementReq.setContent("This requirement refines a use case");
    refinementReq.setPriority("low");
    refinementReq.getRefinesLinks().add("UC_LOGIN");

    // Create use case
    NeedsFile.Need useCase = new NeedsFile.Need("UC_LOGIN", "User Login", "uc");
    useCase.setContent("User can login to the system");

    versionData.addNeed(parentReq);
    versionData.addNeed(childReq);
    versionData.addNeed(refinementReq);
    versionData.addNeed(useCase);

    needsFile.addVersion("1.0", versionData);

    // When
    String json = objectMapper.writeValueAsString(needsFile);
    NeedsFile deserializedFile = objectMapper.readValue(json, NeedsFile.class);

    // Then
    NeedsFile.VersionData deserializedVersion = deserializedFile.getVersions().get("1.0");
    assertNotNull(deserializedVersion);

    NeedsFile.Need deserializedParent = deserializedVersion.getNeeds().get("REQ_PARENT");
    assertAll(
        () -> assertNotNull(deserializedParent),
        () -> assertEquals(2, deserializedParent.getContainsLinks().size()),
        () -> assertTrue(deserializedParent.getContainsLinks().contains("REQ_CHILD1")),
        () -> assertTrue(deserializedParent.getContainsLinks().contains("REQ_CHILD2")));

    NeedsFile.Need deserializedChild = deserializedVersion.getNeeds().get("REQ_CHILD1");
    assertAll(
        () -> assertNotNull(deserializedChild),
        () -> assertEquals(1, deserializedChild.getDeriveLinks().size()),
        () -> assertTrue(deserializedChild.getDeriveLinks().contains("REQ_PARENT")));

    NeedsFile.Need deserializedRefinement = deserializedVersion.getNeeds().get("REQ_REFINE");
    assertAll(
        () -> assertNotNull(deserializedRefinement),
        () -> assertEquals(1, deserializedRefinement.getRefinesLinks().size()),
        () -> assertTrue(deserializedRefinement.getRefinesLinks().contains("UC_LOGIN")));
  }

  @Test
  void testEmptyRequirementRelationships() throws Exception {
    // Given
    NeedsFile.Need need = new NeedsFile.Need("REQ_001", "Test Requirement", "req");

    // When
    String json = objectMapper.writeValueAsString(need);
    NeedsFile.Need deserializedNeed = objectMapper.readValue(json, NeedsFile.Need.class);

    // Then - empty lists should be preserved
    assertAll(
        () -> assertNotNull(deserializedNeed.getDeriveLinks()),
        () -> assertTrue(deserializedNeed.getDeriveLinks().isEmpty()),
        () -> assertNotNull(deserializedNeed.getContainsLinks()),
        () -> assertTrue(deserializedNeed.getContainsLinks().isEmpty()),
        () -> assertNotNull(deserializedNeed.getRefinesLinks()),
        () -> assertTrue(deserializedNeed.getRefinesLinks().isEmpty()));
  }

  @Test
  void testRequirementRelationshipsBackwardCompatibility() throws Exception {
    // Given - JSON without new relationship fields (backward compatibility)
    String json =
        "{"
            + "\"id\":\"REQ_001\","
            + "\"title\":\"Test Requirement\","
            + "\"type\":\"req\","
            + "\"content\":\"This is a test requirement\","
            + "\"status\":\"open\","
            + "\"priority\":\"high\""
            + "}";

    // When
    NeedsFile.Need need = objectMapper.readValue(json, NeedsFile.Need.class);

    // Then - new fields should be initialized as empty lists
    assertAll(
        () -> assertEquals("REQ_001", need.getId()),
        () -> assertEquals("Test Requirement", need.getTitle()),
        () -> assertEquals("req", need.getType()),
        () -> assertNotNull(need.getDeriveLinks()),
        () -> assertTrue(need.getDeriveLinks().isEmpty()),
        () -> assertNotNull(need.getContainsLinks()),
        () -> assertTrue(need.getContainsLinks().isEmpty()),
        () -> assertNotNull(need.getRefinesLinks()),
        () -> assertTrue(need.getRefinesLinks().isEmpty()));
  }

  @Test
  void testRequirementTypesSupport() throws Exception {
    // Given - Create needs with different types including requirements
    NeedsFile needsFile = new NeedsFile();
    needsFile.setCurrentVersion("1.0");

    NeedsFile.VersionData versionData = new NeedsFile.VersionData();

    // Create different types of needs
    NeedsFile.Need requirement = new NeedsFile.Need("REQ_001", "Functional Requirement", "req");
    NeedsFile.Need useCase = new NeedsFile.Need("UC_001", "Login Use Case", "uc");
    NeedsFile.Need actor = new NeedsFile.Need("ACT_001", "System User", "act");

    // Set up relationships
    requirement.getRefinesLinks().add("UC_001");
    useCase.getAssociatesLinks().add("ACT_001");

    versionData.addNeed(requirement);
    versionData.addNeed(useCase);
    versionData.addNeed(actor);

    needsFile.addVersion("1.0", versionData);

    // When
    String json = objectMapper.writeValueAsString(needsFile);
    NeedsFile deserializedFile = objectMapper.readValue(json, NeedsFile.class);

    // Then
    NeedsFile.VersionData deserializedVersion = deserializedFile.getVersions().get("1.0");
    assertEquals(3, deserializedVersion.getNeeds().size());

    assertAll(
        () -> assertEquals("req", deserializedVersion.getNeeds().get("REQ_001").getType()),
        () -> assertEquals("uc", deserializedVersion.getNeeds().get("UC_001").getType()),
        () -> assertEquals("act", deserializedVersion.getNeeds().get("ACT_001").getType()));
  }
}
