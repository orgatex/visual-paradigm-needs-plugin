package com.orgatex.vp.sphinx.importer;

import com.orgatex.vp.sphinx.model.NeedsFile;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Layout engine for positioning elements in a use case diagram.
 */
public class ElementLayoutEngine {

    // Layout configuration
    private static final int ELEMENT_WIDTH = 120;
    private static final int ELEMENT_HEIGHT = 80;
    private static final int HORIZONTAL_SPACING = 40;
    private static final int VERTICAL_SPACING = 30;
    private static final int MARGIN = 50;

    /**
     * Calculate layout positions for all needs.
     *
     * @param needs Map of needs to position
     * @return Map of need IDs to positions
     */
    public Map<String, Point> calculateLayout(Map<String, NeedsFile.Need> needs) {
        Map<String, Point> positions = new HashMap<>();

        // Separate use cases and actors
        List<String> useCaseIds = new ArrayList<>();
        List<String> actorIds = new ArrayList<>();

        for (Map.Entry<String, NeedsFile.Need> entry : needs.entrySet()) {
            String needId = entry.getKey();
            NeedsFile.Need need = entry.getValue();

            if (isUseCaseNeed(need)) {
                useCaseIds.add(needId);
            } else if (isActorNeed(need)) {
                actorIds.add(needId);
            }
        }

        // Calculate grid dimensions for use cases
        int useCaseCount = useCaseIds.size();
        GridDimensions useCaseGrid = calculateGridDimensions(useCaseCount);

        // Position use cases in center area
        positionUseCasesInGrid(positions, useCaseIds, useCaseGrid);

        // Position actors around the perimeter
        positionActorsAroundPerimeter(positions, actorIds, useCaseGrid);

        return positions;
    }

    /**
     * Position use cases in a grid layout.
     */
    private void positionUseCasesInGrid(Map<String, Point> positions, List<String> useCaseIds, GridDimensions grid) {
        int startX = MARGIN + 200; // Leave space for actors on the left
        int startY = MARGIN + 100; // Leave space for actors on top

        int index = 0;
        for (String useCaseId : useCaseIds) {
            int row = index / grid.columns;
            int col = index % grid.columns;

            int x = startX + col * (ELEMENT_WIDTH + HORIZONTAL_SPACING);
            int y = startY + row * (ELEMENT_HEIGHT + VERTICAL_SPACING);

            positions.put(useCaseId, new Point(x, y));
            index++;
        }
    }

    /**
     * Position actors around the perimeter of the use case area.
     */
    private void positionActorsAroundPerimeter(Map<String, Point> positions, List<String> actorIds, GridDimensions useCaseGrid) {
        if (actorIds.isEmpty()) {
            return;
        }

        // Calculate the boundaries of the use case area
        int useCaseAreaStartX = MARGIN + 200;
        int useCaseAreaStartY = MARGIN + 100;
        int useCaseAreaWidth = useCaseGrid.columns * (ELEMENT_WIDTH + HORIZONTAL_SPACING) - HORIZONTAL_SPACING;
        int useCaseAreaHeight = useCaseGrid.rows * (ELEMENT_HEIGHT + VERTICAL_SPACING) - VERTICAL_SPACING;

        // Position actors around the perimeter
        int actorIndex = 0;
        for (String actorId : actorIds) {
            Point actorPosition = calculateActorPosition(actorIndex, actorIds.size(),
                                                       useCaseAreaStartX, useCaseAreaStartY,
                                                       useCaseAreaWidth, useCaseAreaHeight);
            positions.put(actorId, actorPosition);
            actorIndex++;
        }
    }

    /**
     * Calculate position for an actor around the perimeter.
     */
    private Point calculateActorPosition(int actorIndex, int totalActors,
                                       int useCaseStartX, int useCaseStartY,
                                       int useCaseWidth, int useCaseHeight) {

        // Distribute actors around the perimeter (left, top, right, bottom)
        int perimeter = 2 * useCaseWidth + 2 * useCaseHeight;
        double position = (double) actorIndex / totalActors * perimeter;

        // Left side
        if (position < useCaseHeight) {
            return new Point(useCaseStartX - 150,
                           useCaseStartY + (int) position);
        }

        position -= useCaseHeight;

        // Top side
        if (position < useCaseWidth) {
            return new Point(useCaseStartX + (int) position,
                           useCaseStartY - 120);
        }

        position -= useCaseWidth;

        // Right side
        if (position < useCaseHeight) {
            return new Point(useCaseStartX + useCaseWidth + 50,
                           useCaseStartY + (int) position);
        }

        position -= useCaseHeight;

        // Bottom side
        return new Point(useCaseStartX + (int) position,
                        useCaseStartY + useCaseHeight + 50);
    }

    /**
     * Calculate optimal grid dimensions for a given number of elements.
     */
    private GridDimensions calculateGridDimensions(int elementCount) {
        if (elementCount == 0) {
            return new GridDimensions(1, 1);
        }

        // Try to create a roughly square grid
        int columns = (int) Math.ceil(Math.sqrt(elementCount));
        int rows = (int) Math.ceil((double) elementCount / columns);

        // Adjust for better aspect ratio if needed
        if (columns > rows * 2) {
            // Too wide, make it taller
            columns = (int) Math.ceil(Math.sqrt(elementCount * 1.5));
            rows = (int) Math.ceil((double) elementCount / columns);
        }

        return new GridDimensions(rows, columns);
    }

    /**
     * Check if a need represents a use case.
     */
    private boolean isUseCaseNeed(NeedsFile.Need need) {
        return "uc".equals(need.getType()) || "UseCase".equals(need.getElementType());
    }

    /**
     * Check if a need represents an actor.
     */
    private boolean isActorNeed(NeedsFile.Need need) {
        return "actor".equals(need.getType()) || "Actor".equals(need.getElementType());
    }

    /**
     * Simple class to hold grid dimensions.
     */
    private static class GridDimensions {
        final int rows;
        final int columns;

        GridDimensions(int rows, int columns) {
            this.rows = rows;
            this.columns = columns;
        }
    }
}
