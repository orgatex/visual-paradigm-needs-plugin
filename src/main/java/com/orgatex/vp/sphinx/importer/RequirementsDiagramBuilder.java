package com.orgatex.vp.sphinx.importer;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.DiagramManager;
import com.vp.plugin.diagram.IDiagramTypeConstants;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.model.*;
import com.vp.plugin.model.factory.IModelElementFactory;
import java.util.*;

/** Builder for creating requirements diagrams from sphinx-needs data. */
public class RequirementsDiagramBuilder {

  private final ModelLookup modelLookup;
  private final ElementLayoutEngine layoutEngine;

  public RequirementsDiagramBuilder() {
    this.modelLookup = new ModelLookup();
    this.layoutEngine = new ElementLayoutEngine();
  }

  /** Create a new requirements diagram. */
  public IDiagramUIModel createRequirementsDiagram(String diagramName) throws ImportException {
    try {
      DiagramManager diagramManager = ApplicationManager.instance().getDiagramManager();
      if (diagramManager == null) {
        throw new ImportException("DiagramManager not available");
      }

      // Try to create requirements diagram, fallback to use case diagram if not available
      IDiagramUIModel diagram = null;

      try {
        // Try requirements diagram type using reflection
        java.lang.reflect.Field reqDiagramField =
            IModelElementFactory.class.getField("MODEL_TYPE_REQUIREMENTS_DIAGRAM");
        Object reqDiagramType = reqDiagramField.get(null);
        diagram = diagramManager.createDiagram((String) reqDiagramType);
      } catch (Exception e) {
        // Fallback to use case diagram
        System.err.println("Requirements diagram type not available, using use case diagram");
        diagram = diagramManager.createDiagram(IDiagramTypeConstants.DIAGRAM_TYPE_USE_CASE_DIAGRAM);
      }

      if (diagram == null) {
        throw new ImportException("Failed to create diagram");
      }

      diagram.setName(diagramName);
      return diagram;
    } catch (Exception e) {
      throw new ImportException("Failed to create requirements diagram: " + e.getMessage(), e);
    }
  }

  /** Create requirement elements in the diagram. */
  public void createRequirementElements(IDiagramUIModel diagram, Map<String, NeedsFile.Need> needs)
      throws ImportException {
    try {
      System.out.println("Creating requirement elements in diagram: " + diagram.getName());

      // Create all requirement and use case elements
      Map<String, Object> createdElements = new HashMap<>();

      for (Map.Entry<String, NeedsFile.Need> entry : needs.entrySet()) {
        NeedsFile.Need need = entry.getValue();

        if ("req".equals(need.getType())) {
          Object requirement = createRequirement(need, diagram);
          if (requirement != null) {
            createdElements.put(need.getId(), requirement);
          }
        } else if ("uc".equals(need.getType())) {
          Object useCase = createUseCase(need, diagram);
          if (useCase != null) {
            createdElements.put(need.getId(), useCase);
          }
        }
      }

      // Layout elements (placeholder - VP-specific implementation needed)
      // layoutEngine.layoutElements(diagram, createdElements.values());

      System.out.println("Successfully created " + createdElements.size() + " elements");
    } catch (Exception e) {
      throw new ImportException("Failed to create requirement elements: " + e.getMessage(), e);
    }
  }

  /** Create relationships between elements. */
  public void createRequirementRelationships(
      IDiagramUIModel diagram, Map<String, NeedsFile.Need> needs) throws ImportException {
    try {
      System.out.println("Creating requirement relationships in diagram: " + diagram.getName());

      createDeriveRelationships(diagram, needs);
      createContainsRelationships(diagram, needs);
      createRefinementRelationships(diagram, needs);

      System.out.println("Successfully created requirement relationships");
    } catch (Exception e) {
      throw new ImportException("Failed to create requirement relationships: " + e.getMessage(), e);
    }
  }

  /** Create a requirement model element. */
  private Object createRequirement(NeedsFile.Need need, IDiagramUIModel diagram) {
    try {
      // Check if requirement already exists by VP model ID (placeholder)
      // Object existingRequirement = modelLookup.findRequirementByVpId(need.getVpModelId());
      // if (existingRequirement != null) {
      //   System.out.println("Reusing existing requirement: " + need.getTitle());
      //   addRequirementToDisplay(diagram, existingRequirement);
      //   return existingRequirement;
      // }

      // Create new requirement
      IProject project = ApplicationManager.instance().getProjectManager().getProject();
      IModelElementFactory factory = IModelElementFactory.instance();

      Object requirement = factory.createRequirement();
      if (requirement == null) {
        System.err.println("Failed to create requirement for: " + need.getTitle());
        return null;
      }

      // Set requirement properties using reflection
      setElementName(requirement, need.getTitle());
      setElementDescription(requirement, need.getContent());
      setElementUserId(requirement, need.getId());
      setRequirementPriority(requirement, need.getPriority());
      setRequirementStatus(requirement, need.getStatus());

      // Add to project (using reflection for VP API)
      try {
        java.lang.reflect.Method addElementMethod =
            project.getClass().getMethod("addChild", IModelElement.class);
        addElementMethod.invoke(project, requirement);
      } catch (Exception e) {
        // Alternative approach if addChild not available
        System.err.println("Could not add requirement to project: " + e.getMessage());
      }

      // Add to diagram display
      addRequirementToDisplay(diagram, requirement);

      System.out.println("Created requirement: " + need.getId() + " - " + need.getTitle());
      return requirement;
    } catch (Exception e) {
      System.err.println("Error creating requirement " + need.getTitle() + ": " + e.getMessage());
      return null;
    }
  }

  /** Create a use case model element (for refines relationships). */
  private Object createUseCase(NeedsFile.Need need, IDiagramUIModel diagram) {
    try {
      // Check if use case already exists by VP model ID (placeholder)
      // Object existingUseCase = modelLookup.findUseCaseByVpId(need.getVpModelId());
      // if (existingUseCase != null) {
      //   System.out.println("Reusing existing use case: " + need.getTitle());
      //   addUseCaseToDisplay(diagram, existingUseCase);
      //   return existingUseCase;
      // }

      // Create new use case
      IProject project = ApplicationManager.instance().getProjectManager().getProject();
      IModelElementFactory factory = IModelElementFactory.instance();

      Object useCase = factory.createUseCase();
      if (useCase == null) {
        System.err.println("Failed to create use case for: " + need.getTitle());
        return null;
      }

      // Set use case properties using reflection
      setElementName(useCase, need.getTitle());
      setElementDescription(useCase, need.getContent());
      setElementUserId(useCase, need.getId());

      // Add to project (using reflection for VP API)
      try {
        java.lang.reflect.Method addElementMethod =
            project.getClass().getMethod("addChild", IModelElement.class);
        addElementMethod.invoke(project, useCase);
      } catch (Exception e) {
        // Alternative approach if addChild not available
        System.err.println("Could not add use case to project: " + e.getMessage());
      }

      // Add to diagram display
      addUseCaseToDisplay(diagram, useCase);

      System.out.println("Created use case: " + need.getId() + " - " + need.getTitle());
      return useCase;
    } catch (Exception e) {
      System.err.println("Error creating use case " + need.getTitle() + ": " + e.getMessage());
      return null;
    }
  }

  /** Create derive relationships between requirements. */
  private void createDeriveRelationships(
      IDiagramUIModel diagram, Map<String, NeedsFile.Need> needs) {
    for (Map.Entry<String, NeedsFile.Need> entry : needs.entrySet()) {
      NeedsFile.Need fromNeed = entry.getValue();

      if (fromNeed.getDeriveLinks() != null && !fromNeed.getDeriveLinks().isEmpty()) {
        for (String toNeedId : fromNeed.getDeriveLinks()) {
          NeedsFile.Need toNeed = needs.get(toNeedId);
          if (toNeed != null) {
            createDeriveRelationship(diagram, fromNeed, toNeed);
          }
        }
      }
    }
  }

  /** Create contains relationships between requirements. */
  private void createContainsRelationships(
      IDiagramUIModel diagram, Map<String, NeedsFile.Need> needs) {
    for (Map.Entry<String, NeedsFile.Need> entry : needs.entrySet()) {
      NeedsFile.Need fromNeed = entry.getValue();

      if (fromNeed.getContainsLinks() != null && !fromNeed.getContainsLinks().isEmpty()) {
        for (String toNeedId : fromNeed.getContainsLinks()) {
          NeedsFile.Need toNeed = needs.get(toNeedId);
          if (toNeed != null) {
            createContainsRelationship(diagram, fromNeed, toNeed);
          }
        }
      }
    }
  }

  /** Create refinement relationships from requirements to use cases. */
  private void createRefinementRelationships(
      IDiagramUIModel diagram, Map<String, NeedsFile.Need> needs) {
    for (Map.Entry<String, NeedsFile.Need> entry : needs.entrySet()) {
      NeedsFile.Need fromNeed = entry.getValue();

      if (fromNeed.getRefinesLinks() != null && !fromNeed.getRefinesLinks().isEmpty()) {
        for (String toNeedId : fromNeed.getRefinesLinks()) {
          NeedsFile.Need toNeed = needs.get(toNeedId);
          if (toNeed != null) {
            createRefinementRelationship(diagram, fromNeed, toNeed);
          }
        }
      }
    }
  }

  /** Create a derive relationship connector. */
  private void createDeriveRelationship(
      IDiagramUIModel diagram, NeedsFile.Need fromNeed, NeedsFile.Need toNeed) {
    try {
      Object fromElement = findElementInDiagram(diagram, fromNeed.getId());
      Object toElement = findElementInDiagram(diagram, toNeed.getId());

      if (fromElement != null && toElement != null) {
        // Create derive relationship using VP API
        Object relationship =
            createRelationshipConnector(diagram, fromElement, toElement, "derive");
        if (relationship != null) {
          System.out.println(
              "Created derive relationship: " + fromNeed.getId() + " -> " + toNeed.getId());
        }
      }
    } catch (Exception e) {
      System.err.println("Error creating derive relationship: " + e.getMessage());
    }
  }

  /** Create a contains relationship connector. */
  private void createContainsRelationship(
      IDiagramUIModel diagram, NeedsFile.Need fromNeed, NeedsFile.Need toNeed) {
    try {
      Object fromElement = findElementInDiagram(diagram, fromNeed.getId());
      Object toElement = findElementInDiagram(diagram, toNeed.getId());

      if (fromElement != null && toElement != null) {
        // Create contains relationship using VP API
        Object relationship =
            createRelationshipConnector(diagram, fromElement, toElement, "contains");
        if (relationship != null) {
          System.out.println(
              "Created contains relationship: " + fromNeed.getId() + " -> " + toNeed.getId());
        }
      }
    } catch (Exception e) {
      System.err.println("Error creating contains relationship: " + e.getMessage());
    }
  }

  /** Create a refinement relationship connector. */
  private void createRefinementRelationship(
      IDiagramUIModel diagram, NeedsFile.Need fromNeed, NeedsFile.Need toNeed) {
    try {
      Object fromElement = findElementInDiagram(diagram, fromNeed.getId());
      Object toElement = findElementInDiagram(diagram, toNeed.getId());

      if (fromElement != null && toElement != null) {
        // Create refinement relationship using VP API
        Object relationship =
            createRelationshipConnector(diagram, fromElement, toElement, "refines");
        if (relationship != null) {
          System.out.println(
              "Created refinement relationship: " + fromNeed.getId() + " -> " + toNeed.getId());
        }
      }
    } catch (Exception e) {
      System.err.println("Error creating refinement relationship: " + e.getMessage());
    }
  }

  /** Add requirement to diagram display. */
  private void addRequirementToDisplay(IDiagramUIModel diagram, Object requirement) {
    try {
      DiagramManager diagramManager = ApplicationManager.instance().getDiagramManager();
      // Try different method names for adding elements to diagram
      try {
        java.lang.reflect.Method createShapeMethod =
            diagramManager
                .getClass()
                .getMethod("createShapeUIModel", IDiagramUIModel.class, Object.class);
        createShapeMethod.invoke(diagramManager, diagram, requirement);
      } catch (Exception e) {
        System.err.println("Could not add requirement to diagram display: " + e.getMessage());
      }
    } catch (Exception e) {
      System.err.println("Error adding requirement to display: " + e.getMessage());
    }
  }

  /** Add use case to diagram display. */
  private void addUseCaseToDisplay(IDiagramUIModel diagram, Object useCase) {
    try {
      DiagramManager diagramManager = ApplicationManager.instance().getDiagramManager();
      // Try different method names for adding elements to diagram
      try {
        java.lang.reflect.Method createShapeMethod =
            diagramManager
                .getClass()
                .getMethod("createShapeUIModel", IDiagramUIModel.class, Object.class);
        createShapeMethod.invoke(diagramManager, diagram, useCase);
      } catch (Exception e) {
        System.err.println("Could not add use case to diagram display: " + e.getMessage());
      }
    } catch (Exception e) {
      System.err.println("Error adding use case to display: " + e.getMessage());
    }
  }

  /** Find element in diagram by need ID. */
  private Object findElementInDiagram(IDiagramUIModel diagram, String needId) {
    try {
      // Search through diagram elements to find one with matching User ID
      // This would need to be implemented based on VP's specific API
      // For now, return null - this will be enhanced when we have exact VP API details
      return null;
    } catch (Exception e) {
      System.err.println("Error finding element in diagram: " + e.getMessage());
      return null;
    }
  }

  /** Create relationship connector between elements. */
  private Object createRelationshipConnector(
      IDiagramUIModel diagram, Object fromElement, Object toElement, String relationshipType) {
    try {
      DiagramManager diagramManager = ApplicationManager.instance().getDiagramManager();

      // The exact method for creating relationship connectors will depend on VP API
      // This is a placeholder that would need VP-specific implementation
      switch (relationshipType) {
        case "derive":
          // Create derive relationship connector
          break;
        case "contains":
          // Create contains relationship connector
          break;
        case "refines":
          // Create refinement relationship connector
          break;
      }

      return null; // Placeholder
    } catch (Exception e) {
      System.err.println("Error creating relationship connector: " + e.getMessage());
      return null;
    }
  }

  /** Set element name using reflection. */
  private void setElementName(Object element, String name) {
    try {
      java.lang.reflect.Method setNameMethod =
          element.getClass().getMethod("setName", String.class);
      setNameMethod.invoke(element, name);
    } catch (Exception e) {
      System.err.println("Error setting element name: " + e.getMessage());
    }
  }

  /** Set element description using reflection. */
  private void setElementDescription(Object element, String description) {
    try {
      java.lang.reflect.Method setDescriptionMethod =
          element.getClass().getMethod("setDescription", String.class);
      setDescriptionMethod.invoke(element, description != null ? description : "");
    } catch (Exception e) {
      System.err.println("Error setting element description: " + e.getMessage());
    }
  }

  /** Set element User ID using reflection. */
  private void setElementUserId(Object element, String userId) {
    try {
      java.lang.reflect.Method setUserIdMethod =
          element.getClass().getMethod("setUserID", String.class);
      setUserIdMethod.invoke(element, userId);
    } catch (Exception e) {
      try {
        java.lang.reflect.Method setUserIdMethod =
            element.getClass().getMethod("setUserId", String.class);
        setUserIdMethod.invoke(element, userId);
      } catch (Exception e2) {
        System.err.println("Error setting element User ID: " + e2.getMessage());
      }
    }
  }

  /** Set requirement priority using reflection. */
  private void setRequirementPriority(Object requirement, String priority) {
    if (priority == null || priority.isEmpty()) return;

    try {
      // Convert priority string to VP priority constant
      int priorityValue = convertPriorityToVPConstant(priority);

      java.lang.reflect.Method setPriorityMethod =
          requirement.getClass().getMethod("setPriority", int.class);
      setPriorityMethod.invoke(requirement, priorityValue);
    } catch (Exception e) {
      try {
        java.lang.reflect.Method setReqPriorityMethod =
            requirement.getClass().getMethod("setReqPriority", int.class);
        int priorityValue = convertPriorityToVPConstant(priority);
        setReqPriorityMethod.invoke(requirement, priorityValue);
      } catch (Exception e2) {
        System.err.println("Error setting requirement priority: " + e2.getMessage());
      }
    }
  }

  /** Set requirement status using reflection. */
  private void setRequirementStatus(Object requirement, String status) {
    if (status == null || status.isEmpty()) status = "open";

    try {
      java.lang.reflect.Method setStatusMethod =
          requirement.getClass().getMethod("setStatus", String.class);
      setStatusMethod.invoke(requirement, status);
    } catch (Exception e) {
      System.err.println("Error setting requirement status: " + e.getMessage());
    }
  }

  /** Convert priority string to VP priority constant. */
  private int convertPriorityToVPConstant(String priority) {
    return switch (priority.toLowerCase()) {
      case "critical" -> 1;
      case "high" -> 2;
      case "medium" -> 3;
      case "low" -> 4;
      default -> 0; // Unspecified
    };
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
