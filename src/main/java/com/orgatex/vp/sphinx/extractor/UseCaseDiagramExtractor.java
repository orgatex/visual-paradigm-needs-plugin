package com.orgatex.vp.sphinx.extractor;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.shape.IUseCaseUIModel;
import com.vp.plugin.diagram.shape.IActorUIModel;
import com.vp.plugin.diagram.connector.IIncludeUIModel;
import com.vp.plugin.diagram.connector.IExtendUIModel;
import com.vp.plugin.model.IModelElement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enhanced extractor that attempts to extract actual use cases from Visual Paradigm diagrams.
 */
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

        // Extract use cases and actors from diagram elements
        extractUseCasesAndActors(diagram, versionData);

        // Extract relationships (include/extend)
        extractRelationships(diagram, versionData);

        needsFile.addVersion("1.0", versionData);
        return needsFile;
    }

    private static void extractUseCasesAndActors(IDiagramUIModel diagram, NeedsFile.VersionData versionData) {
        try {
            // Get all diagram elements using the correct API
            IDiagramElement[] diagramElements = diagram.toDiagramElementArray();

            // Maps to track actors for each use case
            Map<String, Set<String>> useCaseActors = new HashMap<>();
            Map<String, String> actorNames = new HashMap<>(); // actorId -> actorName

            // First pass: collect all actors
            for (IDiagramElement element : diagramElements) {
                if (element instanceof IActorUIModel) {
                    IActorUIModel actorUI = (IActorUIModel) element;
                    IModelElement actorModel = actorUI.getModelElement();
                    if (actorModel != null) {
                        actorNames.put(actorModel.getId(), actorModel.getName());
                    }
                }
            }

            // Second pass: process use cases and link with actors
            for (IDiagramElement element : diagramElements) {
                if (element instanceof IUseCaseUIModel) {
                    IUseCaseUIModel useCaseUI = (IUseCaseUIModel) element;
                    IModelElement useCaseModel = useCaseUI.getModelElement();
                    if (useCaseModel != null) {
                        // Find associated actors (this will be enhanced when we process relationships)
                        Set<String> associatedActors = findAssociatedActors(useCaseModel, actorNames);
                        useCaseActors.put(useCaseModel.getId(), associatedActors);

                        // Create the use case need
                        processUseCase(useCaseModel, versionData, associatedActors);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error extracting use cases and actors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void extractRelationships(IDiagramUIModel diagram, NeedsFile.VersionData versionData) {
        try {
            // Get all diagram elements to find connectors
            IDiagramElement[] diagramElements = diagram.toDiagramElementArray();

            // Maps to track relationships
            Map<String, Set<String>> includeRelationships = new HashMap<>();
            Map<String, Set<String>> extendRelationships = new HashMap<>();

            for (IDiagramElement element : diagramElements) {
                if (element instanceof IIncludeUIModel) {
                    IIncludeUIModel includeUI = (IIncludeUIModel) element;
                    processIncludeRelationship(includeUI, includeRelationships);
                } else if (element instanceof IExtendUIModel) {
                    IExtendUIModel extendUI = (IExtendUIModel) element;
                    processExtendRelationship(extendUI, extendRelationships);
                }
            }

            // Apply relationships to use cases
            applyRelationshipsToNeeds(versionData, includeRelationships, extendRelationships);

        } catch (Exception e) {
            System.err.println("Error extracting relationships: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processUseCase(IModelElement useCase, NeedsFile.VersionData versionData,
                                     Set<String> associatedActors) {
        String name = useCase.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Unnamed Use Case";
        }

        String id = sanitizeId("UC_" + name);

        // Avoid duplicates by checking for existing ID or appending number
        String finalId = id;
        int counter = 1;
        while (versionData.getNeeds().containsKey(finalId)) {
            finalId = id + "_" + counter;
            counter++;
        }

        NeedsFile.Need need = new NeedsFile.Need(finalId, name, "req");
        need.setContent(getDescription(useCase));
        need.setStatus("open");
        need.setSourceId(useCase.getId());
        need.setElementType("UseCase");

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

        need.setTags(String.join(", ", tags));

        versionData.addNeed(need);
    }

    private static Set<String> findAssociatedActors(IModelElement useCase, Map<String, String> actorNames) {
        Set<String> associatedActors = new HashSet<>();

        try {
            // Try to get relationships from the use case model element
            if (useCase != null) {
                // Use reflection to get relationships
                try {
                    java.lang.reflect.Method getFromRelationshipsMethod = useCase.getClass().getMethod("getFromRelationships");
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
                        java.lang.reflect.Method getToRelationshipsMethod = useCase.getClass().getMethod("getToRelationships");
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

    private static void processIncludeRelationship(IIncludeUIModel includeUI,
                                                  Map<String, Set<String>> includeRelationships) {
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

    private static void processExtendRelationship(IExtendUIModel extendUI,
                                                 Map<String, Set<String>> extendRelationships) {
        try {
            // Get the model element of the extend relationship
            IModelElement extendModel = extendUI.getModelElement();
            if (extendModel != null) {
                String fromId = getFromElementId(extendUI);
                String toId = getToElementId(extendUI);

                if (fromId != null && toId != null) {
                    extendRelationships.computeIfAbsent(fromId, k -> new HashSet<>()).add(toId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing extend relationship: " + e.getMessage());
        }
    }

    private static String getFromElementId(Object connector) {
        try {
            // Use reflection to get the from element, as the exact API method may vary
            java.lang.reflect.Method getFromMethod = connector.getClass().getMethod("getFrom");
            Object fromElement = getFromMethod.invoke(connector);

            if (fromElement instanceof IUseCaseUIModel) {
                IUseCaseUIModel useCaseUI = (IUseCaseUIModel) fromElement;
                IModelElement model = useCaseUI.getModelElement();
                return model != null ? model.getId() : null;
            }
        } catch (Exception e) {
            // Try alternative method names or approaches
            try {
                java.lang.reflect.Method getFromShapeMethod = connector.getClass().getMethod("getFromShape");
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

            if (toElement instanceof IUseCaseUIModel) {
                IUseCaseUIModel useCaseUI = (IUseCaseUIModel) toElement;
                IModelElement model = useCaseUI.getModelElement();
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
        if (shape instanceof IUseCaseUIModel) {
            IUseCaseUIModel useCaseUI = (IUseCaseUIModel) shape;
            IModelElement model = useCaseUI.getModelElement();
            return model != null ? model.getId() : null;
        }
        return null;
    }

    private static void applyRelationshipsToNeeds(NeedsFile.VersionData versionData,
                                                 Map<String, Set<String>> includeRelationships,
                                                 Map<String, Set<String>> extendRelationships) {
        // Create a map from source element ID to need ID for quick lookup
        Map<String, String> sourceIdToNeedId = new HashMap<>();
        for (NeedsFile.Need need : versionData.getNeeds().values()) {
            if (need.getSourceId() != null) {
                sourceIdToNeedId.put(need.getSourceId(), need.getId());
            }
        }

        // Apply include relationships
        for (Map.Entry<String, Set<String>> entry : includeRelationships.entrySet()) {
            String sourceElementId = entry.getKey();
            String sourceNeedId = sourceIdToNeedId.get(sourceElementId);

            if (sourceNeedId != null) {
                NeedsFile.Need sourceNeed = versionData.getNeeds().get(sourceNeedId);
                Set<String> targetElementIds = entry.getValue();
                List<String> linkedNeedIds = new ArrayList<>();

                for (String targetElementId : targetElementIds) {
                    String targetNeedId = sourceIdToNeedId.get(targetElementId);
                    if (targetNeedId != null) {
                        linkedNeedIds.add(targetNeedId);
                    }
                }

                if (!linkedNeedIds.isEmpty()) {
                    String existingLinks = sourceNeed.getLinks();
                    if (existingLinks != null && !existingLinks.trim().isEmpty()) {
                        linkedNeedIds.add(0, existingLinks);
                    }
                    sourceNeed.setLinks(String.join(", ", linkedNeedIds));
                }
            }
        }

        // Apply extend relationships (similar to include)
        for (Map.Entry<String, Set<String>> entry : extendRelationships.entrySet()) {
            String sourceElementId = entry.getKey();
            String sourceNeedId = sourceIdToNeedId.get(sourceElementId);

            if (sourceNeedId != null) {
                NeedsFile.Need sourceNeed = versionData.getNeeds().get(sourceNeedId);
                Set<String> targetElementIds = entry.getValue();
                List<String> linkedNeedIds = new ArrayList<>();

                for (String targetElementId : targetElementIds) {
                    String targetNeedId = sourceIdToNeedId.get(targetElementId);
                    if (targetNeedId != null) {
                        linkedNeedIds.add(targetNeedId);
                    }
                }

                if (!linkedNeedIds.isEmpty()) {
                    String existingLinks = sourceNeed.getLinks();
                    if (existingLinks != null && !existingLinks.trim().isEmpty()) {
                        linkedNeedIds.add(0, existingLinks);
                    }
                    sourceNeed.setLinks(String.join(", ", linkedNeedIds));
                }
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

    private static String sanitizeName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Untitled Diagram";
        }
        return input.trim();
    }

    private static String sanitizeId(String input) {
        if (input == null) return "UNKNOWN";

        return input.toUpperCase()
                   .replaceAll("[^A-Z0-9_]", "_")
                   .replaceAll("_{2,}", "_")
                   .replaceAll("^_+|_+$", "");
    }

    private static String sanitizeTag(String input) {
        if (input == null) return "unknown";

        return input.toLowerCase()
                   .replaceAll("[^a-z0-9_-]", "_")
                   .replaceAll("_{2,}", "_")
                   .replaceAll("^_+|_+$", "");
    }
}
