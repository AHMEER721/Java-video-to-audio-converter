package com.converter.gui;

import com.converter.controller.ConversionController;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainFrame extends JFrame {

    private JTextField filePathField;
    private JTextField startTimeField;
    private JTextField endTimeField;
    private JButton browseButton;
    private JButton convertButton;
    private JButton saveButton;
    private JLabel statusLabel;
    private JFileChooser fileChooser;

    private ConversionController controller;

    public MainFrame() {
        setTitle("Video to Audio Converter");
        setSize(600, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize components
        fileChooser = new JFileChooser();
        filePathField = new JTextField("No file selected", 30);
        filePathField.setEditable(false);
        startTimeField = new JTextField(8);
        endTimeField = new JTextField(8);
        browseButton = new JButton("Browse...");
        convertButton = new JButton("Convert to MP3");
        saveButton = new JButton("Save Audio...");
        statusLabel = new JLabel("Initializing...");

        // Disable buttons initially
        convertButton.setEnabled(false);
        saveButton.setEnabled(false);

        // Layout components
        setupLayout();

        // Initialize Controller (after components are created)
        controller = new ConversionController(this);

        // Add Action Listeners
        browseButton.addActionListener(e -> browseForFile());
        convertButton.addActionListener(e -> startConversion());
        saveButton.addActionListener(e -> saveConvertedFile());

        // Set initial status after controller initialization
        // Controller constructor might update status if FFmpeg check fails
        if (statusLabel.getText().equals("Initializing...")) { // Only update if controller didn't set an error
             statusLabel.setText("Ready. Select a video file to start.");
        }
    }

    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: File Selection Label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Select Video File:"), gbc);

        // Row 2: File Path Field and Browse Button
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(filePathField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        add(browseButton, gbc);

        // Row 3: Trimming Label
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Trim Audio (Optional, Format: HH:MM:SS or seconds):"), gbc);

        // Row 4: Start Time
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        add(new JLabel("Start Time:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(startTimeField, gbc);

        // Row 5: End Time
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        add(new JLabel("End Time:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        add(endTimeField, gbc);

        // Row 6: Convert Button
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        add(convertButton, gbc);

        // Row 7: Status Label
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, gbc);

        // Row 8: Save Button
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        add(saveButton, gbc);
    }

    private void browseForFile() {
        updateStatus("Selecting file...", false);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
            controller.setSelectedInputFile(selectedFile); // Notify controller
        } else {
            updateStatus("File selection cancelled.", false);
        }
    }

    private void startConversion() {
        String startTime = startTimeField.getText();
        String endTime = endTimeField.getText();
        controller.startConversion(startTime, endTime); // Delegate to controller
    }

    private void saveConvertedFile() {
        // Suggest a filename based on the input file
        File currentInput = controller.getSelectedInputFile(); // Need getter in controller or store locally
        if (currentInput != null) {
             fileChooser.setSelectedFile(new File(currentInput.getName().replaceFirst("[.][^.]+$", "") + ".mp3"));
        }

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File destinationFile = fileChooser.getSelectedFile();
            controller.saveOutputFile(destinationFile); // Delegate to controller
        } else {
            updateStatus("Save operation cancelled.", false);
        }
    }

    // --- Methods for Controller to Update GUI ---

    public void updateStatus(String message, boolean isError) {
        // Ensure GUI updates happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(isError ? Color.RED : Color.BLACK);
        });
    }

    public void enableConvertButton(boolean enabled) {
        SwingUtilities.invokeLater(() -> convertButton.setEnabled(enabled));
    }

    public void enableSaveButton(boolean enabled) {
        SwingUtilities.invokeLater(() -> saveButton.setEnabled(enabled));
    }

    public void disableConversionFeatures() {
         SwingUtilities.invokeLater(() -> {
            browseButton.setEnabled(false);
            convertButton.setEnabled(false);
            saveButton.setEnabled(false);
            startTimeField.setEnabled(false);
            endTimeField.setEnabled(false);
        });
    }
}

