package com.orgatex.vp.sphinx.action;

import com.orgatex.vp.sphinx.dialog.ExportRequirementsDiagramDialog;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import com.vp.plugin.diagram.IDiagramUIModel;
import javax.swing.SwingUtilities;

/** Action controller for exporting requirements diagrams to Sphinx-Needs JSON format. */
public class ExportRequirementsDiagramActionController implements VPActionController {

  @Override
  public void performAction(VPAction vpAction) {
    // Show export dialog on EDT
    SwingUtilities.invokeLater(
        () -> {
          ExportRequirementsDiagramDialog dialog = new ExportRequirementsDiagramDialog();
          dialog.setVisible(true);
        });
  }

  @Override
  public void update(VPAction vpAction) {
    try {
      // Enable action if a requirements diagram is currently active
      IDiagramUIModel activeDiagram =
          ApplicationManager.instance().getDiagramManager().getActiveDiagram();

      if (activeDiagram != null) {
        // Check if current diagram is a requirements diagram
        boolean isRequirementsDiagram = isRequirementsDiagram(activeDiagram);
        vpAction.setEnabled(isRequirementsDiagram);
      } else {
        vpAction.setEnabled(false);
      }
    } catch (Exception e) {
      // Fallback: always enable if we can't determine diagram type
      vpAction.setEnabled(true);
    }
  }

  /** Check if diagram is a requirements diagram. */
  private boolean isRequirementsDiagram(IDiagramUIModel diagram) {
    if (diagram == null) return false;

    try {
      // Check diagram type using reflection since exact method names may vary
      java.lang.reflect.Method getTypeMethod = diagram.getClass().getMethod("getType");
      Object diagramType = getTypeMethod.invoke(diagram);

      String typeName = diagramType.toString().toLowerCase();
      return typeName.contains("requirement") || typeName.contains("req");
    } catch (Exception e) {
      // Fallback: check diagram name
      String diagramName = diagram.getName();
      if (diagramName != null) {
        String name = diagramName.toLowerCase();
        return name.contains("requirement") || name.contains("req");
      }
    }

    return false;
  }
}
