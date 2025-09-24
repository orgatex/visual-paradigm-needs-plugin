package com.orgatex.vp.sphinx.service;

import com.orgatex.vp.sphinx.extractor.NeedsFileBuilder;
import com.orgatex.vp.sphinx.generator.JsonExporter;
import com.orgatex.vp.sphinx.model.NeedsFile;
import com.orgatex.vp.sphinx.model.SphinxNeedsExportOption;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ViewManager;
import com.vp.plugin.diagram.IDiagramUIModel;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Service class for exporting diagrams to Sphinx-Needs JSON format.
 *
 * <p>This service provides structured export functionality following Visual Paradigm's service
 * patterns, with enhanced status reporting and configuration support.
 */
public class SphinxNeedsExporter {

  private static final String STATUS_MESSAGE_ID = "sphinx_needs_export";

  private final ViewManager viewManager;

  public SphinxNeedsExporter() {
    this.viewManager = ApplicationManager.instance().getViewManager();
  }

  /**
   * Export diagrams according to the provided export option.
   *
   * @param option the export configuration
   * @param outputFile the target output file
   * @throws IOException if file operations fail
   * @throws IllegalArgumentException if invalid parameters are provided
   */
  public void export(SphinxNeedsExportOption option, File outputFile) throws IOException {
    if (option == null) {
      throw new IllegalArgumentException("Export option cannot be null");
    }
    if (outputFile == null) {
      throw new IllegalArgumentException("Output file cannot be null");
    }

    IDiagramUIModel[] diagrams = option.getSelectedDiagrams();
    if (diagrams.length == 0) {
      throw new IllegalArgumentException("No diagrams selected for export");
    }

    try {
      showStatus("Starting Sphinx-Needs export...");

      // Process each diagram
      NeedsFile aggregatedNeeds = new NeedsFile();

      for (int i = 0; i < diagrams.length; i++) {
        IDiagramUIModel diagram = diagrams[i];
        showStatus(
            "Processing diagram: "
                + diagram.getName()
                + " ("
                + (i + 1)
                + "/"
                + diagrams.length
                + ")");

        NeedsFile diagramNeeds = extractDiagramContent(diagram, option);
        mergeDiagramContent(aggregatedNeeds, diagramNeeds);
      }

      showStatus("Writing output file...");
      JsonExporter.exportToFile(aggregatedNeeds, outputFile);

      showStatus("Export completed successfully: " + outputFile.getName());

    } catch (Exception e) {
      showStatus("Export failed: " + e.getMessage());
      throw e;
    } finally {
      clearStatus();
    }
  }

  /** Extract content from a single diagram according to export options. */
  private NeedsFile extractDiagramContent(IDiagramUIModel diagram, SphinxNeedsExportOption option) {
    NeedsFile needsFile = NeedsFileBuilder.buildFromProject(diagram.getName());

    // Apply export filters based on options
    // Get the current version's needs
    String currentVersion = needsFile.getCurrentVersion();
    NeedsFile.VersionData versionData = needsFile.getVersions().get(currentVersion);

    if (versionData != null && versionData.getNeeds() != null) {
      Map<String, NeedsFile.Need> needs = versionData.getNeeds();

      if (!option.isIncludeActors()) {
        needs.entrySet().removeIf(entry -> "actor".equals(entry.getValue().getType()));
      }

      if (!option.isIncludeUseCases()) {
        needs.entrySet().removeIf(entry -> "usecase".equals(entry.getValue().getType()));
      }

      if (!option.isIncludeConnections()) {
        needs
            .values()
            .forEach(
                need -> {
                  need.getLinks().clear();
                  need.getExtendsLinks().clear();
                  need.getIncludesLinks().clear();
                  need.getAssociatesLinks().clear();
                });
      }

      if (!option.isIncludeMetadata()) {
        needs
            .values()
            .forEach(
                need -> {
                  need.setContent("");
                  need.setPriority("");
                  need.setStatus("");
                });
      }

      // Update the needs amount
      versionData.setNeedsAmount(needs.size());
    }

    return needsFile;
  }

  /** Merge content from multiple diagrams into a single NeedsFile. */
  private void mergeDiagramContent(NeedsFile target, NeedsFile source) {
    // Initialize target if it's empty
    if (target.getCurrentVersion() == null) {
      target.setCurrentVersion(source.getCurrentVersion());
    }
    if (target.getProject() == null) {
      target.setProject(source.getProject());
    }
    if (target.getCreated() == null) {
      target.setCreated(source.getCreated());
    }

    String targetVersion = target.getCurrentVersion();
    String sourceVersion = source.getCurrentVersion();

    NeedsFile.VersionData targetVersionData = target.getVersions().get(targetVersion);
    NeedsFile.VersionData sourceVersionData = source.getVersions().get(sourceVersion);

    // If target doesn't have the version, copy it entirely from source
    if (targetVersionData == null && sourceVersionData != null) {
      target.addVersion(targetVersion, sourceVersionData);
      return;
    }

    // If both have the version, merge the needs
    if (targetVersionData != null && sourceVersionData != null) {
      Map<String, NeedsFile.Need> targetNeeds = targetVersionData.getNeeds();
      Map<String, NeedsFile.Need> sourceNeeds = sourceVersionData.getNeeds();

      // Add all needs from source, avoiding duplicates by ID
      sourceNeeds.forEach(
          (id, need) -> {
            if (!targetNeeds.containsKey(id)) {
              targetNeeds.put(id, need);
            }
          });

      // Update the needs amount
      targetVersionData.setNeedsAmount(targetNeeds.size());
    }
  }

  /** Show status message to user. */
  private void showStatus(String message) {
    viewManager.showMessage(message, STATUS_MESSAGE_ID);
  }

  /** Clear status messages. */
  private void clearStatus() {
    viewManager.clearMessages(STATUS_MESSAGE_ID);
  }

  /**
   * Export a single diagram with default options.
   *
   * @param diagram the diagram to export
   * @param outputFile the target output file
   * @throws IOException if file operations fail
   */
  public void exportDiagram(IDiagramUIModel diagram, File outputFile) throws IOException {
    SphinxNeedsExportOption option = SphinxNeedsExportOption.toExportDiagram(diagram);
    export(option, outputFile);
  }

  /**
   * Export multiple diagrams with default options.
   *
   * @param diagrams the diagrams to export
   * @param outputFile the target output file
   * @throws IOException if file operations fail
   */
  public void exportDiagrams(IDiagramUIModel[] diagrams, File outputFile) throws IOException {
    SphinxNeedsExportOption option = SphinxNeedsExportOption.toExportDiagrams(diagrams);
    export(option, outputFile);
  }
}
