package com.orgatex.vp.sphinx.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.orgatex.vp.sphinx.model.NeedsFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Exports diagram data to sphinx-needs compatible JSON format. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonExporter {

  private static final ObjectMapper objectMapper = createObjectMapper();
  private static final JsonSchema schema = loadSchema();

  private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    return mapper;
  }

  private static JsonSchema loadSchema() {
    try {
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
      InputStream schemaStream =
          JsonExporter.class.getClassLoader().getResourceAsStream("sphinx-needs-5.1.0-schema.json");

      if (schemaStream == null) {
        System.err.println("Warning: Schema file not found, skipping validation");
        return null;
      }

      return factory.getSchema(schemaStream);
    } catch (Exception e) {
      System.err.println("Warning: Failed to load schema, skipping validation: " + e.getMessage());
      return null;
    }
  }

  private static void validateAgainstSchema(NeedsFile needsFile) {
    if (schema == null) {
      return; // Skip validation if schema not available
    }

    try {
      // Convert NeedsFile to JsonNode for validation
      JsonNode jsonNode = objectMapper.valueToTree(needsFile);

      // Validate against schema
      Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

      if (!validationMessages.isEmpty()) {
        System.err.println("Schema validation warnings:");
        for (ValidationMessage message : validationMessages) {
          System.err.println(
              "  - " + message.getMessage() + " at " + message.getInstanceLocation());
        }
      } else {
        System.out.println("✅ Schema validation passed");
      }
    } catch (Exception e) {
      System.err.println("Warning: Schema validation failed: " + e.getMessage());
    }
  }

  public static void exportToFile(NeedsFile needsFile, File outputFile) throws IOException {
    if (needsFile == null) {
      throw new IllegalArgumentException("NeedsFile cannot be null");
    }
    if (outputFile == null) {
      throw new IllegalArgumentException("Output file cannot be null");
    }

    System.out.println("=== DEBUG: JsonExporter.exportToFile ===");
    System.out.println("Output file: " + outputFile.getAbsolutePath());
    System.out.println("NeedsFile project: " + needsFile.getProject());
    System.out.println("NeedsFile current_version: " + needsFile.getCurrentVersion());
    System.out.println(
        "NeedsFile versions count: "
            + (needsFile.getVersions() != null ? needsFile.getVersions().size() : "null"));

    if (needsFile.getVersions() != null && !needsFile.getVersions().isEmpty()) {
      for (String version : needsFile.getVersions().keySet()) {
        var versionData = needsFile.getVersions().get(version);
        System.out.println(
            "Version '"
                + version
                + "' needs count: "
                + (versionData.getNeeds() != null ? versionData.getNeeds().size() : "null"));
      }
    }

    // Ensure parent directory exists
    File parentDir = outputFile.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs();
    }

    // Validate against schema before writing
    validateAgainstSchema(needsFile);

    objectMapper.writeValue(outputFile, needsFile);
    System.out.println("=== DEBUG: JSON export completed ===");
  }

  public static String exportToString(NeedsFile needsFile) throws IOException {
    if (needsFile == null) {
      throw new IllegalArgumentException("NeedsFile cannot be null");
    }

    // Validate against schema before converting to string
    validateAgainstSchema(needsFile);

    return objectMapper.writeValueAsString(needsFile);
  }
}
