package com.orgatex.vp.sphinx.extractor;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.model.IActor;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IProject;
import com.vp.plugin.model.IRequirement;
import com.vp.plugin.model.IUseCase;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Extracts model elements (use cases, actors, requirements) from Visual Paradigm projects. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NeedsModelExtractor {

  /** Extract all model elements from the current VP project and convert to Needs. */
  public static ExtractionResult extractAllModels() {
    try {
      ApplicationManager appManager = ApplicationManager.instance();
      if (appManager == null) {
        System.err.println("ApplicationManager not available (test environment)");
        return new ExtractionResult(new ArrayList<>(), new HashMap<>());
      }

      ProjectManager projectManager = appManager.getProjectManager();
      if (projectManager == null) {
        System.err.println("ProjectManager not available");
        return new ExtractionResult(new ArrayList<>(), new HashMap<>());
      }

      IProject project = projectManager.getProject();
      if (project == null) {
        System.err.println("No project found, cannot extract models");
        return new ExtractionResult(new ArrayList<>(), new HashMap<>());
      }

      List<NeedsFile.Need> needs = new ArrayList<>();
      Map<String, String> vpIdToUserId = new HashMap<>();

      // Extract all model elements from project
      Iterator<IModelElement> allModels = project.allLevelModelElementIterator();
      while (allModels.hasNext()) {
        IModelElement element = allModels.next();

        if (element instanceof IUseCase useCase) {
          NeedsFile.Need need = processUseCase(useCase, vpIdToUserId);
          if (need != null) {
            needs.add(need);
          }
        } else if (element instanceof IActor actor) {
          NeedsFile.Need need = processActor(actor, vpIdToUserId);
          if (need != null) {
            needs.add(need);
          }
        } else if (element instanceof IRequirement requirement) {
          NeedsFile.Need need = processRequirement(requirement, vpIdToUserId);
          if (need != null) {
            needs.add(need);
          }
        }
      }

      System.out.println("Extracted " + needs.size() + " total elements from project");
      return new ExtractionResult(needs, vpIdToUserId);

    } catch (Exception e) {
      System.err.println("Error extracting project models: " + e.getMessage());
      e.printStackTrace();
      return new ExtractionResult(new ArrayList<>(), new HashMap<>());
    }
  }

  /** Process a use case model element and convert to Need. */
  private static NeedsFile.Need processUseCase(IUseCase useCase, Map<String, String> vpIdToUserId) {
    try {
      String name = useCase.getName();
      if (name == null || name.trim().isEmpty()) {
        name = "Unnamed Use Case";
      }

      // Use Visual Paradigm's User ID field, with fallback to VP internal ID
      String id = VpModelProcessor.getUserId(useCase);
      if (id == null || id.trim().isEmpty()) {
        // Fallback: use VP internal ID with UC prefix for use cases
        id = "UC_" + useCase.getId();
        System.out.println("Using fallback ID for use case '" + name + "': " + id);
      }

      // Track VP internal ID to User ID mapping for relationship resolution
      vpIdToUserId.put(useCase.getId(), id);

      NeedsFile.Need need = new NeedsFile.Need(id, name, "uc");
      need.setContent(VpModelProcessor.getDescription(useCase));
      need.setStatus(VpModelProcessor.getStatus(useCase));
      need.setElementType("UseCase");
      need.setPriority(VpModelProcessor.getRank(useCase));

      // Ensure vp_model_id is never null
      String vpModelId = useCase.getId();
      if (vpModelId == null || vpModelId.trim().isEmpty()) {
        vpModelId = "uc_" + System.nanoTime(); // Generate fallback ID
        System.out.println("Warning: Generated fallback VP model ID for use case: " + vpModelId);
      }
      need.setVpModelId(vpModelId);

      // Set tags
      List<String> tags = new ArrayList<>();
      tags.add("usecase");
      tags.add("functional");
      need.setTags(tags);

      System.out.println("Processed use case: " + id + " - " + name);
      return need;

    } catch (Exception e) {
      System.err.println("Error processing use case " + useCase.getName() + ": " + e.getMessage());
      return null;
    }
  }

  /** Process an actor model element and convert to Need. */
  private static NeedsFile.Need processActor(IActor actor, Map<String, String> vpIdToUserId) {
    try {
      String name = actor.getName();
      if (name == null || name.trim().isEmpty()) {
        name = "Unnamed Actor";
      }

      // Use Visual Paradigm's User ID field, with fallback to VP internal ID
      String id = VpModelProcessor.getUserId(actor);
      if (id == null || id.trim().isEmpty()) {
        // Fallback: use VP internal ID with AC prefix for actors
        id = "AC_" + actor.getId();
        System.out.println("Using fallback ID for actor '" + name + "': " + id);
      }

      // Track VP internal ID to User ID mapping for relationship resolution
      vpIdToUserId.put(actor.getId(), id);

      NeedsFile.Need need = new NeedsFile.Need(id, name, "act");
      need.setContent(VpModelProcessor.getDescription(actor));
      need.setStatus("identify"); // Actors typically start in identify phase
      need.setElementType("Actor");

      // Ensure vp_model_id is never null
      String vpModelId = actor.getId();
      if (vpModelId == null || vpModelId.trim().isEmpty()) {
        vpModelId = "ac_" + System.nanoTime(); // Generate fallback ID
        System.out.println("Warning: Generated fallback VP model ID for actor: " + vpModelId);
      }
      need.setVpModelId(vpModelId);

      // Set tags for actors
      List<String> tags = new ArrayList<>();
      tags.add("act");
      tags.add("stakeholder");
      need.setTags(tags);

      System.out.println("Processed actor: " + id + " - " + name);
      return need;

    } catch (Exception e) {
      System.err.println("Error processing actor " + actor.getName() + ": " + e.getMessage());
      return null;
    }
  }

  /** Process a requirement model element and convert to Need. */
  private static NeedsFile.Need processRequirement(
      IRequirement requirement, Map<String, String> vpIdToUserId) {
    try {
      String name = requirement.getName();
      if (name == null || name.trim().isEmpty()) {
        name = "Untitled Requirement";
      }

      // Get User ID from requirement
      String id = VpModelProcessor.getUserId(requirement);

      // Generate ID if not set
      if (id == null || id.trim().isEmpty()) {
        id = "REQ_" + requirement.getId();
        System.out.println("Using fallback ID for requirement '" + name + "': " + id);
      }

      // Track VP internal ID to User ID mapping
      vpIdToUserId.put(requirement.getId(), id);

      NeedsFile.Need need = new NeedsFile.Need(id, name, "req");
      need.setContent(VpModelProcessor.getDescription(requirement));
      need.setElementType("Requirement");

      // Ensure vp_model_id is never null
      String vpModelId = requirement.getId();
      if (vpModelId == null || vpModelId.trim().isEmpty()) {
        vpModelId = "req_" + System.nanoTime(); // Generate fallback ID
        System.out.println("Warning: Generated fallback VP model ID for requirement: " + vpModelId);
      }
      need.setVpModelId(vpModelId);

      // Try to extract requirement-specific properties
      try {
        String priority = VpModelProcessor.getRequirementPriority(requirement);
        if (priority != null && !priority.trim().isEmpty()) {
          need.setPriority(priority);
        }
      } catch (Exception e) {
        // Priority not available or failed to extract
      }

      try {
        String status = VpModelProcessor.getRequirementStatus(requirement);
        if (status != null && !status.trim().isEmpty()) {
          need.setStatus(status);
        } else {
          need.setStatus("open"); // Default status
        }
      } catch (Exception e) {
        need.setStatus("open"); // Default status
      }

      // Set tags
      List<String> tags = new ArrayList<>();
      tags.add("requirement");
      tags.add("functional");
      need.setTags(tags);

      System.out.println("Processed requirement: " + id + " - " + name);
      return need;

    } catch (Exception e) {
      System.err.println(
          "Error processing requirement " + requirement.getName() + ": " + e.getMessage());
      return null;
    }
  }

  /** Result container for model extraction operation. */
  public static class ExtractionResult {
    private final List<NeedsFile.Need> needs;
    private final Map<String, String> vpIdToUserId;

    public ExtractionResult(List<NeedsFile.Need> needs, Map<String, String> vpIdToUserId) {
      this.needs = needs;
      this.vpIdToUserId = vpIdToUserId;
    }

    public List<NeedsFile.Need> getNeeds() {
      return needs;
    }

    public Map<String, String> getVpIdToUserId() {
      return vpIdToUserId;
    }
  }
}
