package com.orgatex.vp.sphinx.dialog;

import com.orgatex.vp.sphinx.extractor.UseCaseDiagramExtractor;
import com.orgatex.vp.sphinx.generator.JsonExporter;
import com.orgatex.vp.sphinx.model.NeedsFile;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.project.IUserPath;
import com.vp.plugin.project.IUserPathOptions;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Simple dialog for exporting use case diagrams to sphinx-needs JSON format.
 */
public class ExportDiagramToSphinxDialog extends JDialog {

    private static final String USER_PATH_NAME = "sphinx_needs_export_destination";

    private final IDiagramUIModel diagram;

    private JTextField outputFileField;
    private JButton browseButton;
    private JButton exportButton;
    private JButton cancelButton;

    public ExportDiagramToSphinxDialog(IDiagramUIModel diagram) {
        super((Frame) null, "Export to Sphinx-Needs", true);
        this.diagram = diagram;
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        outputFileField = new JTextField(40);
        browseButton = new JButton("Browse...");
        exportButton = new JButton("Export");
        cancelButton = new JButton("Cancel");

        // Set output file path from saved preference or default
        String savedPath = getSavedExportDestination();
        String defaultFileName = sanitizeFileName(diagram.getName()) + "_needs.json";

        if (savedPath != null && !savedPath.isEmpty()) {
            // Use saved directory with current diagram name
            File savedDir = new File(savedPath);
            if (savedDir.isDirectory()) {
                outputFileField.setText(new File(savedDir, defaultFileName).getAbsolutePath());
            } else {
                // Saved path is a file, use its directory
                outputFileField.setText(new File(savedDir.getParent(), defaultFileName).getAbsolutePath());
            }
        } else {
            // Use default location
            outputFileField.setText(System.getProperty("user.home") + File.separator + defaultFileName);
        }
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        // Header
        JLabel headerLabel = new JLabel("Export '" + diagram.getName() + "' to Sphinx-Needs JSON format");
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        add(headerLabel, BorderLayout.NORTH);

        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 5);
        mainPanel.add(new JLabel("Output file:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(outputFileField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 5, 5, 10);
        mainPanel.add(browseButton, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(exportButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        browseButton.addActionListener(this::browseFile);
        exportButton.addActionListener(this::export);
        cancelButton.addActionListener(e -> dispose());
    }

    private void browseFile(ActionEvent e) {
        JFileChooser fileChooser = ApplicationManager.instance().getViewManager().createJFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Select output file");
        fileChooser.setSelectedFile(new File(outputFileField.getText()));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputFileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void export(ActionEvent e) {
        String outputPath = outputFileField.getText().trim();
        if (outputPath.isEmpty()) {
            showError("Please select an output file.");
            return;
        }

        File outputFile = new File(outputPath);

        try {
            // Extract diagram data
            NeedsFile needsFile = UseCaseDiagramExtractor.extractDiagram(diagram);

            // Export to JSON
            JsonExporter.exportToFile(needsFile, outputFile);

            // Save the export destination for future use
            saveExportDestination(outputFile.getAbsolutePath());

            // Show success message
            showSuccess("Successfully exported to: " + outputFile.getAbsolutePath());
            dispose();

        } catch (Exception ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        ApplicationManager.instance().getViewManager().showMessageDialog(this, message, "Export Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        ApplicationManager.instance().getViewManager().showMessageDialog(this, message, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
    }

    private String sanitizeFileName(String input) {
        if (input == null) return "diagram";
        return input.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }

    private String getSavedExportDestination() {
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

    private void saveExportDestination(String filePath) {
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
