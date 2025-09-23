package com.orgatex.vp.sphinx.extractor;

import com.vp.plugin.model.IModelElement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class for processing Visual Paradigm model elements. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VpModelProcessor {

  /** Extract User ID from a Visual Paradigm model element using reflection. */
  public static String getUserId(IModelElement element) {
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

  /** Extract description/content from a model element. */
  public static String getDescription(IModelElement element) {
    try {
      String description = element.getDescription();
      return (description != null && !description.trim().isEmpty()) ? description : "";
    } catch (Exception e) {
      return "";
    }
  }

  /** Extract rank/priority from a use case model element. */
  public static String getRank(IModelElement element) {
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
    } catch (Exception e) {
      // getPmPriority method doesn't exist or failed
    }

    return ""; // No rank/priority information found
  }

  /** Extract status from a use case model element. */
  public static String getStatus(IModelElement element) {
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

  /** Extract requirement priority using reflection. */
  public static String getRequirementPriority(IModelElement requirement) {
    try {
      java.lang.reflect.Method getPriorityMethod = requirement.getClass().getMethod("getPriority");
      Object priority = getPriorityMethod.invoke(requirement);

      if (priority instanceof Integer) {
        // Convert VP priority constant to string
        return convertVPPriorityToString((Integer) priority);
      } else if (priority instanceof String) {
        return (String) priority;
      }
    } catch (Exception e) {
      // Method not available or failed
    }
    return null;
  }

  /** Extract requirement status using reflection. */
  public static String getRequirementStatus(IModelElement requirement) {
    try {
      java.lang.reflect.Method getStatusMethod = requirement.getClass().getMethod("getStatus");
      Object status = getStatusMethod.invoke(requirement);

      if (status instanceof String) {
        return (String) status;
      }
    } catch (Exception e) {
      // Method not available or failed
    }
    return null;
  }

  /** Convert VP priority constant to string. */
  private static String convertVPPriorityToString(int priority) {
    return switch (priority) {
      case 1 -> "critical";
      case 2 -> "high";
      case 3 -> "medium";
      case 4 -> "low";
      default -> "medium"; // Default
    };
  }

  /** Check if a model element represents a requirement by examining its class structure. */
  public static boolean isRequirementModel(IModelElement element) {
    if (element == null) {
      return false;
    }

    try {
      // Check if the model element is a requirement by examining its class name
      String className = element.getClass().getSimpleName();
      return className.contains("Requirement")
          || className.contains("IRequirement")
          || isRequirementByFactoryType(element);
    } catch (Exception e) {
      return false;
    }
  }

  /** Check if model element is a requirement by checking factory type. */
  private static boolean isRequirementByFactoryType(IModelElement element) {
    try {
      // Try to call requirement-specific methods to detect requirement type
      java.lang.reflect.Method getPriorityMethod = element.getClass().getMethod("getPriority");
      java.lang.reflect.Method getStatusMethod = element.getClass().getMethod("getStatus");

      // If both methods exist and return appropriate types, likely a requirement
      return getPriorityMethod != null && getStatusMethod != null;
    } catch (Exception e) {
      return false;
    }
  }

  /** Sanitize a name for use as project name. */
  public static String sanitizeName(String input) {
    if (input == null || input.trim().isEmpty()) {
      return "Untitled Diagram";
    }
    return input.trim();
  }

  /** Sanitize an ID string for use as identifier. */
  public static String sanitizeId(String input) {
    if (input == null) return "UNKNOWN";

    return input
        .toUpperCase()
        .replaceAll("[^A-Z0-9_]", "_")
        .replaceAll("_{2,}", "_")
        .replaceAll("^_+|_+$", "");
  }

  /** Sanitize a tag string for use as tag. */
  public static String sanitizeTag(String input) {
    if (input == null) return "unknown";

    return input
        .toLowerCase()
        .replaceAll("[^a-z0-9_-]", "_")
        .replaceAll("_{2,}", "_")
        .replaceAll("^_+|_+$", "");
  }
}
