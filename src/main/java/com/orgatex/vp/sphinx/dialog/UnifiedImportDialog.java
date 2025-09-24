package com.orgatex.vp.sphinx.dialog;

import com.orgatex.vp.sphinx.importer.NeedsFileImporter;
import com.orgatex.vp.sphinx.importer.RequirementsDiagramBuilder;
import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.project.IUserPath;
import com.vp.plugin.project.IUserPathOptions;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Unified dialog for importing sphinx-needs JSON files into Visual Paradigm diagrams. Allows
 * choosing diagram type (use case or requirements) and element types to import.
 */
public class UnifiedImportDialog extends JDialog {

  private static final String USER_PATH_NAME = "sphinx_needs_import_source";

  private JTextField filePathField;
  private JButton browseButton;
  private JButton importButton;
  private JButton cancelButton;

  private JRadioButton useCaseDiagramRadio;
  private JRadioButton requirementsDiagramRadio;
  private ButtonGroup diagramTypeGroup;

  private JCheckBox importUseCasesCheckBox;
  private JCheckBox importActorsCheckBox;
  private JCheckBox importRequirementsCheckBox;

  private JComboBox<String> layoutComboBox;
  private JLabel statusLabel;

  public UnifiedImportDialog() {
    super((JFrame) null, "Import from Sphinx-Needs", true);
    initializeComponents();
    layoutComponents();
    setupEventHandlers();
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    pack();
    setLocationRelativeTo(null);
  }

  private void initializeComponents() {
    filePathField = new JTextField(40);
    browseButton = new JButton("Browse...");
    importButton = new JButton("Import");
    cancelButton = new JButton("Cancel");

    useCaseDiagramRadio = new JRadioButton("Use Case Diagram", true);
    requirementsDiagramRadio = new JRadioButton("Requirements Diagram", false);
    diagramTypeGroup = new ButtonGroup();
    diagramTypeGroup.add(useCaseDiagramRadio);
    diagramTypeGroup.add(requirementsDiagramRadio);

    importUseCasesCheckBox = new JCheckBox("Use Cases", true);
    importActorsCheckBox = new JCheckBox("Actors", true);
    importRequirementsCheckBox = new JCheckBox("Requirements", true);

    layoutComboBox =
        new JComboBox<>(
            new String[] {
              "Organic Layout", "Hierarchical Layout", "Grid Layout", "No Auto Layout"
            });
    layoutComboBox.setSelectedIndex(0);

    statusLabel = new JLabel(" ");
    statusLabel.setForeground(Color.BLUE);

    // Set input file path from saved preference or default
    String savedPath = getSavedImportSource();
    if (savedPath != null && !savedPath.isEmpty()) {
      filePathField.setText(savedPath);
    } else {
      filePathField.setText(System.getProperty("user.home"));
    }
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());

    // Header
    JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JLabel titleLabel = new JLabel("Import sphinx-needs JSON file");
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
    headerPanel.add(titleLabel);
    add(headerPanel, BorderLayout.NORTH);

    // Main content
    JPanel contentPanel = new JPanel(new GridBagLayout());
    contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    GridBagConstraints gbc = new GridBagConstraints();

    // File selection section
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, 0, 10, 0);
    contentPanel.add(new JLabel("Input JSON file:"), gbc);

    JPanel filePanel = new JPanel(new BorderLayout(5, 0));
    filePanel.add(filePathField, BorderLayout.CENTER);
    filePanel.add(browseButton, BorderLayout.EAST);

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(0, 0, 20, 0);
    contentPanel.add(filePanel, gbc);

    // Diagram type section
    JPanel diagramTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    diagramTypePanel.setBorder(BorderFactory.createTitledBorder("Diagram Type"));
    diagramTypePanel.add(useCaseDiagramRadio);
    diagramTypePanel.add(requirementsDiagramRadio);

    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 0, 15, 0);
    contentPanel.add(diagramTypePanel, gbc);

    // Element selection section
    JPanel elementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    elementPanel.setBorder(BorderFactory.createTitledBorder("Elements to Import"));
    elementPanel.add(importUseCasesCheckBox);
    elementPanel.add(importActorsCheckBox);
    elementPanel.add(importRequirementsCheckBox);

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.insets = new Insets(0, 0, 15, 0);
    contentPanel.add(elementPanel, gbc);

    // Layout selection section
    JPanel layoutPanel = new JPanel(new BorderLayout());
    layoutPanel.setBorder(BorderFactory.createTitledBorder("Layout"));
    layoutPanel.add(layoutComboBox, BorderLayout.CENTER);

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.insets = new Insets(0, 0, 15, 0);
    contentPanel.add(layoutPanel, gbc);

    // Status label
    gbc.gridx = 0;
    gbc.gridy = 5;
    gbc.insets = new Insets(10, 0, 0, 0);
    contentPanel.add(statusLabel, gbc);

    add(contentPanel, BorderLayout.CENTER);

    // Button panel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
    buttonPanel.add(importButton);
    buttonPanel.add(cancelButton);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  private void setupEventHandlers() {
    browseButton.addActionListener(this::onBrowseClicked);
    importButton.addActionListener(this::onImportClicked);
    cancelButton.addActionListener(this::onCancelClicked);

    // Update UI based on diagram type selection
    useCaseDiagramRadio.addActionListener(e -> updateElementCheckboxes());
    requirementsDiagramRadio.addActionListener(e -> updateElementCheckboxes());

    // Enable/disable import button based on file selection
    filePathField
        .getDocument()
        .addDocumentListener(
            new javax.swing.event.DocumentListener() {
              public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateImportButtonState();
              }

              public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateImportButtonState();
              }

              public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateImportButtonState();
              }
            });

    updateImportButtonState();
    updateElementCheckboxes();
  }

  private void updateElementCheckboxes() {
    if (useCaseDiagramRadio.isSelected()) {
      importUseCasesCheckBox.setEnabled(true);
      importActorsCheckBox.setEnabled(true);
      importRequirementsCheckBox.setEnabled(false);
      importRequirementsCheckBox.setSelected(false);
    } else {
      importUseCasesCheckBox.setEnabled(false);
      importUseCasesCheckBox.setSelected(false);
      importActorsCheckBox.setEnabled(false);
      importActorsCheckBox.setSelected(false);
      importRequirementsCheckBox.setEnabled(true);
      importRequirementsCheckBox.setSelected(true);
    }
  }

  private void onBrowseClicked(ActionEvent e) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Select Sphinx-Needs JSON file");
    fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));

    String currentPath = filePathField.getText().trim();
    if (!currentPath.isEmpty()) {
      File currentFile = new File(currentPath);
      if (currentFile.exists()) {
        if (currentFile.isDirectory()) {
          fileChooser.setCurrentDirectory(currentFile);
        } else {
          fileChooser.setSelectedFile(currentFile);
        }
      }
    }

    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      filePathField.setText(selectedFile.getAbsolutePath());
    }
  }

  private void onImportClicked(ActionEvent e) {
    String inputPath = filePathField.getText().trim();
    if (inputPath.isEmpty()) {
      showError("Please select an input file.");
      return;
    }

    File inputFile = new File(inputPath);
    if (!inputFile.exists()) {
      showError("Input file does not exist: " + inputPath);
      return;
    }

    if (!inputFile.isFile()) {
      showError("Input path is not a file: " + inputPath);
      return;
    }

    if (!importUseCasesCheckBox.isSelected()
        && !importActorsCheckBox.isSelected()
        && !importRequirementsCheckBox.isSelected()) {
      showError("Please select at least one element type to import.");
      return;
    }

    saveImportSource(inputPath);
    performImport(inputFile);
  }

  private void onCancelClicked(ActionEvent e) {
    dispose();
  }

  private void performImport(File inputFile) {
    setControlsEnabled(false);
    statusLabel.setText("Importing from JSON file...");
    statusLabel.setForeground(Color.BLUE);

    SwingWorker<IDiagramUIModel, Void> worker =
        new SwingWorker<IDiagramUIModel, Void>() {
          @Override
          protected IDiagramUIModel doInBackground() throws Exception {
            if (requirementsDiagramRadio.isSelected()) {
              return importToRequirementsDiagram(inputFile);
            } else {
              return importToUseCaseDiagram(inputFile);
            }
          }

          @Override
          protected void done() {
            try {
              IDiagramUIModel diagram = get();
              statusLabel.setText("Import completed successfully!");
              statusLabel.setForeground(new Color(0, 128, 0));

              if (layoutComboBox.getSelectedIndex() < 3) {
                applyLayout(diagram, layoutComboBox.getSelectedIndex());
              }

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

  private IDiagramUIModel importToUseCaseDiagram(File jsonFile) throws Exception {
    NeedsFileImporter importer = new NeedsFileImporter();
    return importer.importFromFile(jsonFile);
  }

  private IDiagramUIModel importToRequirementsDiagram(File jsonFile) throws Exception {
    RequirementsDiagramBuilder builder = new RequirementsDiagramBuilder();

    String diagramName = createDiagramName(jsonFile.getName());
    IDiagramUIModel diagram = builder.createRequirementsDiagram(diagramName);

    com.fasterxml.jackson.databind.ObjectMapper objectMapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    NeedsFile needsFile = objectMapper.readValue(jsonFile, NeedsFile.class);

    String currentVersion = needsFile.getCurrentVersion();
    NeedsFile.VersionData versionData = needsFile.getVersions().get(currentVersion);

    if (versionData == null || versionData.getNeeds() == null) {
      throw new Exception("No requirements found in JSON file");
    }

    builder.createRequirementElements(diagram, versionData.getNeeds());
    builder.createRequirementRelationships(diagram, versionData.getNeeds());

    ApplicationManager.instance().getDiagramManager().openDiagram(diagram);
    return diagram;
  }

  private String createDiagramName(String fileName) {
    String baseName =
        requirementsDiagramRadio.isSelected() ? "Imported Requirements" : "Imported Use Cases";
    if (fileName.toLowerCase().endsWith(".json")) {
      fileName = fileName.substring(0, fileName.length() - 5);
    }
    return baseName + " (" + fileName + ")";
  }

  private void applyLayout(IDiagramUIModel diagram, int layoutType) {
    try {
      switch (layoutType) {
        case 0:
          ApplicationManager.instance()
              .getDiagramManager()
              .layout(diagram, ApplicationManager.instance().getDiagramManager().LAYOUT_ORGANIC);
          break;
        case 1:
          tryLayoutWithReflection(diagram, "LAYOUT_HIERARCHICAL");
          break;
        case 2:
          tryLayoutWithReflection(diagram, "LAYOUT_GRID");
          break;
        case 3:
        default:
          break;
      }
    } catch (Exception e) {
      System.err.println("Error applying layout: " + e.getMessage());
    }
  }

  private void tryLayoutWithReflection(IDiagramUIModel diagram, String layoutConstantName) {
    try {
      java.lang.reflect.Field layoutField =
          ApplicationManager.instance().getDiagramManager().getClass().getField(layoutConstantName);
      Object layoutConstant = layoutField.get(ApplicationManager.instance().getDiagramManager());

      if (layoutConstant instanceof Integer) {
        ApplicationManager.instance().getDiagramManager().layout(diagram, (Integer) layoutConstant);
      } else if (layoutConstant instanceof String) {
        java.lang.reflect.Method layoutMethod =
            ApplicationManager.instance()
                .getDiagramManager()
                .getClass()
                .getMethod("layout", IDiagramUIModel.class, String.class);
        layoutMethod.invoke(
            ApplicationManager.instance().getDiagramManager(), diagram, layoutConstant);
      }
    } catch (Exception e) {
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

  private void updateImportButtonState() {
    String inputPath = filePathField.getText().trim();
    importButton.setEnabled(!inputPath.isEmpty());
  }

  private void setControlsEnabled(boolean enabled) {
    filePathField.setEnabled(enabled);
    browseButton.setEnabled(enabled);
    importButton.setEnabled(enabled && !filePathField.getText().trim().isEmpty());
    cancelButton.setEnabled(enabled);
    useCaseDiagramRadio.setEnabled(enabled);
    requirementsDiagramRadio.setEnabled(enabled);
    importUseCasesCheckBox.setEnabled(enabled);
    importActorsCheckBox.setEnabled(enabled);
    importRequirementsCheckBox.setEnabled(enabled);
    layoutComboBox.setEnabled(enabled);
  }

  private void showError(String message) {
    JOptionPane.showMessageDialog(this, message, "Import Error", JOptionPane.ERROR_MESSAGE);
  }

  private String getSavedImportSource() {
    try {
      IUserPathOptions userPathOptions = ApplicationManager.instance().getUserPathOptions();
      IUserPath[] userPaths = userPathOptions.getUserPaths();
      for (IUserPath userPath : userPaths) {
        if (USER_PATH_NAME.equals(userPath.getName())) {
          return userPath.getPath();
        }
      }
    } catch (Exception e) {
      // Ignore errors when retrieving saved path
    }
    return null;
  }

  private void saveImportSource(String filePath) {
    try {
      IUserPathOptions userPathOptions = ApplicationManager.instance().getUserPathOptions();

      IUserPath[] userPaths = userPathOptions.getUserPaths();
      for (IUserPath userPath : userPaths) {
        if (USER_PATH_NAME.equals(userPath.getName())) {
          userPathOptions.removeUserPath(userPath);
          break;
        }
      }

      userPathOptions.addUserPath(USER_PATH_NAME, filePath);
    } catch (Exception e) {
      // Ignore errors when saving path - not critical for functionality
    }
  }
}
