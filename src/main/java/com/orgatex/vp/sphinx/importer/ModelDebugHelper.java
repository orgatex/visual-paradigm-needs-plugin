package com.orgatex.vp.sphinx.importer;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IProject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Debug helper for troubleshooting model reuse issues. */
public class ModelDebugHelper {

  public static void listAllModelsInProject() {
    System.out.println("=== DEBUG: Listing all models in current project ===");

    try {
      ProjectManager projectManager = ApplicationManager.instance().getProjectManager();
      IProject project = projectManager.getProject();

      if (project == null) {
        System.out.println("DEBUG: No project is currently open");
        return;
      }

      System.out.println("DEBUG: Project name: " + project.getName());

      Iterator<IModelElement> allModels = project.allLevelModelElementIterator();
      List<IModelElement> models = new ArrayList<>();

      while (allModels.hasNext()) {
        models.add(allModels.next());
      }

      System.out.println("DEBUG: Found " + models.size() + " total models in project");

      // Group by type
      int useCaseCount = 0;
      int actorCount = 0;
      int otherCount = 0;

      for (IModelElement model : models) {
        String type = model.getClass().getSimpleName();
        if (type.contains("UseCase")) {
          useCaseCount++;
          if (useCaseCount <= 5) {
            System.out.println(
                "DEBUG: UseCase - ID: " + model.getId() + ", Name: " + model.getName());
          }
        } else if (type.contains("Actor")) {
          actorCount++;
          if (actorCount <= 5) {
            System.out.println(
                "DEBUG: Actor - ID: " + model.getId() + ", Name: " + model.getName());
          }
        } else {
          otherCount++;
          if (otherCount <= 5) {
            System.out.println(
                "DEBUG: " + type + " - ID: " + model.getId() + ", Name: " + model.getName());
          }
        }
      }

      System.out.println(
          "DEBUG: Summary - UseCases: "
              + useCaseCount
              + ", Actors: "
              + actorCount
              + ", Others: "
              + otherCount);

    } catch (Exception e) {
      System.err.println("DEBUG: Error listing models: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void searchForSpecificIds(String[] idsToFind) {
    System.out.println("=== DEBUG: Searching for specific model IDs ===");

    try {
      ProjectManager projectManager = ApplicationManager.instance().getProjectManager();
      IProject project = projectManager.getProject();

      if (project == null) {
        System.out.println("DEBUG: No project is currently open");
        return;
      }

      for (String id : idsToFind) {
        System.out.println("DEBUG: Looking for ID: " + id);
        boolean found = false;

        Iterator<IModelElement> allModels = project.allLevelModelElementIterator();
        while (allModels.hasNext()) {
          IModelElement model = allModels.next();
          if (id.equals(model.getId())) {
            System.out.println(
                "DEBUG: ✓ FOUND - ID: "
                    + id
                    + ", Name: "
                    + model.getName()
                    + ", Type: "
                    + model.getClass().getSimpleName());
            found = true;
            break;
          }
        }

        if (!found) {
          System.out.println("DEBUG: ✗ NOT FOUND - ID: " + id);
        }
      }

    } catch (Exception e) {
      System.err.println("DEBUG: Error searching for IDs: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
