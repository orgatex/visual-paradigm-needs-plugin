package com.orgatex.vp.sphinx.dialog;

import com.orgatex.vp.sphinx.extractor.RequirementsDiagramExtractor;
import com.orgatex.vp.sphinx.generator.JsonExporter;
import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.diagram.IDiagramUIModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Dialog for exporting requirements diagrams to Sphinx-Needs JSON format. */
public class ExportRequirementsDiagramDialog extends JDialog {

  private JTextField filePathField;
  private JButton browseButton;
  private JButton exportButton;
  private JButton cancelButton;
  private JCheckBox includeDeriveRelationshipsCheckBox;
  private JCheckBox includeContainsRelationshipsCheckBox;
  private JCheckBox includeRefinesRelationshipsCheckBox;
  private JLabel statusLabel;

  public ExportRequirementsDiagramDialog() {
    initializeComponents();
    setupLayout();
    setupEventHandlers();
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    pack();
    setLocationRelativeTo(null);
  }

  private void initializeComponents() {
    setTitle("Export Requirements Diagram to Sphinx-Needs");
    setModal(true);

    filePathField = new JTextField(30);
    browseButton = new JButton("Browse...");
    exportButton = new JButton("Export");
    cancelButton = new JButton("Cancel");

    includeDeriveRelationshipsCheckBox = new JCheckBox("Include derive relationships", true);
    includeContainsRelationshipsCheckBox = new JCheckBox("Include contains relationships", true);
    includeRefinesRelationshipsCheckBox = new JCheckBox("Include refines relationships", true);

    statusLabel = new JLabel(" ");
    statusLabel.setForeground(Color.BLUE);

    // Set default export path
    String defaultPath =
        System.getProperty("user.home") + File.separator + "requirements_export.json";
    filePathField.setText(defaultPath);
  }

  private void setupLayout() {
    setLayout(new BorderLayout());
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

    // Title
    JLabel titleLabel = new JLabel("Export Requirements to Sphinx-Needs JSON");
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
    titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
    mainPanel.add(titleLabel, BorderLayout.NORTH);

    // Content panel
    JPanel contentPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    // File path selection
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(5, 0, 5, 10);
    contentPanel.add(new JLabel("Export to:"), gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(5, 0, 5, 10);
    contentPanel.add(filePathField, gbc);

    gbc.gridx = 2;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.insets = new Insets(5, 0, 5, 0);
    contentPanel.add(browseButton, gbc);

    // Relationship options
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(15, 0, 5, 0);
    contentPanel.add(new JLabel("Include Relationships:"), gbc);

    gbc.gridy = 2;
    gbc.insets = new Insets(5, 20, 5, 0);
    contentPanel.add(includeDeriveRelationshipsCheckBox, gbc);

    gbc.gridy = 3;
    contentPanel.add(includeContainsRelationshipsCheckBox, gbc);

    gbc.gridy = 4;
    contentPanel.add(includeRefinesRelationshipsCheckBox, gbc);

    // Status label
    gbc.gridy = 5;
    gbc.insets = new Insets(15, 0, 0, 0);
    contentPanel.add(statusLabel, gbc);

    mainPanel.add(contentPanel, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
    buttonPanel.add(exportButton);
    buttonPanel.add(cancelButton);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel);
  }

  private void setupEventHandlers() {
    browseButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            chooseExportFile();
          }
        });

    exportButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            performExport();
          }
        });

    cancelButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        });
  }

  private void chooseExportFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));
    fileChooser.setSelectedFile(new File(filePathField.getText()));

    int result = fileChooser.showSaveDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      String filePath = selectedFile.getAbsolutePath();

      // Ensure .json extension
      if (!filePath.toLowerCase().endsWith(".json")) {
        filePath += ".json";
      }

      filePathField.setText(filePath);
    }
  }

  private void performExport() {
    String filePath = filePathField.getText().trim();
    if (filePath.isEmpty()) {
      showError("Please select an export file location.");
      return;
    }

    // Disable controls during export
    setControlsEnabled(false);
    statusLabel.setText("Exporting requirements diagram...");
    statusLabel.setForeground(Color.BLUE);

    // Perform export in background thread
    SwingWorker<Void, Void> worker =
        new SwingWorker<Void, Void>() {
          @Override
          protected Void doInBackground() throws Exception {
            try {
              // Get current active diagram
              IDiagramUIModel activeDiagram =
                  ApplicationManager.instance().getDiagramManager().getActiveDiagram();
              if (activeDiagram == null) {
                throw new Exception(
                    "No active requirements diagram found. Please open a requirements diagram first.");
              }

              // Extract requirements from diagram
              NeedsFile needsFile = RequirementsDiagramExtractor.extractDiagram(activeDiagram);

              // Apply relationship filters based on checkbox selections
              if (!includeDeriveRelationshipsCheckBox.isSelected()) {
                clearDeriveRelationships(needsFile);
              }
              if (!includeContainsRelationshipsCheckBox.isSelected()) {
                clearContainsRelationships(needsFile);
              }
              if (!includeRefinesRelationshipsCheckBox.isSelected()) {
                clearRefinesRelationships(needsFile);
              }

              // Export to JSON file
              File outputFile = new File(filePath);
              JsonExporter.exportToFile(needsFile, outputFile);

              return null;
            } catch (Exception e) {
              throw e;
            }
          }

          @Override
          protected void done() {
            try {
              get(); // Check for exceptions
              statusLabel.setText("Export completed successfully!");
              statusLabel.setForeground(new Color(0, 128, 0));

              // Auto-close dialog after successful export
              Timer timer =
                  new Timer(
                      2000,
                      new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                          dispose();
                        }
                      });
              timer.setRepeats(false);
              timer.start();

            } catch (Exception e) {
              String errorMessage =
                  e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
              showError("Export failed: " + errorMessage);
              statusLabel.setText("Export failed.");
              statusLabel.setForeground(Color.RED);
            } finally {
              setControlsEnabled(true);
            }
          }
        };

    worker.execute();
  }

  private void clearDeriveRelationships(NeedsFile needsFile) {
    if (needsFile.getVersions() != null) {
      for (NeedsFile.VersionData versionData : needsFile.getVersions().values()) {
        if (versionData.getNeeds() != null) {
          for (NeedsFile.Need need : versionData.getNeeds().values()) {
            if (need.getDeriveLinks() != null) {
              need.getDeriveLinks().clear();
            }
          }
        }
      }
    }
  }

  private void clearContainsRelationships(NeedsFile needsFile) {
    if (needsFile.getVersions() != null) {
      for (NeedsFile.VersionData versionData : needsFile.getVersions().values()) {
        if (versionData.getNeeds() != null) {
          for (NeedsFile.Need need : versionData.getNeeds().values()) {
            if (need.getContainsLinks() != null) {
              need.getContainsLinks().clear();
            }
          }
        }
      }
    }
  }

  private void clearRefinesRelationships(NeedsFile needsFile) {
    if (needsFile.getVersions() != null) {
      for (NeedsFile.VersionData versionData : needsFile.getVersions().values()) {
        if (versionData.getNeeds() != null) {
          for (NeedsFile.Need need : versionData.getNeeds().values()) {
            if (need.getRefinesLinks() != null) {
              need.getRefinesLinks().clear();
            }
          }
        }
      }
    }
  }

  private void setControlsEnabled(boolean enabled) {
    filePathField.setEnabled(enabled);
    browseButton.setEnabled(enabled);
    exportButton.setEnabled(enabled);
    includeDeriveRelationshipsCheckBox.setEnabled(enabled);
    includeContainsRelationshipsCheckBox.setEnabled(enabled);
    includeRefinesRelationshipsCheckBox.setEnabled(enabled);
  }

  private void showError(String message) {
    JOptionPane.showMessageDialog(this, message, "Export Error", JOptionPane.ERROR_MESSAGE);
  }
}
