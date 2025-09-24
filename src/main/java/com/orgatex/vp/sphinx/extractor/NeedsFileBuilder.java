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
    if (projectName == null) {
      throw new IllegalArgumentException("Project name cannot be null");
    }
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
      // Skip needs that already exist (by ID) to avoid duplicates
      if (!versionData.getNeeds().containsKey(need.getId())) {
        versionData.addNeed(need);
      } else {
        System.out.println("Warning: Skipping duplicate need with ID: " + need.getId());
      }
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

    // Contains relationships: populate "contains" field
    for (Map.Entry<String, Set<String>> entry :
        relationshipMaps.getContainsRelationships().entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        Set<String> containsLinks = new HashSet<>();

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            containsLinks.add(toUserId);
          }
        }

        fromNeed.setContainsLinks(new ArrayList<>(containsLinks));
      }
    }

    // Derive relationships: populate "derive" field
    for (Map.Entry<String, Set<String>> entry :
        relationshipMaps.getDeriveRelationships().entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        Set<String> deriveLinks = new HashSet<>();

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            deriveLinks.add(toUserId);
          }
        }

        fromNeed.setDeriveLinks(new ArrayList<>(deriveLinks));
      }
    }

    // Refines relationships: populate "refines" field
    for (Map.Entry<String, Set<String>> entry :
        relationshipMaps.getRefinesRelationships().entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        Set<String> refinesLinks = new HashSet<>();

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            refinesLinks.add(toUserId);
          }
        }

        fromNeed.setRefinesLinks(new ArrayList<>(refinesLinks));
      }
    }
  }
}
