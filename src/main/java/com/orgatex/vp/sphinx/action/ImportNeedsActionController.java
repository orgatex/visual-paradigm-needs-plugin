package com.orgatex.vp.sphinx.action;

import com.orgatex.vp.sphinx.dialog.UnifiedImportDialog;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import javax.swing.SwingUtilities;

/** Action controller for importing Sphinx-Needs JSON files into Visual Paradigm diagrams. */
public class ImportNeedsActionController implements VPActionController {

  @Override
  public void performAction(VPAction vpAction) {
    SwingUtilities.invokeLater(
        () -> {
          UnifiedImportDialog dialog = new UnifiedImportDialog();
          dialog.setVisible(true);
        });
  }

  @Override
  public void update(VPAction vpAction) {
    vpAction.setEnabled(true);
  }
}
