package com.orgatex.vp.sphinx.extractor;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.connector.IAssociationUIModel;
import com.vp.plugin.diagram.connector.IExtendUIModel;
import com.vp.plugin.diagram.connector.IIncludeUIModel;
import com.vp.plugin.diagram.connector.IRQRefineUIModel;
import com.vp.plugin.diagram.connector.IRQTraceUIModel;
import com.vp.plugin.diagram.connector.IRequirementDeriveUIModel;
import com.vp.plugin.diagram.shape.IActorUIModel;
import com.vp.plugin.diagram.shape.IRequirementUIModel;
import com.vp.plugin.diagram.shape.IUseCaseUIModel;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IProject;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Extracts relationships between model elements from Visual Paradigm diagrams. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NeedsRelationshipExtractor {

  /** Extract all relationships from the current VP project's diagrams. */
  public static RelationshipMaps extractAllRelationships() {
    try {
      ApplicationManager appManager = ApplicationManager.instance();
      if (appManager == null) {
        System.err.println("ApplicationManager not available (test environment)");
        return new RelationshipMaps();
      }

      ProjectManager projectManager = appManager.getProjectManager();
      if (projectManager == null) {
        System.err.println("ProjectManager not available");
        return new RelationshipMaps();
      }

      IProject project = projectManager.getProject();
      if (project == null) {
        System.err.println("No project found, cannot extract relationships");
        return new RelationshipMaps();
      }

      // Maps to collect relationships from ALL diagrams (but deduplicate)
      Map<String, Set<String>> allIncludeRelationships = new HashMap<>();
      Map<String, Set<String>> allExtendRelationships = new HashMap<>();
      Map<String, Set<String>> allAssociateRelationships = new HashMap<>();
      Map<String, Set<String>> allContainsRelationships = new HashMap<>();
      Map<String, Set<String>> allDeriveRelationships = new HashMap<>();
      Map<String, Set<String>> allRefinesRelationships = new HashMap<>();

      // Search through all diagrams for relationships
      @SuppressWarnings("unchecked")
      Iterator<IDiagramUIModel> diagrams = project.diagramIterator();
      int diagramCount = 0;
      while (diagrams.hasNext()) {
        IDiagramUIModel diagram = diagrams.next();
        diagramCount++;

        // Extract relationships from this diagram and add to global maps
        extractRelationshipsFromDiagram(
            diagram,
            allIncludeRelationships,
            allExtendRelationships,
            allAssociateRelationships,
            allContainsRelationships,
            allDeriveRelationships,
            allRefinesRelationships);
      }

      // Print summary of extracted relationships
      System.out.println("Relationship extraction summary:");
      System.out.println("  Include: " + allIncludeRelationships.size());
      System.out.println("  Extend: " + allExtendRelationships.size());
      System.out.println("  Associate: " + allAssociateRelationships.size());
      System.out.println("  Contains: " + allContainsRelationships.size());
      System.out.println("  Derive: " + allDeriveRelationships.size());
      System.out.println("  Refines: " + allRefinesRelationships.size());

      System.out.println("Extracted relationships from all diagrams in project");
      return new RelationshipMaps(
          allIncludeRelationships,
          allExtendRelationships,
          allAssociateRelationships,
          allContainsRelationships,
          allDeriveRelationships,
          allRefinesRelationships);

    } catch (Exception e) {
      System.err.println("Error extracting project relationships: " + e.getMessage());
      e.printStackTrace();
      return new RelationshipMaps();
    }
  }

  /** Extract relationships from a single diagram and add to global relationship maps. */
  private static void extractRelationshipsFromDiagram(
      IDiagramUIModel diagram,
      Map<String, Set<String>> allIncludeRelationships,
      Map<String, Set<String>> allExtendRelationships,
      Map<String, Set<String>> allAssociateRelationships,
      Map<String, Set<String>> allContainsRelationships,
      Map<String, Set<String>> allDeriveRelationships,
      Map<String, Set<String>> allRefinesRelationships) {
    try {
      // Get all diagram elements to find connectors
      IDiagramElement[] diagramElements = diagram.toDiagramElementArray();

      for (IDiagramElement element : diagramElements) {
        if (element instanceof IIncludeUIModel includeUI) {
          processIncludeRelationship(includeUI, allIncludeRelationships);
        } else if (element instanceof IExtendUIModel extendUI) {
          processExtendRelationship(extendUI, allExtendRelationships);
        } else if (element instanceof IAssociationUIModel associateUI) {
          processAssociateRelationship(associateUI, allAssociateRelationships);
        } else if (element instanceof IRequirementDeriveUIModel deriveUI) {
          processRequirementDeriveRelationship(deriveUI, allDeriveRelationships);
        } else if (element instanceof IRQRefineUIModel refineUI) {
          processRequirementRefineRelationship(refineUI, allRefinesRelationships);
        } else if (element instanceof IRQTraceUIModel traceUI) {
          processRequirementTraceRelationship(traceUI, allContainsRelationships);
        }
      }
    } catch (Exception e) {
      System.err.println("Error extracting relationships from diagram: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Process include relationship and add to global map. */
  private static void processIncludeRelationship(
      IIncludeUIModel includeUI, Map<String, Set<String>> includeRelationships) {
    try {
      String fromId = getFromElementId(includeUI);
      String toId = getToElementId(includeUI);

      if (fromId != null && toId != null) {
        includeRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
      }
    } catch (Exception e) {
      System.err.println("Error processing include relationship: " + e.getMessage());
    }
  }

  /** Process extend relationship and add to global map. */
  private static void processExtendRelationship(
      IExtendUIModel extendUI, Map<String, Set<String>> extendRelationships) {
    try {
      String fromId = getFromElementId(extendUI);
      String toId = getToElementId(extendUI);

      if (fromId != null && toId != null) {
        // In VP extend relationships, the arrow direction is: extending → base
        // Semantically: "extending" extends "base"
        // So base goes into extending's "extends" list: extending.extends = [base]
        // Therefore: toId goes into fromId's extends list
        extendRelationships.computeIfAbsent(toId, k -> new HashSet<>()).add(fromId);
      }
    } catch (Exception e) {
      System.err.println("Error processing extend relationship: " + e.getMessage());
    }
  }

  /** Process association relationship and add to global map. */
  private static void processAssociateRelationship(
      IAssociationUIModel associateUI, Map<String, Set<String>> associateRelationships) {
    try {
      String fromId = getFromElementId(associateUI);
      String toId = getToElementId(associateUI);

      if (fromId != null && toId != null) {
        // For associations, we can create bidirectional links
        associateRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
        associateRelationships.computeIfAbsent(toId, k -> new HashSet<>()).add(fromId);
      }
    } catch (Exception e) {
      System.err.println("Error processing associate relationship: " + e.getMessage());
    }
  }

  /** Get element ID from connector's from side. */
  private static String getFromElementId(Object connector) {
    try {
      java.lang.reflect.Method getFromMethod = connector.getClass().getMethod("getFrom");
      Object fromElement = getFromMethod.invoke(connector);

      if (fromElement instanceof IUseCaseUIModel useCaseUI) {
        IModelElement model = useCaseUI.getModelElement();
        return model != null ? model.getId() : null;
      } else if (fromElement instanceof IActorUIModel actorUI) {
        IModelElement model = actorUI.getModelElement();
        return model != null ? model.getId() : null;
      } else if (fromElement instanceof IRequirementUIModel requirementUI) {
        IModelElement model = requirementUI.getModelElement();
        return model != null ? model.getId() : null;
      }
    } catch (Exception e) {
      try {
        java.lang.reflect.Method getFromShapeMethod =
            connector.getClass().getMethod("getFromShape");
        Object fromShape = getFromShapeMethod.invoke(connector);
        return extractElementId(fromShape);
      } catch (Exception e2) {
        // Could not get from element
      }
    }
    return null;
  }

  /** Get element ID from connector's to side. */
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
      } else if (toElement instanceof IRequirementUIModel requirementUI) {
        IModelElement model = requirementUI.getModelElement();
        return model != null ? model.getId() : null;
      }
    } catch (Exception e) {
      try {
        java.lang.reflect.Method getToShapeMethod = connector.getClass().getMethod("getToShape");
        Object toShape = getToShapeMethod.invoke(connector);
        return extractElementId(toShape);
      } catch (Exception e2) {
        // Could not get to element
      }
    }
    return null;
  }

  /** Extract element ID from diagram shape. */
  private static String extractElementId(Object shape) {
    if (shape instanceof IUseCaseUIModel useCaseUI) {
      IModelElement model = useCaseUI.getModelElement();
      return model != null ? model.getId() : null;
    } else if (shape instanceof IActorUIModel actorUI) {
      IModelElement model = actorUI.getModelElement();
      return model != null ? model.getId() : null;
    } else if (shape instanceof IRequirementUIModel requirementUI) {
      IModelElement model = requirementUI.getModelElement();
      return model != null ? model.getId() : null;
    }
    return null;
  }

  /** Process requirement derive relationship and add to global map. */
  private static void processRequirementDeriveRelationship(
      IRequirementDeriveUIModel deriveUI, Map<String, Set<String>> deriveRelationships) {
    try {
      String fromId = getFromElementId(deriveUI);
      String toId = getToElementId(deriveUI);

      if (fromId != null && toId != null) {
        deriveRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
      }
    } catch (Exception e) {
      System.err.println("Error processing derive relationship: " + e.getMessage());
    }
  }

  /** Process requirement refine relationship and add to global map. */
  private static void processRequirementRefineRelationship(
      IRQRefineUIModel refineUI, Map<String, Set<String>> refinesRelationships) {
    try {
      String fromId = getFromElementId(refineUI);
      String toId = getToElementId(refineUI);

      if (fromId != null && toId != null) {
        refinesRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
      }
    } catch (Exception e) {
      System.err.println("Error processing refine relationship: " + e.getMessage());
    }
  }

  /** Process requirement trace relationship and add to global map (mapping to contains). */
  private static void processRequirementTraceRelationship(
      IRQTraceUIModel traceUI, Map<String, Set<String>> containsRelationships) {
    try {
      String fromId = getFromElementId(traceUI);
      String toId = getToElementId(traceUI);

      if (fromId != null && toId != null) {
        containsRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
      }
    } catch (Exception e) {
      System.err.println("Error processing trace relationship: " + e.getMessage());
    }
  }

  /** Container for relationship maps extracted from diagrams. */
  public static class RelationshipMaps {
    private final Map<String, Set<String>> includeRelationships;
    private final Map<String, Set<String>> extendRelationships;
    private final Map<String, Set<String>> associateRelationships;
    private final Map<String, Set<String>> containsRelationships;
    private final Map<String, Set<String>> deriveRelationships;
    private final Map<String, Set<String>> refinesRelationships;

    public RelationshipMaps() {
      this.includeRelationships = new HashMap<>();
      this.extendRelationships = new HashMap<>();
      this.associateRelationships = new HashMap<>();
      this.containsRelationships = new HashMap<>();
      this.deriveRelationships = new HashMap<>();
      this.refinesRelationships = new HashMap<>();
    }

    public RelationshipMaps(
        Map<String, Set<String>> includeRelationships,
        Map<String, Set<String>> extendRelationships,
        Map<String, Set<String>> associateRelationships,
        Map<String, Set<String>> containsRelationships,
        Map<String, Set<String>> deriveRelationships,
        Map<String, Set<String>> refinesRelationships) {
      this.includeRelationships = includeRelationships;
      this.extendRelationships = extendRelationships;
      this.associateRelationships = associateRelationships;
      this.containsRelationships = containsRelationships;
      this.deriveRelationships = deriveRelationships;
      this.refinesRelationships = refinesRelationships;
    }

    public Map<String, Set<String>> getIncludeRelationships() {
      return includeRelationships;
    }

    public Map<String, Set<String>> getExtendRelationships() {
      return extendRelationships;
    }

    public Map<String, Set<String>> getAssociateRelationships() {
      return associateRelationships;
    }

    public Map<String, Set<String>> getContainsRelationships() {
      return containsRelationships;
    }

    public Map<String, Set<String>> getDeriveRelationships() {
      return deriveRelationships;
    }

    public Map<String, Set<String>> getRefinesRelationships() {
      return refinesRelationships;
    }
  }
}
