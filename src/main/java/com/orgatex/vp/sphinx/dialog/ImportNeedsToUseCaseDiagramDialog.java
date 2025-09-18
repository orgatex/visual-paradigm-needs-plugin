package com.orgatex.vp.sphinx.dialog;

import com.orgatex.vp.sphinx.importer.NeedsFileImporter;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.project.IUserPath;
import com.vp.plugin.project.IUserPathOptions;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Dialog for importing sphinx-needs JSON files into Visual Paradigm use case diagrams. */
public class ImportNeedsToUseCaseDiagramDialog extends JDialog {

  private static final String USER_PATH_NAME = "sphinx_needs_import_source";

  private JTextField inputFileField;
  private JButton browseButton;
  private JButton importButton;
  private JButton cancelButton;

  public ImportNeedsToUseCaseDiagramDialog() {
    super((Frame) null, "Import from Sphinx-Needs", true);
    initializeComponents();
    layoutComponents();
    setupEventHandlers();
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    pack();
    setLocationRelativeTo(null);
  }

  private void initializeComponents() {
    inputFileField = new JTextField(40);
    browseButton = new JButton("Browse...");
    importButton = new JButton("Import");
    cancelButton = new JButton("Cancel");

    // Set input file path from saved preference or default
    String savedPath = getSavedImportSource();
    if (savedPath != null && !savedPath.isEmpty()) {
      inputFileField.setText(savedPath);
    } else {
      // Use default location
      inputFileField.setText(System.getProperty("user.home"));
    }
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());

    // Header
    JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    headerPanel.add(new JLabel("Import sphinx-needs JSON file to create a use case diagram"));
    add(headerPanel, BorderLayout.NORTH);

    // Main content
    JPanel contentPanel = new JPanel(new GridBagLayout());
    contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    GridBagConstraints gbc = new GridBagConstraints();

    // Input file label
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0, 0, 10, 0);
    contentPanel.add(new JLabel("Input JSON file:"), gbc);

    // Input file field and browse button
    JPanel filePanel = new JPanel(new BorderLayout(5, 0));
    filePanel.add(inputFileField, BorderLayout.CENTER);
    filePanel.add(browseButton, BorderLayout.EAST);

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(0, 0, 20, 0);
    contentPanel.add(filePanel, gbc);

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

    // Enable/disable import button based on file selection
    inputFileField
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
  }

  private void onBrowseClicked(ActionEvent e) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Select Sphinx-Needs JSON file");
    fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));

    // Set initial directory from current field value
    String currentPath = inputFileField.getText().trim();
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
      inputFileField.setText(selectedFile.getAbsolutePath());
    }
  }

  private void onImportClicked(ActionEvent e) {
    String inputPath = inputFileField.getText().trim();
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

    // Save the input path for next time
    saveImportSource(inputPath);

    // Perform the import
    performImport(inputFile);
  }

  private void onCancelClicked(ActionEvent e) {
    dispose();
  }

  private void performImport(File inputFile) {
    try {
      // Disable UI during import
      setControlsEnabled(false);
      importButton.setText("Importing...");

      // Create importer and perform import
      NeedsFileImporter importer = new NeedsFileImporter();
      IDiagramUIModel diagram = importer.importFromFile(inputFile);

      // Show success message
      showSuccess(
          "Successfully imported "
              + inputFile.getName()
              + "\nCreated diagram: "
              + diagram.getName());

      // Close dialog
      dispose();

    } catch (NeedsFileImporter.ImportException ex) {
      showError("Import failed: " + ex.getMessage());
    } catch (Exception ex) {
      showError("Unexpected error during import: " + ex.getMessage());
      ex.printStackTrace();
    } finally {
      // Re-enable UI
      setControlsEnabled(true);
      importButton.setText("Import");
    }
  }

  private void updateImportButtonState() {
    String inputPath = inputFileField.getText().trim();
    importButton.setEnabled(!inputPath.isEmpty());
  }

  private void setControlsEnabled(boolean enabled) {
    inputFileField.setEnabled(enabled);
    browseButton.setEnabled(enabled);
    importButton.setEnabled(enabled && !inputFileField.getText().trim().isEmpty());
    cancelButton.setEnabled(enabled);
  }

  private void showError(String message) {
    JOptionPane.showMessageDialog(this, message, "Import Error", JOptionPane.ERROR_MESSAGE);
  }

  private void showSuccess(String message) {
    JOptionPane.showMessageDialog(
        this, message, "Import Successful", JOptionPane.INFORMATION_MESSAGE);
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

      // Remove existing path with same name if it exists
      IUserPath[] userPaths = userPathOptions.getUserPaths();
      for (IUserPath userPath : userPaths) {
        if (USER_PATH_NAME.equals(userPath.getName())) {
          userPathOptions.removeUserPath(userPath);
          break;
        }
      }

      // Add new path
      userPathOptions.addUserPath(USER_PATH_NAME, filePath);
    } catch (Exception e) {
      // Ignore errors when saving path - not critical for functionality
    }
  }
}
