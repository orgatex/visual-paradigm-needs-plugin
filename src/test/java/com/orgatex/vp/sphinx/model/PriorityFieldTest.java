package com.orgatex.vp.sphinx.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Test priority field JSON serialization behavior. */
class PriorityFieldTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testPriorityNullIsOmitted() throws Exception {
    NeedsFile.Need need = new NeedsFile.Need("REQ001", "Test Requirement", "req");
    need.setPriority(null);

    String json = objectMapper.writeValueAsString(need);
    assertFalse(json.contains("priority"), "Null priority should be omitted from JSON");
  }

  @Test
  void testPriorityEmptyStringIsOmitted() throws Exception {
    NeedsFile.Need need = new NeedsFile.Need("REQ001", "Test Requirement", "req");
    need.setPriority("");

    String json = objectMapper.writeValueAsString(need);
    assertFalse(json.contains("priority"), "Empty priority should be omitted from JSON");
  }

  @Test
  void testPriorityValidValueIsIncluded() throws Exception {
    NeedsFile.Need need = new NeedsFile.Need("REQ001", "Test Requirement", "req");
    need.setPriority("high");

    String json = objectMapper.writeValueAsString(need);
    assertTrue(json.contains("\"priority\":\"high\""), "Valid priority should be included in JSON");
  }

  @Test
  void testPriorityWhitespaceIsIncluded() throws Exception {
    NeedsFile.Need need = new NeedsFile.Need("REQ001", "Test Requirement", "req");
    need.setPriority("   ");

    String json = objectMapper.writeValueAsString(need);
    assertTrue(
        json.contains("priority"), "Whitespace-only priority should be included with NON_EMPTY");
  }
}
