package com.orgatex.vp.sphinx.extractor;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IProject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Extractor for requirements diagrams with support for derive, contains, and refines relationships.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequirementsDiagramExtractor {

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

    // Extract all requirements from the entire project
    extractAllProjectRequirements(versionData, vpIdToUserId);

    // Extract relationships from the entire project
    extractAllProjectRelationships(versionData, vpIdToUserId);

    needsFile.addVersion("1.0", versionData);
    return needsFile;
  }

  /** Extract all requirements from the entire project. */
  private static void extractAllProjectRequirements(
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
        System.err.println("No project found, cannot extract requirements");
        return;
      }

      // Extract all requirements from project using Visual Paradigm API
      @SuppressWarnings("unchecked")
      Iterator<IModelElement> allModels = project.allLevelModelElementIterator();
      while (allModels.hasNext()) {
        IModelElement element = allModels.next();

        // Check if element is a requirement
        if (isRequirement(element)) {
          processRequirement(element, versionData, vpIdToUserId);
        }
        // Also check for use cases to support refines relationships
        else if (isUseCase(element)) {
          processUseCase(element, versionData, vpIdToUserId);
        }
      }

      System.out.println(
          "Extracted " + versionData.getNeeds().size() + " total elements from project");
    } catch (Exception e) {
      System.err.println("Error extracting project requirements: " + e.getMessage());
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

      // Maps to track relationships
      Map<String, Set<String>> deriveRelationships = new HashMap<>();
      Map<String, Set<String>> containsRelationships = new HashMap<>();
      Map<String, Set<String>> refinesRelationships = new HashMap<>();

      // Search through all diagrams for relationships
      @SuppressWarnings("unchecked")
      Iterator<IDiagramUIModel> diagrams = project.diagramIterator();
      while (diagrams.hasNext()) {
        IDiagramUIModel diagram = diagrams.next();

        // Only process requirements diagrams
        if (isRequirementsDiagram(diagram)) {
          extractRelationships(
              diagram, deriveRelationships, containsRelationships, refinesRelationships);
        }
      }

      // Apply relationships to needs
      applyRelationshipsToNeeds(
          versionData,
          deriveRelationships,
          containsRelationships,
          refinesRelationships,
          vpIdToUserId);

      System.out.println("Extracted relationships from all requirements diagrams in project");
    } catch (Exception e) {
      System.err.println("Error extracting project relationships: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Extract relationships from a specific diagram. */
  private static void extractRelationships(
      IDiagramUIModel diagram,
      Map<String, Set<String>> deriveRelationships,
      Map<String, Set<String>> containsRelationships,
      Map<String, Set<String>> refinesRelationships) {
    try {
      IDiagramElement[] diagramElements = diagram.toDiagramElementArray();

      for (IDiagramElement element : diagramElements) {
        // Process different types of requirement relationships
        processRelationshipElement(
            element, deriveRelationships, containsRelationships, refinesRelationships);
      }
    } catch (Exception e) {
      System.err.println("Error extracting relationships from diagram: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Process a requirement model element and add it to the needs data. */
  private static void processRequirement(
      IModelElement requirement,
      NeedsFile.VersionData versionData,
      Map<String, String> vpIdToUserId) {
    try {
      String name = requirement.getName();
      if (name == null || name.trim().isEmpty()) {
        name = "Unnamed Requirement";
      }

      // Use Visual Paradigm's User ID field
      String id = getUserId(requirement);

      // Handle null User ID by skipping this requirement if no User ID is set
      if (id == null || id.trim().isEmpty()) {
        System.err.println("Warning: Skipping requirement '" + name + "' - no User ID set");
        return; // Skip requirements without User ID
      }

      // Track VP internal ID to User ID mapping for relationship resolution
      vpIdToUserId.put(requirement.getId(), id);

      // Avoid duplicates by checking for existing ID or appending number
      String finalId = id;
      int counter = 1;
      while (versionData.getNeeds().containsKey(finalId)) {
        finalId = id + "_" + counter;
        counter++;
      }

      NeedsFile.Need need = new NeedsFile.Need(finalId, name, "req");
      need.setContent(getDescription(requirement));
      need.setStatus(getStatus(requirement));
      need.setElementType("Requirement");
      need.setPriority(getPriority(requirement));
      need.setVpModelId(requirement.getId()); // Store Visual Paradigm model ID

      // Set tags
      List<String> tags = new ArrayList<>();
      tags.add("requirement");
      tags.add("functional"); // Default - could be enhanced with actual requirement classification
      need.setTags(tags);

      versionData.addNeed(need);
      System.out.println("Processed requirement: " + finalId + " - " + name);
    } catch (Exception e) {
      System.err.println(
          "Error processing requirement " + requirement.getName() + ": " + e.getMessage());
    }
  }

  /** Process a use case model element (to support refines relationships). */
  private static void processUseCase(
      IModelElement useCase, NeedsFile.VersionData versionData, Map<String, String> vpIdToUserId) {
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
      need.setPriority(getPriority(useCase));
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

  /** Apply relationships to needs. */
  private static void applyRelationshipsToNeeds(
      NeedsFile.VersionData versionData,
      Map<String, Set<String>> deriveRelationships,
      Map<String, Set<String>> containsRelationships,
      Map<String, Set<String>> refinesRelationships,
      Map<String, String> vpIdToUserId) {

    // Derive relationships: populate "derive" field
    for (Map.Entry<String, Set<String>> entry : deriveRelationships.entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        List<String> deriveLinks = new ArrayList<>();
        if (fromNeed.getDeriveLinks() != null) {
          deriveLinks.addAll(fromNeed.getDeriveLinks());
        }

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            deriveLinks.add(toUserId);
          }
        }

        fromNeed.setDeriveLinks(deriveLinks);
      }
    }

    // Contains relationships: populate "contains" field
    for (Map.Entry<String, Set<String>> entry : containsRelationships.entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        List<String> containsLinks = new ArrayList<>();
        if (fromNeed.getContainsLinks() != null) {
          containsLinks.addAll(fromNeed.getContainsLinks());
        }

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            containsLinks.add(toUserId);
          }
        }

        fromNeed.setContainsLinks(containsLinks);
      }
    }

    // Refines relationships: populate "refines" field
    for (Map.Entry<String, Set<String>> entry : refinesRelationships.entrySet()) {
      String fromUserId = vpIdToUserId.get(entry.getKey());
      if (fromUserId != null && versionData.getNeeds().containsKey(fromUserId)) {
        NeedsFile.Need fromNeed = versionData.getNeeds().get(fromUserId);
        List<String> refinesLinks = new ArrayList<>();
        if (fromNeed.getRefinesLinks() != null) {
          refinesLinks.addAll(fromNeed.getRefinesLinks());
        }

        for (String toVpId : entry.getValue()) {
          String toUserId = vpIdToUserId.get(toVpId);
          if (toUserId != null && versionData.getNeeds().containsKey(toUserId)) {
            refinesLinks.add(toUserId);
          }
        }

        fromNeed.setRefinesLinks(refinesLinks);
      }
    }
  }

  /** Check if element is a requirement. */
  private static boolean isRequirement(IModelElement element) {
    if (element == null) return false;

    // Use reflection to check element type since exact class names may vary
    String className = element.getClass().getSimpleName();
    return className.contains("Requirement") || className.contains("IRequirement");
  }

  /** Check if element is a use case. */
  private static boolean isUseCase(IModelElement element) {
    if (element == null) return false;

    String className = element.getClass().getSimpleName();
    return className.contains("UseCase") || className.contains("IUseCase");
  }

  /** Check if diagram is a requirements diagram. */
  private static boolean isRequirementsDiagram(IDiagramUIModel diagram) {
    if (diagram == null) return false;

    try {
      // Check diagram type using reflection since exact method names may vary
      java.lang.reflect.Method getTypeMethod = diagram.getClass().getMethod("getType");
      Object diagramType = getTypeMethod.invoke(diagram);

      String typeName = diagramType.toString().toLowerCase();
      return typeName.contains("requirement") || typeName.contains("req");
    } catch (Exception e) {
      // Fallback: check diagram name
      String diagramName = diagram.getName();
      if (diagramName != null) {
        String name = diagramName.toLowerCase();
        return name.contains("requirement") || name.contains("req");
      }
    }

    return false;
  }

  /** Process relationship element from diagram. */
  private static void processRelationshipElement(
      IDiagramElement element,
      Map<String, Set<String>> deriveRelationships,
      Map<String, Set<String>> containsRelationships,
      Map<String, Set<String>> refinesRelationships) {

    // Use reflection to identify relationship types since exact class names may vary
    String className = element.getClass().getSimpleName();

    try {
      if (className.contains("Derive") || className.contains("Derivation")) {
        processDerivationRelationship(element, deriveRelationships);
      } else if (className.contains("Contains") || className.contains("Containment")) {
        processContainsRelationship(element, containsRelationships);
      } else if (className.contains("Refine") || className.contains("Refinement")) {
        processRefinementRelationship(element, refinesRelationships);
      }
    } catch (Exception e) {
      System.err.println("Error processing relationship element: " + e.getMessage());
    }
  }

  /** Process derivation relationship. */
  private static void processDerivationRelationship(
      IDiagramElement element, Map<String, Set<String>> deriveRelationships) {
    try {
      String fromId = getFromElementId(element);
      String toId = getToElementId(element);

      if (fromId != null && toId != null) {
        deriveRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
      }
    } catch (Exception e) {
      System.err.println("Error processing derivation relationship: " + e.getMessage());
    }
  }

  /** Process contains relationship. */
  private static void processContainsRelationship(
      IDiagramElement element, Map<String, Set<String>> containsRelationships) {
    try {
      String fromId = getFromElementId(element);
      String toId = getToElementId(element);

      if (fromId != null && toId != null) {
        containsRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
      }
    } catch (Exception e) {
      System.err.println("Error processing contains relationship: " + e.getMessage());
    }
  }

  /** Process refinement relationship. */
  private static void processRefinementRelationship(
      IDiagramElement element, Map<String, Set<String>> refinesRelationships) {
    try {
      String fromId = getFromElementId(element);
      String toId = getToElementId(element);

      if (fromId != null && toId != null) {
        refinesRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
      }
    } catch (Exception e) {
      System.err.println("Error processing refinement relationship: " + e.getMessage());
    }
  }

  /** Get from element ID from connector. */
  private static String getFromElementId(IDiagramElement connector) {
    try {
      java.lang.reflect.Method getFromMethod = connector.getClass().getMethod("getFrom");
      Object fromElement = getFromMethod.invoke(connector);
      return extractElementId(fromElement);
    } catch (Exception e) {
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

  /** Get to element ID from connector. */
  private static String getToElementId(IDiagramElement connector) {
    try {
      java.lang.reflect.Method getToMethod = connector.getClass().getMethod("getTo");
      Object toElement = getToMethod.invoke(connector);
      return extractElementId(toElement);
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

  /** Extract element ID from shape or model element. */
  private static String extractElementId(Object element) {
    if (element == null) return null;

    try {
      // Try to get model element if this is a UI element
      java.lang.reflect.Method getModelElementMethod =
          element.getClass().getMethod("getModelElement");
      Object modelElement = getModelElementMethod.invoke(element);
      if (modelElement != null) {
        java.lang.reflect.Method getIdMethod = modelElement.getClass().getMethod("getId");
        return (String) getIdMethod.invoke(modelElement);
      }
    } catch (Exception e) {
      // Not a UI element, try direct ID access
      try {
        java.lang.reflect.Method getIdMethod = element.getClass().getMethod("getId");
        return (String) getIdMethod.invoke(element);
      } catch (Exception e2) {
        System.err.println("Could not extract element ID: " + e2.getMessage());
      }
    }

    return null;
  }

  /** Get description from model element. */
  private static String getDescription(IModelElement element) {
    try {
      String description = element.getDescription();
      return (description != null && !description.trim().isEmpty()) ? description : "";
    } catch (Exception e) {
      return "";
    }
  }

  /** Get User ID from model element. */
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

  /** Get priority from model element. */
  private static String getPriority(IModelElement element) {
    try {
      // Try requirement-specific priority methods
      java.lang.reflect.Method getPriorityMethod = element.getClass().getMethod("getPriority");
      Object result = getPriorityMethod.invoke(element);
      if (result != null) {
        return result.toString();
      }
    } catch (Exception e) {
      // Try alternative priority method names for requirements
      try {
        java.lang.reflect.Method getReqPriorityMethod =
            element.getClass().getMethod("getReqPriority");
        Object result = getReqPriorityMethod.invoke(element);
        if (result != null) {
          int priority = (Integer) result;
          // Convert VP priority constants to meaningful values
          return switch (priority) {
            case 1 -> "critical";
            case 2 -> "high";
            case 3 -> "medium";
            case 4 -> "low";
            default -> "";
          };
        }
      } catch (Exception e2) {
        // No priority information found
      }
    }
    return ""; // Default empty priority
  }

  /** Get status from model element. */
  private static String getStatus(IModelElement element) {
    try {
      // Try to get status from requirement
      java.lang.reflect.Method getStatusMethod = element.getClass().getMethod("getStatus");
      Object result = getStatusMethod.invoke(element);
      if (result != null) {
        return result.toString();
      }
    } catch (Exception e) {
      // getStatus method doesn't exist or failed
    }

    return "open"; // Default status for requirements
  }

  /** Sanitize name for project title. */
  private static String sanitizeName(String input) {
    if (input == null || input.trim().isEmpty()) {
      return "Untitled Requirements";
    }
    return input.trim();
  }
}
