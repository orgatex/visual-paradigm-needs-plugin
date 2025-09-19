package com.orgatex.vp.sphinx.importer;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.DiagramManager;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramTypeConstants;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.model.IActor;
import com.vp.plugin.model.IAssociation;
import com.vp.plugin.model.IExtend;
import com.vp.plugin.model.IInclude;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IUseCase;
import com.vp.plugin.model.factory.IModelElementFactory;
import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Builder class for creating Visual Paradigm use case diagrams from needs data. */
public class UseCaseDiagramBuilder {

  private final DiagramManager diagramManager;
  private final IModelElementFactory modelFactory;
  private final ElementLayoutEngine layoutEngine;
  private final ModelLookup modelLookup;

  // Track created elements for relationship creation
  private final Map<String, IDiagramElement> createdElements = new HashMap<>();
  private final Map<String, IModelElement> createdModels = new HashMap<>();

  public UseCaseDiagramBuilder() {
    this.diagramManager = ApplicationManager.instance().getDiagramManager();
    this.modelFactory = IModelElementFactory.instance();
    this.layoutEngine = new ElementLayoutEngine();
    this.modelLookup = new ModelLookup();
  }

  /**
   * Create a new use case diagram.
   *
   * @param diagramName Name for the diagram
   * @return The created diagram
   * @throws Exception if diagram creation fails
   */
  public IDiagramUIModel createUseCaseDiagram(String diagramName) throws Exception {
    IDiagramUIModel diagram =
        diagramManager.createDiagram(IDiagramTypeConstants.DIAGRAM_TYPE_USE_CASE_DIAGRAM);
    diagram.setName(diagramName);
    return diagram;
  }

  /**
   * Create all use case elements in the diagram.
   *
   * @param diagram The target diagram
   * @param needs Map of needs to create
   * @throws Exception if element creation fails
   */
  public void createUseCaseElements(IDiagramUIModel diagram, Map<String, NeedsFile.Need> needs)
      throws Exception {
    // Calculate layout positions
    Map<String, Point> positions = layoutEngine.calculateLayout(needs);

    // Create elements for each need
    for (Map.Entry<String, NeedsFile.Need> entry : needs.entrySet()) {
      String needId = entry.getKey();
      NeedsFile.Need need = entry.getValue();

      if (isUseCaseNeed(need)) {
        createUseCaseElement(diagram, need, positions.get(needId));
      } else if (isActorNeed(need)) {
        createActorElement(diagram, need, positions.get(needId));
      }
    }

    System.out.println("Created " + createdElements.size() + " diagram elements");
  }

  /**
   * Create relationships between elements.
   *
   * @param diagram The target diagram
   * @param needs Map of needs with relationship data
   * @throws Exception if relationship creation fails
   */
  public void createRelationships(IDiagramUIModel diagram, Map<String, NeedsFile.Need> needs)
      throws Exception {
    int relationshipCount = 0;

    for (Map.Entry<String, NeedsFile.Need> entry : needs.entrySet()) {
      String sourceId = entry.getKey();
      NeedsFile.Need need = entry.getValue();

      // Create include relationships
      relationshipCount += createIncludeRelationships(diagram, sourceId, need.getIncludesLinks());

      // Create extend relationships
      relationshipCount += createExtendRelationships(diagram, sourceId, need.getExtendsLinks());

      // Create association relationships
      relationshipCount +=
          createAssociationRelationships(diagram, sourceId, need.getAssociatesLinks());
    }

    System.out.println("Created " + relationshipCount + " relationships");
  }

  /** Create a use case element in the diagram. */
  private void createUseCaseElement(IDiagramUIModel diagram, NeedsFile.Need need, Point position)
      throws Exception {
    System.out.println("=== DEBUG: Creating use case element ===");
    System.out.println("Need ID: " + need.getId());
    System.out.println("Need Title: " + need.getTitle());
    System.out.println("Need VP Model ID: " + need.getVpModelId());

    IUseCase useCaseModel = null;
    boolean isReusedModel = false;

    // Check if model already exists using Visual Paradigm model ID
    if (need.getVpModelId() != null && !need.getVpModelId().trim().isEmpty()) {
      System.out.println("DEBUG: Looking for existing model with VP ID: " + need.getVpModelId());
      useCaseModel = modelLookup.findModelById(need.getVpModelId(), IUseCase.class);
      if (useCaseModel != null) {
        isReusedModel = true;
        System.out.println(
            "DEBUG: ✓ Found existing model! Name: "
                + useCaseModel.getName()
                + ", ID: "
                + useCaseModel.getId());
        System.out.println(
            "Reusing existing use case model: " + need.getId() + " - " + need.getTitle());
      } else {
        System.out.println("DEBUG: ✗ No existing model found with VP ID: " + need.getVpModelId());
      }
    } else {
      System.out.println("DEBUG: No VP Model ID provided, will create new model");
    }

    // Create new model if not found
    if (useCaseModel == null) {
      System.out.println("DEBUG: Creating new use case model...");
      useCaseModel = modelFactory.createUseCase();
      useCaseModel.setName(need.getTitle());
      useCaseModel.setUserID(need.getId());
      // Set status if available and valid
      setUseCaseStatus(useCaseModel, need.getStatus());
      System.out.println("DEBUG: Created new model with VP ID: " + useCaseModel.getId());
      System.out.println("Created new use case model: " + need.getId() + " - " + need.getTitle());
    }

    // Create the diagram element (auxiliary view for reused models)
    IDiagramElement useCaseElement = diagramManager.createDiagramElement(diagram, useCaseModel);

    // Set position and size
    int width = 120;
    int height = 60;
    if (position != null) {
      useCaseElement.setBounds(position.x, position.y, width, height);
    }

    // Store for relationship creation
    createdElements.put(need.getId(), useCaseElement);
    createdModels.put(need.getId(), useCaseModel);

    if (isReusedModel) {
      System.out.println(
          "Added auxiliary view for use case: " + need.getId() + " - " + need.getTitle());
    } else {
      System.out.println("Created use case: " + need.getId() + " - " + need.getTitle());
    }
  }

  /** Create an actor element in the diagram. */
  private void createActorElement(IDiagramUIModel diagram, NeedsFile.Need need, Point position)
      throws Exception {
    System.out.println("=== DEBUG: Creating actor element ===");
    System.out.println("Need ID: " + need.getId());
    System.out.println("Need Title: " + need.getTitle());
    System.out.println("Need VP Model ID: " + need.getVpModelId());

    IActor actorModel = null;
    boolean isReusedModel = false;

    // Check if model already exists using Visual Paradigm model ID
    if (need.getVpModelId() != null && !need.getVpModelId().trim().isEmpty()) {
      System.out.println("DEBUG: Looking for existing actor with VP ID: " + need.getVpModelId());
      actorModel = modelLookup.findModelById(need.getVpModelId(), IActor.class);
      if (actorModel != null) {
        isReusedModel = true;
        System.out.println(
            "DEBUG: ✓ Found existing actor! Name: "
                + actorModel.getName()
                + ", ID: "
                + actorModel.getId());
        System.out.println(
            "Reusing existing actor model: " + need.getId() + " - " + need.getTitle());
      } else {
        System.out.println("DEBUG: ✗ No existing actor found with VP ID: " + need.getVpModelId());
      }
    } else {
      System.out.println("DEBUG: No VP Model ID provided, will create new actor");
    }

    // Create new model if not found
    if (actorModel == null) {
      System.out.println("DEBUG: Creating new actor model...");
      actorModel = modelFactory.createActor();
      actorModel.setName(need.getTitle());
      actorModel.setUserID(need.getId());
      System.out.println("DEBUG: Created new actor with VP ID: " + actorModel.getId());
      System.out.println("Created new actor model: " + need.getId() + " - " + need.getTitle());
    }

    // Create the diagram element (auxiliary view for reused models)
    IDiagramElement actorElement = diagramManager.createDiagramElement(diagram, actorModel);

    // Set position and size
    int width = 60;
    int height = 80;
    if (position != null) {
      actorElement.setBounds(position.x, position.y, width, height);
    }

    // Store for relationship creation
    createdElements.put(need.getId(), actorElement);
    createdModels.put(need.getId(), actorModel);

    if (isReusedModel) {
      System.out.println(
          "Added auxiliary view for actor: " + need.getId() + " - " + need.getTitle());
    } else {
      System.out.println("Created actor: " + need.getId() + " - " + need.getTitle());
    }
  }

  /** Create include relationships. */
  private int createIncludeRelationships(
      IDiagramUIModel diagram, String sourceId, String includesLinks) throws Exception {
    if (includesLinks == null || includesLinks.trim().isEmpty()) {
      return 0;
    }

    Set<String> targetIds = parseRelationshipTargets(includesLinks);
    int count = 0;

    for (String targetId : targetIds) {
      if (createIncludeRelationship(diagram, sourceId, targetId)) {
        count++;
      }
    }

    return count;
  }

  /** Create extend relationships. */
  private int createExtendRelationships(
      IDiagramUIModel diagram, String sourceId, String extendsLinks) throws Exception {
    if (extendsLinks == null || extendsLinks.trim().isEmpty()) {
      return 0;
    }

    Set<String> targetIds = parseRelationshipTargets(extendsLinks);
    int count = 0;

    for (String targetId : targetIds) {
      if (createExtendRelationship(diagram, sourceId, targetId)) {
        count++;
      }
    }

    return count;
  }

  /** Create association relationships. */
  private int createAssociationRelationships(
      IDiagramUIModel diagram, String sourceId, String associatesLinks) throws Exception {
    if (associatesLinks == null || associatesLinks.trim().isEmpty()) {
      return 0;
    }

    Set<String> targetIds = parseRelationshipTargets(associatesLinks);
    int count = 0;

    for (String targetId : targetIds) {
      if (createAssociationRelationship(diagram, sourceId, targetId)) {
        count++;
      }
    }

    return count;
  }

  /** Create a single include relationship. */
  private boolean createIncludeRelationship(
      IDiagramUIModel diagram, String sourceId, String targetId) throws Exception {
    IDiagramElement sourceElement = createdElements.get(sourceId);
    IDiagramElement targetElement = createdElements.get(targetId);
    IModelElement sourceModel = createdModels.get(sourceId);
    IModelElement targetModel = createdModels.get(targetId);

    if (sourceElement == null
        || targetElement == null
        || sourceModel == null
        || targetModel == null) {
      System.err.println(
          "Warning: Cannot create include relationship from "
              + sourceId
              + " to "
              + targetId
              + " - elements not found");
      return false;
    }

    // Create include model
    IInclude includeModel = modelFactory.createInclude();
    includeModel.setFrom(sourceModel);
    includeModel.setTo(targetModel);

    // Create connector
    diagramManager.createConnector(diagram, includeModel, sourceElement, targetElement, null);

    return true;
  }

  /** Create a single extend relationship. */
  private boolean createExtendRelationship(
      IDiagramUIModel diagram, String sourceId, String targetId) throws Exception {
    IDiagramElement sourceElement = createdElements.get(sourceId);
    IDiagramElement targetElement = createdElements.get(targetId);
    IModelElement sourceModel = createdModels.get(sourceId);
    IModelElement targetModel = createdModels.get(targetId);

    if (sourceElement == null
        || targetElement == null
        || sourceModel == null
        || targetModel == null) {
      System.err.println(
          "Warning: Cannot create extend relationship from "
              + sourceId
              + " to "
              + targetId
              + " - elements not found");
      return false;
    }

    // Create extend model
    IExtend extendModel = modelFactory.createExtend();
    extendModel.setFrom(sourceModel);
    extendModel.setTo(targetModel);

    // Create connector
    diagramManager.createConnector(diagram, extendModel, sourceElement, targetElement, null);

    return true;
  }

  /** Create a single association relationship. */
  private boolean createAssociationRelationship(
      IDiagramUIModel diagram, String sourceId, String targetId) throws Exception {
    IDiagramElement sourceElement = createdElements.get(sourceId);
    IDiagramElement targetElement = createdElements.get(targetId);
    IModelElement sourceModel = createdModels.get(sourceId);
    IModelElement targetModel = createdModels.get(targetId);

    if (sourceElement == null
        || targetElement == null
        || sourceModel == null
        || targetModel == null) {
      System.err.println(
          "Warning: Cannot create association relationship from "
              + sourceId
              + " to "
              + targetId
              + " - elements not found");
      return false;
    }

    // Create association model
    IAssociation associationModel = modelFactory.createAssociation();
    associationModel.setFrom(sourceModel);
    associationModel.setTo(targetModel);

    // Create connector
    diagramManager.createConnector(diagram, associationModel, sourceElement, targetElement, null);

    return true;
  }

  /** Parse relationship target IDs from comma-separated string. */
  private Set<String> parseRelationshipTargets(String relationshipString) {
    Set<String> targets = new HashSet<>();
    if (relationshipString != null && !relationshipString.trim().isEmpty()) {
      String[] parts = relationshipString.split(",");
      for (String part : parts) {
        String trimmed = part.trim();
        if (!trimmed.isEmpty()) {
          targets.add(trimmed);
        }
      }
    }
    return targets;
  }

  /** Check if a need represents a use case. */
  private boolean isUseCaseNeed(NeedsFile.Need need) {
    return "uc".equals(need.getType()) || "UseCase".equals(need.getElementType());
  }

  /** Check if a need represents an actor. */
  private boolean isActorNeed(NeedsFile.Need need) {
    return "actor".equals(need.getType()) || "Actor".equals(need.getElementType());
  }

  /** Set use case status if valid. */
  private void setUseCaseStatus(IUseCase useCaseModel, String status) {
    if (status == null || status.trim().isEmpty()) {
      return;
    }

    try {
      // Map status strings to VP status constants
      int vpStatus = mapStatusToVP(status);
      if (vpStatus >= 0) {
        useCaseModel.setStatus(vpStatus);
      }
    } catch (Exception e) {
      System.err.println(
          "Warning: Could not set status '" + status + "' for use case: " + e.getMessage());
    }
  }

  /** Map status string to Visual Paradigm status constant. */
  private int mapStatusToVP(String status) {
    switch (status.toLowerCase()) {
      case "identify":
        return 0; // STATUS_IDENTIFY
      case "discuss":
        return 1; // STATUS_DISCUSS
      case "elaborate":
        return 2; // STATUS_ELABORATE
      case "design":
        return 3; // STATUS_DESIGN
      case "consent":
        return 4; // STATUS_CONSENT
      case "develop":
        return 5; // STATUS_DEVELOP
      case "complete":
        return 6; // STATUS_COMPLETE
      default:
        return -1; // Unknown status
    }
  }
}
