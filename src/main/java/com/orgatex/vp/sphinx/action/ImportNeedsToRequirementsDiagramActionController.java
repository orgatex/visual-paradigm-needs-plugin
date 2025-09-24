package com.orgatex.vp.sphinx.action;

import com.orgatex.vp.sphinx.dialog.UnifiedImportDialog;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import javax.swing.SwingUtilities;

/** Action controller for importing Sphinx-Needs JSON files into Visual Paradigm diagrams. */
public class ImportNeedsToRequirementsDiagramActionController implements VPActionController {

  @Override
  public void performAction(VPAction vpAction) {
    // Show unified import dialog on EDT
    SwingUtilities.invokeLater(
        () -> {
          UnifiedImportDialog dialog = new UnifiedImportDialog();
          dialog.setVisible(true);
        });
  }

  @Override
  public void update(VPAction vpAction) {
    // Always enable import action - no specific requirements needed
    vpAction.setEnabled(true);
  }
}
