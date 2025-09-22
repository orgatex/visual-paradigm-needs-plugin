package com.orgatex.vp.sphinx.action;

import com.orgatex.vp.sphinx.dialog.ImportNeedsToRequirementsDiagramDialog;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import javax.swing.SwingUtilities;

/**
 * Action controller for importing Sphinx-Needs JSON files into Visual Paradigm requirements
 * diagrams.
 */
public class ImportNeedsToRequirementsDiagramActionController implements VPActionController {

  @Override
  public void performAction(VPAction vpAction) {
    // Show import dialog on EDT
    SwingUtilities.invokeLater(
        () -> {
          ImportNeedsToRequirementsDiagramDialog dialog =
              new ImportNeedsToRequirementsDiagramDialog();
          dialog.setVisible(true);
        });
  }

  @Override
  public void update(VPAction vpAction) {
    // Always enable requirements import action - no specific requirements needed
    vpAction.setEnabled(true);
  }
}
