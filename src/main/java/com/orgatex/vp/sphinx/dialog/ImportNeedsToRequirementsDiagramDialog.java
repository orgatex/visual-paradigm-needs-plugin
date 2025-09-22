package com.orgatex.vp.sphinx.dialog;

import com.orgatex.vp.sphinx.importer.NeedsFileImporter;
import com.orgatex.vp.sphinx.importer.RequirementsDiagramBuilder;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.diagram.IDiagramUIModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Dialog for importing Sphinx-Needs JSON files into requirements diagrams. */
public class ImportNeedsToRequirementsDiagramDialog extends JDialog {

  private JTextField filePathField;
  private JButton browseButton;
  private JButton importButton;
  private JButton cancelButton;
  private JCheckBox createNewDiagramCheckBox;
  private JComboBox<String> layoutComboBox;
  private JLabel statusLabel;

  public ImportNeedsToRequirementsDiagramDialog() {
    initializeComponents();
    setupLayout();
    setupEventHandlers();
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    pack();
    setLocationRelativeTo(null);
  }

  private void initializeComponents() {
    setTitle("Import Sphinx-Needs to Requirements Diagram");
    setModal(true);

    filePathField = new JTextField(30);
    browseButton = new JButton("Browse...");
    importButton = new JButton("Import");
    cancelButton = new JButton("Cancel");

    createNewDiagramCheckBox = new JCheckBox("Create new requirements diagram", true);

    layoutComboBox =
        new JComboBox<>(
            new String[] {
              "Organic Layout", "Hierarchical Layout", "Grid Layout", "No Auto Layout"
            });
    layoutComboBox.setSelectedIndex(0);

    statusLabel = new JLabel(" ");
    statusLabel.setForeground(Color.BLUE);
  }

  private void setupLayout() {
    setLayout(new BorderLayout());
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

    // Title
    JLabel titleLabel = new JLabel("Import Sphinx-Needs to Requirements Diagram");
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
    contentPanel.add(new JLabel("JSON File:"), gbc);

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

    // Import options
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(15, 0, 5, 0);
    contentPanel.add(new JLabel("Import Options:"), gbc);

    gbc.gridy = 2;
    gbc.insets = new Insets(5, 20, 5, 0);
    contentPanel.add(createNewDiagramCheckBox, gbc);

    // Layout selection
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.insets = new Insets(10, 20, 5, 10);
    contentPanel.add(new JLabel("Layout:"), gbc);

    gbc.gridx = 1;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(10, 0, 5, 0);
    contentPanel.add(layoutComboBox, gbc);

    // Status label
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 3;
    gbc.insets = new Insets(15, 0, 0, 0);
    contentPanel.add(statusLabel, gbc);

    mainPanel.add(contentPanel, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
    buttonPanel.add(importButton);
    buttonPanel.add(cancelButton);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel);
  }

  private void setupEventHandlers() {
    browseButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            chooseImportFile();
          }
        });

    importButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            performImport();
          }
        });

    cancelButton.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        });

    createNewDiagramCheckBox.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            layoutComboBox.setEnabled(createNewDiagramCheckBox.isSelected());
          }
        });
  }

  private void chooseImportFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));

    if (!filePathField.getText().trim().isEmpty()) {
      fileChooser.setSelectedFile(new File(filePathField.getText()));
    }

    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      filePathField.setText(selectedFile.getAbsolutePath());
    }
  }

  private void performImport() {
    String filePath = filePathField.getText().trim();
    if (filePath.isEmpty()) {
      showError("Please select a JSON file to import.");
      return;
    }

    File importFile = new File(filePath);
    if (!importFile.exists()) {
      showError("The selected file does not exist.");
      return;
    }

    // Disable controls during import
    setControlsEnabled(false);
    statusLabel.setText("Importing requirements from JSON file...");
    statusLabel.setForeground(Color.BLUE);

    // Perform import in background thread
    SwingWorker<IDiagramUIModel, Void> worker =
        new SwingWorker<IDiagramUIModel, Void>() {
          @Override
          protected IDiagramUIModel doInBackground() throws Exception {
            try {
              if (createNewDiagramCheckBox.isSelected()) {
                // Create new requirements diagram
                return importToNewRequirementsDiagram(importFile);
              } else {
                // Import to existing use case diagram (for backward compatibility)
                NeedsFileImporter importer = new NeedsFileImporter();
                return importer.importFromFile(importFile);
              }
            } catch (Exception e) {
              throw e;
            }
          }

          @Override
          protected void done() {
            try {
              IDiagramUIModel diagram = get(); // Check for exceptions and get result
              statusLabel.setText("Import completed successfully!");
              statusLabel.setForeground(new Color(0, 128, 0));

              // Apply layout if requested
              if (createNewDiagramCheckBox.isSelected() && layoutComboBox.getSelectedIndex() < 3) {
                applyLayout(diagram, layoutComboBox.getSelectedIndex());
              }

              // Auto-close dialog after successful import
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
              showError("Import failed: " + errorMessage);
              statusLabel.setText("Import failed.");
              statusLabel.setForeground(Color.RED);
            } finally {
              setControlsEnabled(true);
            }
          }
        };

    worker.execute();
  }

  private IDiagramUIModel importToNewRequirementsDiagram(File jsonFile) throws Exception {
    // Create requirements diagram builder
    RequirementsDiagramBuilder builder = new RequirementsDiagramBuilder();

    // Create new requirements diagram
    String diagramName = createDiagramName(jsonFile.getName());
    IDiagramUIModel diagram = builder.createRequirementsDiagram(diagramName);

    // Parse JSON file to extract needs
    com.fasterxml.jackson.databind.ObjectMapper objectMapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    com.orgatex.vp.sphinx.model.NeedsFile needsFile =
        objectMapper.readValue(jsonFile, com.orgatex.vp.sphinx.model.NeedsFile.class);

    // Get current version data
    String currentVersion = needsFile.getCurrentVersion();
    com.orgatex.vp.sphinx.model.NeedsFile.VersionData versionData =
        needsFile.getVersions().get(currentVersion);

    if (versionData == null || versionData.getNeeds() == null) {
      throw new Exception("No requirements found in JSON file");
    }

    // Create requirement elements in diagram
    builder.createRequirementElements(diagram, versionData.getNeeds());

    // Create relationships
    builder.createRequirementRelationships(diagram, versionData.getNeeds());

    // Open the diagram
    ApplicationManager.instance().getDiagramManager().openDiagram(diagram);

    return diagram;
  }

  private String createDiagramName(String fileName) {
    String baseName = "Imported Requirements";

    // Remove .json extension if present
    if (fileName.toLowerCase().endsWith(".json")) {
      fileName = fileName.substring(0, fileName.length() - 5);
    }

    return baseName + " (" + fileName + ")";
  }

  private void applyLayout(IDiagramUIModel diagram, int layoutType) {
    try {
      switch (layoutType) {
        case 0: // Organic Layout
          ApplicationManager.instance()
              .getDiagramManager()
              .layout(diagram, ApplicationManager.instance().getDiagramManager().LAYOUT_ORGANIC);
          break;
        case 1: // Hierarchical Layout
          // Use reflection to try hierarchical layout
          tryLayoutWithReflection(diagram, "LAYOUT_HIERARCHICAL");
          break;
        case 2: // Grid Layout
          // Use reflection to try grid layout
          tryLayoutWithReflection(diagram, "LAYOUT_GRID");
          break;
        case 3: // No Auto Layout
        default:
          // Don't apply any layout
          break;
      }
    } catch (Exception e) {
      System.err.println("Error applying layout: " + e.getMessage());
    }
  }

  private void setControlsEnabled(boolean enabled) {
    filePathField.setEnabled(enabled);
    browseButton.setEnabled(enabled);
    importButton.setEnabled(enabled);
    createNewDiagramCheckBox.setEnabled(enabled);
    layoutComboBox.setEnabled(enabled && createNewDiagramCheckBox.isSelected());
  }

  private void tryLayoutWithReflection(IDiagramUIModel diagram, String layoutConstantName) {
    try {
      // Try to get layout constant using reflection
      java.lang.reflect.Field layoutField =
          ApplicationManager.instance().getDiagramManager().getClass().getField(layoutConstantName);
      Object layoutConstant = layoutField.get(ApplicationManager.instance().getDiagramManager());
      // Cast to appropriate type based on VP API expectations
      if (layoutConstant instanceof Integer) {
        ApplicationManager.instance().getDiagramManager().layout(diagram, (Integer) layoutConstant);
      } else if (layoutConstant instanceof String) {
        // Try string-based layout method
        java.lang.reflect.Method layoutMethod =
            ApplicationManager.instance()
                .getDiagramManager()
                .getClass()
                .getMethod("layout", IDiagramUIModel.class, String.class);
        layoutMethod.invoke(
            ApplicationManager.instance().getDiagramManager(), diagram, layoutConstant);
      }
    } catch (Exception e) {
      // Fallback to organic layout if specific layout not available
      System.err.println("Layout " + layoutConstantName + " not available, using organic layout");
      try {
        ApplicationManager.instance()
            .getDiagramManager()
            .layout(diagram, ApplicationManager.instance().getDiagramManager().LAYOUT_ORGANIC);
      } catch (Exception e2) {
        System.err.println("Error applying fallback layout: " + e2.getMessage());
      }
    }
  }

  private void showError(String message) {
    JOptionPane.showMessageDialog(this, message, "Import Error", JOptionPane.ERROR_MESSAGE);
  }
}
