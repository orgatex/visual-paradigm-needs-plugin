package com.orgatex.vp.sphinx.extractor;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.diagram.IDiagramUIModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Orchestrates the extraction and building of complete NeedsFile objects. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NeedsFileBuilder {

  /** Build a complete NeedsFile from a diagram context. */
  public static NeedsFile buildFromDiagram(IDiagramUIModel diagram) {
    if (diagram == null) {
      throw new IllegalArgumentException("Diagram cannot be null");
    }

    // Extract all models from the project
    NeedsModelExtractor.ExtractionResult modelResult = NeedsModelExtractor.extractAllModels();

    // Extract all relationships from the project
    NeedsRelationshipExtractor.RelationshipMaps relationshipMaps =
        NeedsRelationshipExtractor.extractAllRelationships();

    // Build the complete NeedsFile
    return buildNeedsFile(
        VpModelProcessor.sanitizeName(diagram.getName()),
        modelResult.getNeeds(),
        modelResult.getVpIdToUserId(),
        relationshipMaps);
  }

  /** Build a complete NeedsFile directly (for project-level extraction). */
  public static NeedsFile buildFromProject(String projectName) {
    // Extract all models from the project
    NeedsModelExtractor.ExtractionResult modelResult = NeedsModelExtractor.extractAllModels();

    // Extract all relationships from the project
    NeedsRelationshipExtractor.RelationshipMaps relationshipMaps =
        NeedsRelationshipExtractor.extractAllRelationships();

    // Build the complete NeedsFile
    return buildNeedsFile(
        VpModelProcessor.sanitizeName(projectName),
        modelResult.getNeeds(),
        modelResult.getVpIdToUserId(),
        relationshipMaps);
  }

  /** Build the final NeedsFile by combining extracted elements and relationships. */
  private static NeedsFile buildNeedsFile(
      String projectName,
      List<NeedsFile.Need> needs,
      Map<String, String> vpIdToUserId,
      NeedsRelationshipExtractor.RelationshipMaps relationshipMaps) {

    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    // Create the NeedsFile structure
    NeedsFile needsFile = new NeedsFile();
    needsFile.setCreated(timestamp);
    needsFile.setProject(projectName);
    needsFile.setCurrentVersion("1.0");

    // Create version data
    NeedsFile.VersionData versionData = new NeedsFile.VersionData();
    versionData.setCreated(timestamp);
    versionData.setCreator(new NeedsFile.Creator());

    // Add all extracted needs to the version
    for (NeedsFile.Need need : needs) {
      // Handle duplicate IDs by appending counter
      String finalId = need.getId();
      int counter = 1;
      while (versionData.getNeeds().containsKey(finalId)) {
        finalId = need.getId() + "_" + counter;
        counter++;
      }

      if (!finalId.equals(need.getId())) {
        // Update the need's ID if we had to change it
        need = new NeedsFile.Need(finalId, need.getTitle(), need.getType());
        need.setContent(need.getContent());
        need.setStatus(need.getStatus());
        need.setTags(need.getTags());
        need.setPriority(need.getPriority());
        need.setElementType(need.getElementType());
        need.setVpModelId(need.getVpModelId());
      }

      versionData.addNeed(need);
    }

    // Apply relationships to the needs
    applyRelationshipsToNeeds(versionData, relationshipMaps, vpIdToUserId);

    needsFile.addVersion("1.0", versionData);
    return needsFile;
  }

  /** Apply extracted relationships to the needs in the version data. */
  private static void applyRelationshipsToNeeds(
      NeedsFile.VersionData versionData,
      NeedsRelationshipExtractor.RelationshipMaps relationshipMaps,
      Map<String, String> vpIdToUserId) {

    // Include relationships: populate "includes" field
    for (Map.Entry<String, Set<String>> entry :
        relationshipMaps.getIncludeRelationships().entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        Set<String> includeLinks = new HashSet<>();

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            includeLinks.add(toUserId);
          }
        }

        fromNeed.setIncludesLinks(new ArrayList<>(includeLinks));
      }
    }

    // Extend relationships: populate "extends" field
    for (Map.Entry<String, Set<String>> entry :
        relationshipMaps.getExtendRelationships().entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        Set<String> extendLinks = new HashSet<>();

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            extendLinks.add(toUserId);
          }
        }

        fromNeed.setExtendsLinks(new ArrayList<>(extendLinks));
      }
    }

    // Associate relationships: populate "associates" field
    for (Map.Entry<String, Set<String>> entry :
        relationshipMaps.getAssociateRelationships().entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        Set<String> associateLinks = new HashSet<>();

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            associateLinks.add(toUserId);
          }
        }

        fromNeed.setAssociatesLinks(new ArrayList<>(associateLinks));
      }
    }
  }
}
