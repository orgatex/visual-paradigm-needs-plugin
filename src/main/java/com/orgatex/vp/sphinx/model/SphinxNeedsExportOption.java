package com.orgatex.vp.sphinx.model;

import com.vp.plugin.diagram.IDiagramUIModel;

/**
 * Export configuration for Sphinx-Needs JSON format, following Visual Paradigm's export option
 * pattern.
 *
 * <p>This class provides a structured way to configure Sphinx-Needs export operations, similar to
 * VP's built-in ExportXMLOption class.
 */
public final class SphinxNeedsExportOption {

  private final IDiagramUIModel[] selectedDiagrams;
  private boolean includeMetadata = true;
  private boolean includeConnections = true;
  private boolean includeActors = true;
  private boolean includeUseCases = true;
  private String outputFormat = "json";

  private SphinxNeedsExportOption(IDiagramUIModel[] diagrams) {
    this.selectedDiagrams = diagrams != null ? diagrams.clone() : new IDiagramUIModel[0];
  }

  /**
   * Create export option for a single diagram.
   *
   * @param diagram the diagram to export
   * @return configured export option
   */
  public static SphinxNeedsExportOption toExportDiagram(IDiagramUIModel diagram) {
    if (diagram == null) {
      throw new IllegalArgumentException("Diagram cannot be null");
    }
    return new SphinxNeedsExportOption(new IDiagramUIModel[] {diagram});
  }

  /**
   * Create export option for multiple diagrams.
   *
   * @param diagrams the diagrams to export
   * @return configured export option
   */
  public static SphinxNeedsExportOption toExportDiagrams(IDiagramUIModel[] diagrams) {
    if (diagrams == null || diagrams.length == 0) {
      throw new IllegalArgumentException("At least one diagram must be provided");
    }
    return new SphinxNeedsExportOption(diagrams);
  }

  /**
   * Get the selected diagrams for export.
   *
   * @return array of selected diagrams
   */
  public IDiagramUIModel[] getSelectedDiagrams() {
    return selectedDiagrams.clone();
  }

  /**
   * Check if metadata should be included in export.
   *
   * @return true if metadata should be included
   */
  public boolean isIncludeMetadata() {
    return includeMetadata;
  }

  /**
   * Set whether to include metadata in export.
   *
   * @param includeMetadata true to include metadata
   */
  public void setIncludeMetadata(boolean includeMetadata) {
    this.includeMetadata = includeMetadata;
  }

  /**
   * Check if connections should be included in export.
   *
   * @return true if connections should be included
   */
  public boolean isIncludeConnections() {
    return includeConnections;
  }

  /**
   * Set whether to include connections in export.
   *
   * @param includeConnections true to include connections
   */
  public void setIncludeConnections(boolean includeConnections) {
    this.includeConnections = includeConnections;
  }

  /**
   * Check if actors should be included in export.
   *
   * @return true if actors should be included
   */
  public boolean isIncludeActors() {
    return includeActors;
  }

  /**
   * Set whether to include actors in export.
   *
   * @param includeActors true to include actors
   */
  public void setIncludeActors(boolean includeActors) {
    this.includeActors = includeActors;
  }

  /**
   * Check if use cases should be included in export.
   *
   * @return true if use cases should be included
   */
  public boolean isIncludeUseCases() {
    return includeUseCases;
  }

  /**
   * Set whether to include use cases in export.
   *
   * @param includeUseCases true to include use cases
   */
  public void setIncludeUseCases(boolean includeUseCases) {
    this.includeUseCases = includeUseCases;
  }

  /**
   * Get the output format.
   *
   * @return output format (currently always "json")
   */
  public String getOutputFormat() {
    return outputFormat;
  }

  /**
   * Set the output format.
   *
   * @param outputFormat the output format (currently only "json" is supported)
   */
  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat != null ? outputFormat : "json";
  }
}
