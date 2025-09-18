package com.orgatex.vp.sphinx.action;

import com.orgatex.vp.sphinx.dialog.ImportNeedsToUseCaseDiagramDialog;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import javax.swing.SwingUtilities;

/**
 * Action controller for importing sphinx-needs JSON files into Visual Paradigm use case diagrams.
 */
public class ImportNeedsToUseCaseDiagramActionController implements VPActionController {

  @Override
  public void performAction(VPAction vpAction) {
    // Show import dialog on EDT
    SwingUtilities.invokeLater(
        () -> {
          ImportNeedsToUseCaseDiagramDialog dialog = new ImportNeedsToUseCaseDiagramDialog();
          dialog.setVisible(true);
        });
  }

  @Override
  public void update(VPAction vpAction) {
    // Always enable import action - no specific requirements needed
    vpAction.setEnabled(true);
  }
}
