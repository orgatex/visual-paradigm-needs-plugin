package com.orgatex.vp.sphinx.extractor;

import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.diagram.IDiagramUIModel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Basic extractor that creates a minimal needs file from diagram metadata.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BasicDiagramExtractor {

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

        // Create a basic need for the diagram itself
        NeedsFile.Need diagramNeed = new NeedsFile.Need();
        diagramNeed.setId(sanitizeId("DIAGRAM_" + diagram.getName()));
        diagramNeed.setTitle("Diagram: " + diagram.getName());
        diagramNeed.setType("spec");
        diagramNeed.setContent("Use case diagram exported from Visual Paradigm");
        diagramNeed.setStatus("open");
        diagramNeed.setTags("diagram, exported");
        diagramNeed.setElementType("Diagram");

        versionData.addNeed(diagramNeed);
        needsFile.addVersion("1.0", versionData);

        return needsFile;
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
}
