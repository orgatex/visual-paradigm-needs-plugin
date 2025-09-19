package com.orgatex.vp.sphinx.importer;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.DiagramManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IProject;
import java.util.Iterator;

/** Utility class for finding existing Visual Paradigm models by ID. */
public class ModelLookup {

  private final ProjectManager projectManager;

  public ModelLookup() {
    ApplicationManager appManager = ApplicationManager.instance();
    this.projectManager = (appManager != null) ? appManager.getProjectManager() : null;
  }

  /**
   * Find an existing model element by its Visual Paradigm ID.
   *
   * @param modelId The Visual Paradigm model ID to search for
   * @return The model element if found, null otherwise
   */
  public IModelElement findModelById(String modelId) {
    System.out.println("=== DEBUG: ModelLookup.findModelById ===");
    System.out.println("Searching for model ID: " + modelId);

    if (modelId == null || modelId.trim().isEmpty()) {
      System.out.println("DEBUG: ModelId is null or empty, returning null");
      return null;
    }

    try {
      if (projectManager == null) {
        System.out.println("DEBUG: ProjectManager is null (test environment), returning null");
        return null;
      }

      IProject project = projectManager.getProject();
      if (project == null) {
        System.out.println("DEBUG: Project is null, returning null");
        return null;
      }

      System.out.println("DEBUG: Project found, searching through project models...");

      // First try: Search through all project-level model elements
      IModelElement found = searchInProjectModels(project, modelId);
      if (found != null) {
        return found;
      }

      // Second try: Search through all diagrams for models
      System.out.println("DEBUG: Not found in project models, searching in diagrams...");
      found = searchInDiagramModels(project, modelId);
      if (found != null) {
        return found;
      }

      System.out.println("DEBUG: Model not found in project or diagrams");
    } catch (Exception e) {
      System.err.println(
          "ERROR: Exception during model search for ID " + modelId + ": " + e.getMessage());
      e.printStackTrace();
    }

    System.out.println("DEBUG: Returning null - model not found");
    return null;
  }

  /** Search for model in project-level models. */
  private IModelElement searchInProjectModels(IProject project, String modelId) {
    try {
      Iterator<IModelElement> allModels = project.allLevelModelElementIterator();
      int modelCount = 0;
      while (allModels.hasNext()) {
        IModelElement element = allModels.next();
        modelCount++;
        String elementId = element.getId();
        String elementName = element.getName();
        String elementType = element.getClass().getSimpleName();

        if (modelCount <= 10) { // Log first 10 models to see what's available
          System.out.println(
              "DEBUG: Project Model "
                  + modelCount
                  + " - ID: "
                  + elementId
                  + ", Name: "
                  + elementName
                  + ", Type: "
                  + elementType);
        }

        if (modelId.equals(elementId)) {
          System.out.println(
              "DEBUG: ✓ FOUND MATCH in project models! ID: "
                  + elementId
                  + ", Name: "
                  + elementName
                  + ", Type: "
                  + elementType);
          return element;
        }
      }

      System.out.println(
          "DEBUG: Searched through " + modelCount + " project models, no match found");
    } catch (Exception e) {
      System.err.println("ERROR: Exception searching project models: " + e.getMessage());
    }
    return null;
  }

  /** Search for model in all diagrams. */
  private IModelElement searchInDiagramModels(IProject project, String modelId) {
    try {
      ApplicationManager appManager = ApplicationManager.instance();
      if (appManager == null) {
        System.err.println("DEBUG: ApplicationManager not available in test environment");
        return null;
      }

      DiagramManager diagramManager = appManager.getDiagramManager();
      if (diagramManager == null) {
        System.err.println("DEBUG: DiagramManager not available");
        return null;
      }

      Iterator<IDiagramUIModel> diagrams = project.diagramIterator();
      if (diagrams == null) {
        System.err.println("DEBUG: No diagrams available");
        return null;
      }
      int diagramCount = 0;
      int totalElementsSearched = 0;

      while (diagrams.hasNext()) {
        IDiagramUIModel diagram = diagrams.next();
        diagramCount++;
        String diagramName = diagram.getName();
        String diagramType = diagram.getType();

        System.out.println(
            "DEBUG: Searching diagram "
                + diagramCount
                + " - "
                + diagramName
                + " ("
                + diagramType
                + ")");

        // Search through all elements in this diagram
        Iterator<IDiagramElement> diagramElements = diagram.diagramElementIterator();
        int elementsInDiagram = 0;

        while (diagramElements.hasNext()) {
          IDiagramElement diagramElement = diagramElements.next();
          elementsInDiagram++;
          totalElementsSearched++;

          IModelElement modelElement = diagramElement.getModelElement();
          if (modelElement != null) {
            String elementId = modelElement.getId();
            String elementName = modelElement.getName();
            String elementType = modelElement.getClass().getSimpleName();

            if (totalElementsSearched <= 20) { // Log first 20 diagram elements
              System.out.println(
                  "DEBUG: Diagram Element "
                      + totalElementsSearched
                      + " - ID: "
                      + elementId
                      + ", Name: "
                      + elementName
                      + ", Type: "
                      + elementType);
            }

            if (modelId.equals(elementId)) {
              System.out.println(
                  "DEBUG: ✓ FOUND MATCH in diagram '"
                      + diagramName
                      + "'! ID: "
                      + elementId
                      + ", Name: "
                      + elementName
                      + ", Type: "
                      + elementType);
              return modelElement;
            }
          }
        }

        System.out.println(
            "DEBUG: Searched " + elementsInDiagram + " elements in diagram " + diagramName);
      }

      System.out.println(
          "DEBUG: Searched through "
              + diagramCount
              + " diagrams with "
              + totalElementsSearched
              + " total elements, no match found");
    } catch (Exception e) {
      System.err.println("ERROR: Exception searching diagram models: " + e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Check if a model with the given Visual Paradigm ID already exists in the project.
   *
   * @param modelId The Visual Paradigm model ID to check
   * @return true if a model with this ID exists, false otherwise
   */
  public boolean modelExists(String modelId) {
    return findModelById(modelId) != null;
  }

  /**
   * Find an existing model element by its Visual Paradigm ID with type checking.
   *
   * @param modelId The Visual Paradigm model ID to search for
   * @param expectedType The expected class type (e.g., IUseCase.class, IActor.class)
   * @return The model element if found and of correct type, null otherwise
   */
  @SuppressWarnings("unchecked")
  public <T extends IModelElement> T findModelById(String modelId, Class<T> expectedType) {
    IModelElement element = findModelById(modelId);
    if (element != null && expectedType.isInstance(element)) {
      return (T) element;
    }
    return null;
  }
}
