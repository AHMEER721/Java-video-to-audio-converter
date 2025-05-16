package com.converter.controller;

import com.converter.gui.MainFrame;
import com.converter.service.FFmpegService;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;

public class ConversionController {

    private MainFrame view;
    private FFmpegService ffmpegService;
    private File selectedInputFile = null;
    private File tempOutputFile = null; // Store temporary output file path

    public ConversionController(MainFrame view) {
        this.view = view;
        this.ffmpegService = new FFmpegService();
        // Check if FFmpeg exists on startup
        if (!ffmpegService.checkFFmpegExists()) {
            view.updateStatus("Error: FFmpeg not found in system PATH. Please install FFmpeg.", true);
            view.disableConversionFeatures(); // Disable buttons if FFmpeg is missing
        }
    }

    public void setSelectedInputFile(File file) {
        this.selectedInputFile = file;
        this.tempOutputFile = null; // Reset temp output when input changes
        view.updateStatus("File selected: " + file.getName() + ". Ready to convert.", false);
        view.enableConvertButton(true);
        view.enableSaveButton(false);
    }

    public void startConversion(String startTime, String endTime) {
        if (selectedInputFile == null) {
            view.updateStatus("Error: No input file selected.", true);
            return;
        }

        // Basic validation for time format (can be improved)
        if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
            view.updateStatus("Error: Invalid time format. Use HH:MM:SS or seconds.", true);
            return;
        }

        view.updateStatus("Conversion started... Please wait.", false);
        view.enableConvertButton(false);
        view.enableSaveButton(false);

        // Define temporary output file path
        try {
            File outputDir = new File("/home/ubuntu/video_audio_converter_app/output");
            if (!outputDir.exists()) outputDir.mkdirs();
            // Create a unique temp file name
            tempOutputFile = File.createTempFile("audio_conversion_", ".mp3", outputDir);
        } catch (IOException e) {
            view.updateStatus("Error: Could not create temporary output file. " + e.getMessage(), true);
            view.enableConvertButton(true);
            return;
        }

        final String finalStartTime = startTime.trim().isEmpty() ? null : startTime.trim();
        final String finalEndTime = endTime.trim().isEmpty() ? null : endTime.trim();
        final String inputPath = selectedInputFile.getAbsolutePath();
        final String outputPath = tempOutputFile.getAbsolutePath();

        // Run FFmpeg conversion in a background thread to avoid freezing the GUI
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return ffmpegService.convertVideoToAudio(inputPath, outputPath, finalStartTime, finalEndTime);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get(); // Get the result from doInBackground
                    if (success) {
                        view.updateStatus("Conversion successful! Ready to save.", false);
                        view.enableSaveButton(true);
                    } else {
                        view.updateStatus("Error: Conversion failed. Check console log for details.", true);
                        if (tempOutputFile != null) tempOutputFile.delete(); // Clean up failed temp file
                        tempOutputFile = null;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    view.updateStatus("Error: Conversion process failed. " + e.getMessage(), true);
                     if (tempOutputFile != null) tempOutputFile.delete();
                     tempOutputFile = null;
                    e.printStackTrace(); // Log the full error
                } finally {
                    view.enableConvertButton(true); // Re-enable convert button regardless of outcome
                }
            }
        };

        worker.execute();
    }

    public void saveOutputFile(File destinationFile) {
        if (tempOutputFile == null || !tempOutputFile.exists()) {
            view.updateStatus("Error: No converted file available to save or temp file missing.", true);
            return;
        }

        // Ensure .mp3 extension
        if (!destinationFile.getName().toLowerCase().endsWith(".mp3")) {
            destinationFile = new File(destinationFile.getParentFile(), destinationFile.getName() + ".mp3");
        }

        try {
            Files.move(tempOutputFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            view.updateStatus("File successfully saved to: " + destinationFile.getAbsolutePath(), false);
            tempOutputFile = null; // Reset temp file path after successful move
            view.enableSaveButton(false); // Disable save button after successful save
        } catch (IOException e) {
            view.updateStatus("Error: Failed to save file. " + e.getMessage(), true);
            e.printStackTrace(); // Log the error
        }
    }

    // Simple time format validation (accepts empty, seconds, or HH:MM:SS)
    private boolean isValidTimeFormat(String time) {
        if (time == null || time.trim().isEmpty()) {
            return true; // Empty is valid (no trimming)
        }
        // Regex for HH:MM:SS or just seconds (integer or float)
        return time.trim().matches("^(\\d{1,2}:){2}\\d{1,2}(\\.\\d+)?$|^\\d+(\\.\\d+)?$");
    }

    // Getter for the selected input file (needed by MainFrame)
    public File getSelectedInputFile() {
        return selectedInputFile;
    }

}

