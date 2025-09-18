package com.orgatex.vp.sphinx.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.diagram.IDiagramUIModel;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Core importer class for importing sphinx-needs JSON files into Visual Paradigm use case diagrams.
 */
public class NeedsFileImporter {

  private final ObjectMapper objectMapper;
  private final UseCaseDiagramBuilder diagramBuilder;

  public NeedsFileImporter() {
    this.objectMapper = new ObjectMapper();
    this.diagramBuilder = new UseCaseDiagramBuilder();
  }

  /**
   * Import a needs JSON file and create a use case diagram.
   *
   * @param jsonFile The JSON file to import
   * @return The created diagram
   * @throws ImportException if import fails
   */
  public IDiagramUIModel importFromFile(File jsonFile) throws ImportException {
    try {
      // Parse the JSON file
      NeedsFile needsFile = parseNeedsFile(jsonFile);

      // Validate the structure
      validateNeedsFile(needsFile);

      // Extract the version data (using the current version)
      String currentVersion = needsFile.getCurrentVersion();
      NeedsFile.VersionData versionData = needsFile.getVersions().get(currentVersion);

      if (versionData == null) {
        throw new ImportException("No version data found for version: " + currentVersion);
      }

      // Create the diagram
      String diagramName = createDiagramName(needsFile.getProject(), jsonFile.getName());
      IDiagramUIModel diagram = diagramBuilder.createUseCaseDiagram(diagramName);

      // Import needs into the diagram
      importNeeds(diagram, versionData);

      // Apply auto-layout to organize elements nicely
      ApplicationManager.instance()
          .getDiagramManager()
          .layout(diagram, ApplicationManager.instance().getDiagramManager().LAYOUT_ORGANIC);

      // Open the diagram
      ApplicationManager.instance().getDiagramManager().openDiagram(diagram);

      return diagram;

    } catch (IOException e) {
      throw new ImportException("Failed to read JSON file: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new ImportException("Failed to import diagram: " + e.getMessage(), e);
    }
  }

  /** Parse the needs JSON file. */
  private NeedsFile parseNeedsFile(File jsonFile) throws IOException {
    return objectMapper.readValue(jsonFile, NeedsFile.class);
  }

  /** Validate the needs file structure. */
  private void validateNeedsFile(NeedsFile needsFile) throws ImportException {
    if (needsFile == null) {
      throw new ImportException("Invalid JSON file: unable to parse needs file");
    }

    if (needsFile.getCurrentVersion() == null) {
      throw new ImportException("No current version specified in needs file");
    }

    if (needsFile.getVersions() == null || needsFile.getVersions().isEmpty()) {
      throw new ImportException("No version data found in needs file");
    }

    String currentVersion = needsFile.getCurrentVersion();
    NeedsFile.VersionData versionData = needsFile.getVersions().get(currentVersion);

    if (versionData == null) {
      throw new ImportException("Version data not found for current version: " + currentVersion);
    }

    if (versionData.getNeeds() == null || versionData.getNeeds().isEmpty()) {
      throw new ImportException("No needs found in version data");
    }
  }

  /** Create a diagram name from project name and file name. */
  private String createDiagramName(String projectName, String fileName) {
    String baseName = projectName != null ? projectName : "Imported Use Cases";

    // Remove .json extension if present
    if (fileName.toLowerCase().endsWith(".json")) {
      fileName = fileName.substring(0, fileName.length() - 5);
    }

    return baseName + " (" + fileName + ")";
  }

  /** Import all needs into the diagram. */
  private void importNeeds(IDiagramUIModel diagram, NeedsFile.VersionData versionData)
      throws ImportException {
    Map<String, NeedsFile.Need> needs = versionData.getNeeds();

    System.out.println("Importing " + needs.size() + " needs into diagram: " + diagram.getName());

    // Step 1: Create all use case elements
    try {
      diagramBuilder.createUseCaseElements(diagram, needs);

      // Step 2: Create relationships between elements
      diagramBuilder.createRelationships(diagram, needs);
    } catch (Exception e) {
      throw new ImportException("Failed to create diagram elements: " + e.getMessage(), e);
    }

    System.out.println("Successfully imported " + needs.size() + " needs");
  }

  /** Exception for import operations. */
  public static class ImportException extends Exception {
    public ImportException(String message) {
      super(message);
    }

    public ImportException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
