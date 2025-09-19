package com.orgatex.vp.sphinx.extractor;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.connector.IAssociationUIModel;
import com.vp.plugin.diagram.connector.IExtendUIModel;
import com.vp.plugin.diagram.connector.IIncludeUIModel;
import com.vp.plugin.diagram.shape.IActorUIModel;
import com.vp.plugin.diagram.shape.IUseCaseUIModel;
import com.vp.plugin.model.IActor;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IProject;
import com.vp.plugin.model.IUseCase;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Iterator;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Enhanced extractor that attempts to extract actual use cases from Visual Paradigm diagrams. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UseCaseDiagramExtractor {

  public static NeedsFile extractDiagram(IDiagramUIModel diagram) {
    if (diagram == null) {
      throw new IllegalArgumentException("Diagram cannot be null");
    }

    NeedsFile needsFile = new NeedsFile();
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    needsFile.setCreated(timestamp);
    needsFile.setProject(sanitizeName(diagram.getName()));
    needsFile.setCurrentVersion("1.0");

    // Create version data
    NeedsFile.VersionData versionData = new NeedsFile.VersionData();
    versionData.setCreated(timestamp);
    versionData.setCreator(new NeedsFile.Creator());

    // Create mapping to track VP internal IDs to User IDs
    Map<String, String> vpIdToUserId = new HashMap<>();

    // Extract ALL use cases and actors from the entire project
    extractAllProjectUseCasesAndActors(versionData, vpIdToUserId);

    // Extract relationships from the entire project
    extractAllProjectRelationships(versionData, vpIdToUserId);

    needsFile.addVersion("1.0", versionData);
    return needsFile;
  }

  private static void extractUseCasesAndActors(
      IDiagramUIModel diagram,
      NeedsFile.VersionData versionData,
      Map<String, String> vpIdToUserId) {
    try {
      // Get all diagram elements using the correct API
      IDiagramElement[] diagramElements = diagram.toDiagramElementArray();

      // Maps to track actors for each use case
      Map<String, Set<String>> useCaseActors = new HashMap<>();
      Map<String, String> actorNames = new HashMap<>(); // actorId -> actorName

      // First pass: collect and export all actors
      for (IDiagramElement element : diagramElements) {
        if (element instanceof IActorUIModel actorUI) {
          IModelElement actorModel = actorUI.getModelElement();
          if (actorModel != null) {
            actorNames.put(actorModel.getId(), actorModel.getName());
            // Export actor as separate need
            processActor(actorModel, versionData, vpIdToUserId);
          }
        }
      }

      // Second pass: process use cases and link with actors
      for (IDiagramElement element : diagramElements) {
        if (element instanceof IUseCaseUIModel useCaseUI) {
          IModelElement useCaseModel = useCaseUI.getModelElement();
          if (useCaseModel != null) {
            // Find associated actors (this will be enhanced when we process relationships)
            Set<String> associatedActors = findAssociatedActors(useCaseModel, actorNames);
            useCaseActors.put(useCaseModel.getId(), associatedActors);

            // Create the use case need
            processUseCase(useCaseModel, versionData, associatedActors, vpIdToUserId);
          }
        }
      }

    } catch (Exception e) {
      System.err.println("Error extracting use cases and actors: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void extractRelationships(
      IDiagramUIModel diagram,
      NeedsFile.VersionData versionData,
      Map<String, String> vpIdToUserId) {
    try {
      // Get all diagram elements to find connectors
      IDiagramElement[] diagramElements = diagram.toDiagramElementArray();

      // Maps to track relationships
      Map<String, Set<String>> includeRelationships = new HashMap<>();
      Map<String, Set<String>> extendRelationships = new HashMap<>();
      Map<String, Set<String>> associateRelationships = new HashMap<>();

      for (IDiagramElement element : diagramElements) {
        if (element instanceof IIncludeUIModel includeUI) {
          processIncludeRelationship(includeUI, includeRelationships);
        } else if (element instanceof IExtendUIModel extendUI) {
          processExtendRelationship(extendUI, extendRelationships);
        } else if (element instanceof IAssociationUIModel associateUI) {
          processAssociateRelationship(associateUI, associateRelationships);
        }
      }

      // Apply relationships to use cases
      applyRelationshipsToNeeds(
          versionData,
          includeRelationships,
          extendRelationships,
          associateRelationships,
          vpIdToUserId);

    } catch (Exception e) {
      System.err.println("Error extracting relationships: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void processUseCase(
      IModelElement useCase,
      NeedsFile.VersionData versionData,
      Set<String> associatedActors,
      Map<String, String> vpIdToUserId) {
    String name = useCase.getName();
    if (name == null || name.trim().isEmpty()) {
      name = "Unnamed Use Case";
    }

    // Use Visual Paradigm's User ID field
    String id = getUserId(useCase);

    // Handle null User ID by skipping this use case if no User ID is set
    if (id == null || id.trim().isEmpty()) {
      System.err.println("Warning: Skipping use case '" + name + "' - no User ID set");
      return; // Skip use cases without User ID
    }

    // Track VP internal ID to User ID mapping for relationship resolution
    vpIdToUserId.put(useCase.getId(), id);

    // Avoid duplicates by checking for existing ID or appending number
    String finalId = id;
    int counter = 1;
    while (versionData.getNeeds().containsKey(finalId)) {
      finalId = id + "_" + counter;
      counter++;
    }

    NeedsFile.Need need = new NeedsFile.Need(finalId, name, "uc");
    need.setContent(getDescription(useCase));
    need.setStatus(getStatus(useCase));
    need.setElementType("UseCase");
    need.setPriority(getRank(useCase));
    need.setVpModelId(useCase.getId()); // Store Visual Paradigm model ID

    // Set tags including any associated actors
    List<String> tags = new ArrayList<>();
    tags.add("usecase");
    tags.add("functional");

    // Add actor associations as tags
    if (associatedActors != null && !associatedActors.isEmpty()) {
      for (String actorName : associatedActors) {
        tags.add("actor:" + sanitizeTag(actorName));
      }
    }

    need.setTags(tags);

    versionData.addNeed(need);
  }

  private static void processActor(
      IModelElement actor, NeedsFile.VersionData versionData, Map<String, String> vpIdToUserId) {
    String name = actor.getName();
    if (name == null || name.trim().isEmpty()) {
      name = "Unnamed Actor";
    }

    // Use Visual Paradigm's User ID field for actors too
    String id = getUserId(actor);

    // Handle null User ID by skipping this actor if no User ID is set
    if (id == null || id.trim().isEmpty()) {
      System.err.println("Warning: Skipping actor '" + name + "' - no User ID set");
      return; // Skip actors without User ID
    }

    // Track VP internal ID to User ID mapping for relationship resolution
    vpIdToUserId.put(actor.getId(), id);

    // Avoid duplicates by checking for existing ID or appending number
    String finalId = id;
    int counter = 1;
    while (versionData.getNeeds().containsKey(finalId)) {
      finalId = id + "_" + counter;
      counter++;
    }

    NeedsFile.Need need = new NeedsFile.Need(finalId, name, "actor");
    need.setContent(getDescription(actor));
    need.setStatus("identify"); // Actors typically start in identify phase
    need.setElementType("Actor");
    need.setVpModelId(actor.getId()); // Store Visual Paradigm model ID

    // Set tags for actors
    List<String> tags = new ArrayList<>();
    tags.add("actor");
    tags.add("stakeholder");
    need.setTags(tags);

    versionData.addNeed(need);
  }

  private static Set<String> findAssociatedActors(
      IModelElement useCase, Map<String, String> actorNames) {
    Set<String> associatedActors = new HashSet<>();

    try {
      // Try to get relationships from the use case model element
      if (useCase != null) {
        // Use reflection to get relationships
        try {
          java.lang.reflect.Method getFromRelationshipsMethod =
              useCase.getClass().getMethod("getFromRelationships");
          Object[] fromRelationships = (Object[]) getFromRelationshipsMethod.invoke(useCase);

          if (fromRelationships != null) {
            for (Object relationship : fromRelationships) {
              String relatedActorId = getRelatedActorId(relationship);
              if (relatedActorId != null && actorNames.containsKey(relatedActorId)) {
                associatedActors.add(actorNames.get(relatedActorId));
              }
            }
          }
        } catch (Exception e) {
          // Try alternative method names
          try {
            java.lang.reflect.Method getToRelationshipsMethod =
                useCase.getClass().getMethod("getToRelationships");
            Object[] toRelationships = (Object[]) getToRelationshipsMethod.invoke(useCase);

            if (toRelationships != null) {
              for (Object relationship : toRelationships) {
                String relatedActorId = getRelatedActorId(relationship);
                if (relatedActorId != null && actorNames.containsKey(relatedActorId)) {
                  associatedActors.add(actorNames.get(relatedActorId));
                }
              }
            }
          } catch (Exception e2) {
            // Association relationships might not be directly accessible
            // We'll handle actor associations through diagram-level processing
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Error finding associated actors: " + e.getMessage());
    }

    return associatedActors;
  }

  private static String getRelatedActorId(Object relationship) {
    try {
      // Try to get the other end of the relationship
      java.lang.reflect.Method getFromMethod = relationship.getClass().getMethod("getFrom");
      Object fromElement = getFromMethod.invoke(relationship);

      java.lang.reflect.Method getToMethod = relationship.getClass().getMethod("getTo");
      Object toElement = getToMethod.invoke(relationship);

      // Check if either end is an actor
      if (fromElement != null && fromElement.getClass().getSimpleName().contains("Actor")) {
        java.lang.reflect.Method getIdMethod = fromElement.getClass().getMethod("getId");
        return (String) getIdMethod.invoke(fromElement);
      }

      if (toElement != null && toElement.getClass().getSimpleName().contains("Actor")) {
        java.lang.reflect.Method getIdMethod = toElement.getClass().getMethod("getId");
        return (String) getIdMethod.invoke(toElement);
      }
    } catch (Exception e) {
      // Ignore, not an actor relationship
    }
    return null;
  }

  private static void processIncludeRelationship(
      IIncludeUIModel includeUI, Map<String, Set<String>> includeRelationships) {
    try {
      // Get the model element of the include relationship
      IModelElement includeModel = includeUI.getModelElement();
      if (includeModel != null) {
        // Try to get from/to elements
        // The exact API may vary, but typically relationships have from/to
        String fromId = getFromElementId(includeUI);
        String toId = getToElementId(includeUI);

        if (fromId != null && toId != null) {
          includeRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
        }
      }
    } catch (Exception e) {
      System.err.println("Error processing include relationship: " + e.getMessage());
    }
  }

  private static void processExtendRelationship(
      IExtendUIModel extendUI, Map<String, Set<String>> extendRelationships) {
    try {
      // Get the model element of the extend relationship
      IModelElement extendModel = extendUI.getModelElement();
      if (extendModel != null) {
        String fromId = getFromElementId(extendUI);
        String toId = getToElementId(extendUI);

        if (fromId != null && toId != null) {
          // In VP extend relationships, the direction is opposite to semantic meaning
          // VP: base -> extending, but semantically: extending extends base
          extendRelationships.computeIfAbsent(toId, k -> new HashSet<>()).add(fromId);
        }
      }
    } catch (Exception e) {
      System.err.println("Error processing extend relationship: " + e.getMessage());
    }
  }

  private static void processAssociateRelationship(
      IAssociationUIModel associateUI, Map<String, Set<String>> associateRelationships) {
    try {
      // Get the model element of the association relationship
      IModelElement associateModel = associateUI.getModelElement();
      if (associateModel != null) {
        String fromId = getFromElementId(associateUI);
        String toId = getToElementId(associateUI);

        if (fromId != null && toId != null) {
          // For associations, we can create bidirectional links
          associateRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
          associateRelationships.computeIfAbsent(toId, k -> new HashSet<>()).add(fromId);
        }
      }
    } catch (Exception e) {
      System.err.println("Error processing associate relationship: " + e.getMessage());
    }
  }

  private static String getFromElementId(Object connector) {
    try {
      // Use reflection to get the from element, as the exact API method may vary
      java.lang.reflect.Method getFromMethod = connector.getClass().getMethod("getFrom");
      Object fromElement = getFromMethod.invoke(connector);

      if (fromElement instanceof IUseCaseUIModel useCaseUI) {
        IModelElement model = useCaseUI.getModelElement();
        return model != null ? model.getId() : null;
      } else if (fromElement instanceof IActorUIModel actorUI) {
        IModelElement model = actorUI.getModelElement();
        return model != null ? model.getId() : null;
      }
    } catch (Exception e) {
      // Try alternative method names or approaches
      try {
        java.lang.reflect.Method getFromShapeMethod =
            connector.getClass().getMethod("getFromShape");
        Object fromShape = getFromShapeMethod.invoke(connector);
        return extractElementId(fromShape);
      } catch (Exception e2) {
        System.err.println("Could not get from element: " + e2.getMessage());
      }
    }
    return null;
  }

  private static String getToElementId(Object connector) {
    try {
      java.lang.reflect.Method getToMethod = connector.getClass().getMethod("getTo");
      Object toElement = getToMethod.invoke(connector);

      if (toElement instanceof IUseCaseUIModel useCaseUI) {
        IModelElement model = useCaseUI.getModelElement();
        return model != null ? model.getId() : null;
      } else if (toElement instanceof IActorUIModel actorUI) {
        IModelElement model = actorUI.getModelElement();
        return model != null ? model.getId() : null;
      }
    } catch (Exception e) {
      try {
        java.lang.reflect.Method getToShapeMethod = connector.getClass().getMethod("getToShape");
        Object toShape = getToShapeMethod.invoke(connector);
        return extractElementId(toShape);
      } catch (Exception e2) {
        System.err.println("Could not get to element: " + e2.getMessage());
      }
    }
    return null;
  }

  private static String extractElementId(Object shape) {
    if (shape instanceof IUseCaseUIModel useCaseUI) {
      IModelElement model = useCaseUI.getModelElement();
      return model != null ? model.getId() : null;
    } else if (shape instanceof IActorUIModel actorUI) {
      IModelElement model = actorUI.getModelElement();
      return model != null ? model.getId() : null;
    }
    return null;
  }

  private static void applyRelationshipsToNeeds(
      NeedsFile.VersionData versionData,
      Map<String, Set<String>> includeRelationships,
      Map<String, Set<String>> extendRelationships,
      Map<String, Set<String>> associateRelationships,
      Map<String, String> vpIdToUserId) {

    // Include relationships: populate "includes" field
    for (Map.Entry<String, Set<String>> entry : includeRelationships.entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        List<String> includeLinks = new ArrayList<>();
        if (fromNeed.getIncludesLinks() != null) {
          includeLinks.addAll(fromNeed.getIncludesLinks());
        }

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            includeLinks.add(toUserId);
          }
        }

        fromNeed.setIncludesLinks(includeLinks);
      }
    }

    // Extend relationships: populate "extends" field
    for (Map.Entry<String, Set<String>> entry : extendRelationships.entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        List<String> extendLinks = new ArrayList<>();
        if (fromNeed.getExtendsLinks() != null) {
          extendLinks.addAll(fromNeed.getExtendsLinks());
        }

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            extendLinks.add(toUserId);
          }
        }

        fromNeed.setExtendsLinks(extendLinks);
      }
    }

    // Associate relationships: populate "associates" field
    for (Map.Entry<String, Set<String>> entry : associateRelationships.entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        List<String> associateLinks = new ArrayList<>();
        if (fromNeed.getAssociatesLinks() != null) {
          associateLinks.addAll(fromNeed.getAssociatesLinks());
        }

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            associateLinks.add(toUserId);
          }
        }

        fromNeed.setAssociatesLinks(associateLinks);
      }
    }
  }

  private static String getDescription(IModelElement element) {
    try {
      String description = element.getDescription();
      return (description != null && !description.trim().isEmpty()) ? description : "";
    } catch (Exception e) {
      return "";
    }
  }

  private static String getUserId(IModelElement element) {
    try {
      // Try to get the User ID field from Visual Paradigm
      java.lang.reflect.Method getUserIdMethod = element.getClass().getMethod("getUserID");
      Object result = getUserIdMethod.invoke(element);
      if (result != null) {
        return result.toString();
      }
    } catch (Exception e) {
      // Method doesn't exist or failed, try alternative method names
      try {
        java.lang.reflect.Method getUserIdMethod = element.getClass().getMethod("getUserId");
        Object result = getUserIdMethod.invoke(element);
        if (result != null) {
          return result.toString();
        }
      } catch (Exception e2) {
        // User ID method not available
      }
    }
    return null;
  }

  private static String getRank(IModelElement element) {
    try {
      // Try getUcRank() method for use cases
      java.lang.reflect.Method getUcRankMethod = element.getClass().getMethod("getUcRank");
      Object result = getUcRankMethod.invoke(element);
      if (result != null) {
        int rank = (Integer) result;
        // Convert VP rank constants to meaningful values
        return switch (rank) {
          case 1 -> "high"; // UC_RANK_HIGH
          case 2 -> "medium"; // UC_RANK_MEDIUM
          case 3 -> "low"; // UC_RANK_LOW
          default -> ""; // UC_RANK_UNSPECIFIED
        };
      }
      // If result is null, fall through to try next method
    } catch (Exception e) {
      // getUcRank method doesn't exist or failed
    }

    try {
      // Try getPmPriority() method as fallback
      java.lang.reflect.Method getPmPriorityMethod = element.getClass().getMethod("getPmPriority");
      Object result = getPmPriorityMethod.invoke(element);
      if (result != null) {
        return result.toString();
      }
      // If result is null, fall through to final fallback
    } catch (Exception e) {
      // getPmPriority method doesn't exist or failed
    }

    // Log warning for missing priority information but return empty string for schema compliance
    System.err.println("Warning: No priority information found for element " + element.getName());
    return ""; // No rank/priority information found - return empty for schema compliance
  }

  private static String getStatus(IModelElement element) {
    try {
      // Try to get status from use case
      java.lang.reflect.Method getStatusMethod = element.getClass().getMethod("getStatus");
      Object result = getStatusMethod.invoke(element);
      if (result != null) {
        int status = (Integer) result;
        // Convert VP status constants to meaningful values
        return switch (status) {
          case 0 -> "identify"; // STATUS_IDENTIFY
          case 1 -> "discuss"; // STATUS_DISCUSS
          case 2 -> "elaborate"; // STATUS_ELABORATE
          case 3 -> "design"; // STATUS_DESIGN
          case 4 -> "consent"; // STATUS_CONSENT
          case 5 -> "develop"; // STATUS_DEVELOP
          case 6 -> "complete"; // STATUS_COMPLETE
          default -> "identify"; // Default to identify
        };
      }
    } catch (Exception e) {
      // getStatus method doesn't exist or failed
    }

    return "identify"; // Default status if no status information found
  }

  private static String sanitizeName(String input) {
    if (input == null || input.trim().isEmpty()) {
      return "Untitled Diagram";
    }
    return input.trim();
  }

  private static String sanitizeId(String input) {
    if (input == null) return "UNKNOWN";

    return input
        .toUpperCase()
        .replaceAll("[^A-Z0-9_]", "_")
        .replaceAll("_{2,}", "_")
        .replaceAll("^_+|_+$", "");
  }

  private static String sanitizeTag(String input) {
    if (input == null) return "unknown";

    return input
        .toLowerCase()
        .replaceAll("[^a-z0-9_-]", "_")
        .replaceAll("_{2,}", "_")
        .replaceAll("^_+|_+$", "");
  }

  /** Extract all use cases and actors from the entire project. */
  private static void extractAllProjectUseCasesAndActors(
      NeedsFile.VersionData versionData, Map<String, String> vpIdToUserId) {
    try {
      ApplicationManager appManager = ApplicationManager.instance();
      if (appManager == null) {
        System.err.println("ApplicationManager not available (test environment)");
        return;
      }

      ProjectManager projectManager = appManager.getProjectManager();
      if (projectManager == null) {
        System.err.println("ProjectManager not available");
        return;
      }

      IProject project = projectManager.getProject();
      if (project == null) {
        System.err.println("No project found, cannot extract use cases");
        return;
      }

      // Extract all use cases from project
      Iterator<IModelElement> allModels = project.allLevelModelElementIterator();
      while (allModels.hasNext()) {
        IModelElement element = allModels.next();
        if (element instanceof IUseCase useCase) {
          processUseCase(useCase, versionData, vpIdToUserId);
        } else if (element instanceof IActor actor) {
          processActor(actor, versionData, vpIdToUserId);
        }
      }

      // Also search through all diagrams for additional models
      Iterator<IDiagramUIModel> diagrams = project.diagramIterator();
      while (diagrams.hasNext()) {
        IDiagramUIModel diagram = diagrams.next();
        IDiagramElement[] diagramElements = diagram.toDiagramElementArray();

        for (IDiagramElement diagramElement : diagramElements) {
          IModelElement modelElement = diagramElement.getModelElement();
          if (modelElement != null) {
            String modelId = modelElement.getId();

            // Only process if we haven't already processed this model
            if (!vpIdToUserId.containsKey(modelId)) {
              if (modelElement instanceof IUseCase useCase) {
                processUseCase(useCase, versionData, vpIdToUserId);
              } else if (modelElement instanceof IActor actor) {
                processActor(actor, versionData, vpIdToUserId);
              }
            }
          }
        }
      }

      System.out.println(
          "Extracted " + versionData.getNeeds().size() + " total elements from project");
    } catch (Exception e) {
      System.err.println("Error extracting project use cases and actors: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Extract relationships from the entire project. */
  private static void extractAllProjectRelationships(
      NeedsFile.VersionData versionData, Map<String, String> vpIdToUserId) {
    try {
      ApplicationManager appManager = ApplicationManager.instance();
      if (appManager == null) {
        System.err.println("ApplicationManager not available (test environment)");
        return;
      }

      ProjectManager projectManager = appManager.getProjectManager();
      if (projectManager == null) {
        System.err.println("ProjectManager not available");
        return;
      }

      IProject project = projectManager.getProject();
      if (project == null) {
        System.err.println("No project found, cannot extract relationships");
        return;
      }

      // Search through all diagrams for relationships
      Iterator<IDiagramUIModel> diagrams = project.diagramIterator();
      while (diagrams.hasNext()) {
        IDiagramUIModel diagram = diagrams.next();

        // Extract relationships from this diagram
        extractRelationships(diagram, versionData, vpIdToUserId);
      }

      System.out.println("Extracted relationships from all diagrams in project");
    } catch (Exception e) {
      System.err.println("Error extracting project relationships: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Process a use case model element and add it to the needs data. */
  private static void processUseCase(
      IUseCase useCase, NeedsFile.VersionData versionData, Map<String, String> vpIdToUserId) {
    try {
      String name = useCase.getName();
      if (name == null || name.trim().isEmpty()) {
        name = "Unnamed Use Case";
      }

      // Use Visual Paradigm's User ID field
      String id = getUserId(useCase);

      // Handle null User ID by skipping this use case if no User ID is set
      if (id == null || id.trim().isEmpty()) {
        System.err.println("Warning: Skipping use case '" + name + "' - no User ID set");
        return; // Skip use cases without User ID
      }

      // Track VP internal ID to User ID mapping for relationship resolution
      vpIdToUserId.put(useCase.getId(), id);

      // Avoid duplicates by checking for existing ID or appending number
      String finalId = id;
      int counter = 1;
      while (versionData.getNeeds().containsKey(finalId)) {
        finalId = id + "_" + counter;
        counter++;
      }

      NeedsFile.Need need = new NeedsFile.Need(finalId, name, "uc");
      need.setContent(getDescription(useCase));
      need.setStatus(getStatus(useCase));
      need.setElementType("UseCase");
      need.setPriority(getRank(useCase));
      need.setVpModelId(useCase.getId()); // Store Visual Paradigm model ID

      // Set tags
      List<String> tags = new ArrayList<>();
      tags.add("usecase");
      tags.add("functional");
      need.setTags(tags);

      versionData.addNeed(need);
      System.out.println("Processed use case: " + finalId + " - " + name);
    } catch (Exception e) {
      System.err.println("Error processing use case " + useCase.getName() + ": " + e.getMessage());
    }
  }
}
