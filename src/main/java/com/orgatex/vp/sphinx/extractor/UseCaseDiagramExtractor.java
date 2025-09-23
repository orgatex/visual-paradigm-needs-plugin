package com.orgatex.vp.sphinx.extractor;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.diagram.IDiagramUIModel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Legacy extractor that delegates to the new modular architecture.
 *
 * @deprecated Use {@link NeedsFileBuilder} directly for new code.
 */
@Deprecated
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UseCaseDiagramExtractor {

  /**
   * Extract a NeedsFile from a Visual Paradigm diagram.
   *
   * <p>This method now delegates to the new modular architecture consisting of:
   *
   * <ul>
   *   <li>{@link NeedsModelExtractor} - Extracts model elements
   *   <li>{@link NeedsRelationshipExtractor} - Extracts relationships
   *   <li>{@link NeedsFileBuilder} - Orchestrates the build process
   * </ul>
   *
   * @param diagram The Visual Paradigm diagram to extract from
   * @return A complete NeedsFile with all elements and relationships
   * @throws IllegalArgumentException if diagram is null
   */
  public static NeedsFile extractDiagram(IDiagramUIModel diagram) {
    return NeedsFileBuilder.buildFromDiagram(diagram);
  }

  /**
   * Legacy method for sanitizing names.
   *
   * @deprecated Use {@link VpModelProcessor#sanitizeName(String)} instead.
   */
  @Deprecated
  private static String sanitizeName(String input) {
    return VpModelProcessor.sanitizeName(input);
  }

  /**
   * Legacy method for sanitizing IDs.
   *
   * @deprecated Use {@link VpModelProcessor#sanitizeId(String)} instead.
   */
  @Deprecated
  private static String sanitizeId(String input) {
    return VpModelProcessor.sanitizeId(input);
  }

  /**
   * Legacy method for sanitizing tags.
   *
   * @deprecated Use {@link VpModelProcessor#sanitizeTag(String)} instead.
   */
  @Deprecated
  private static String sanitizeTag(String input) {
    return VpModelProcessor.sanitizeTag(input);
  }
}
