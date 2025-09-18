package com.orgatex.vp.sphinx.action;

import com.orgatex.vp.sphinx.dialog.ExportDiagramToSphinxDialog;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import com.vp.plugin.diagram.IDiagramUIModel;
import javax.swing.SwingUtilities;

/** Action controller for exporting use case diagrams to Sphinx-Needs JSON format. */
public class ExportDiagramToSphinxActionController implements VPActionController {

  @Override
  public void performAction(VPAction vpAction) {
    // Get active diagram
    IDiagramUIModel diagram = ApplicationManager.instance().getDiagramManager().getActiveDiagram();

    if (diagram == null) {
      ApplicationManager.instance()
          .getViewManager()
          .showMessage("No active diagram found. Please open a diagram before exporting.");
      return;
    }

    // Show export dialog on EDT (removing diagram type check for now)
    SwingUtilities.invokeLater(
        () -> {
          ExportDiagramToSphinxDialog dialog = new ExportDiagramToSphinxDialog(diagram);
          dialog.setVisible(true);
        });
  }

  @Override
  public void update(VPAction vpAction) {
    // Enable action if there's an active diagram
    IDiagramUIModel diagram = ApplicationManager.instance().getDiagramManager().getActiveDiagram();
    vpAction.setEnabled(diagram != null);
  }
}
