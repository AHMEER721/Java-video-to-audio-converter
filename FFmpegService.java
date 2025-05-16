package com.converter.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FFmpegService {

    private String ffmpegPath = "ffmpeg"; // Assumes ffmpeg is in the system PATH

    // Basic conversion without trimming
    public boolean convertVideoToAudio(String inputFile, String outputFile) throws IOException, InterruptedException {
        return convertVideoToAudio(inputFile, outputFile, null, null);
    }

    // Conversion with optional trimming
    public boolean convertVideoToAudio(String inputFile, String outputFile, String startTime, String endTime) throws IOException, InterruptedException {
        File inFile = new File(inputFile);
        if (!inFile.exists()) {
            System.err.println("Input file does not exist: " + inputFile);
            return false;
        }

        File outFile = new File(outputFile);
        // Ensure output directory exists
        File outputDir = outFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                System.err.println("Could not create output directory: " + outputDir.getAbsolutePath());
                return false;
            }
        }

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(inputFile);

        // Add trimming options if provided and valid
        if (startTime != null && !startTime.trim().isEmpty()) {
            command.add("-ss");
            command.add(startTime.trim());
        }
        if (endTime != null && !endTime.trim().isEmpty()) {
            // If using -to, it should come after -ss but before -i for faster seeking if possible,
            // but placing it after -i is safer for accuracy. Let's place after -i.
            // Alternatively, use -t (duration) if endTime is provided and startTime is not, or calculate duration.
            // Using -to is generally more intuitive if both start and end are given.
            // However, calculating duration (-t) from start and end might be more robust across ffmpeg versions.
            // Let's stick to -to for now, assuming HH:MM:SS format or seconds.
            command.add("-to");
            command.add(endTime.trim());
        }

        command.add("-vn"); // Disable video recording
        command.add("-acodec");
        command.add("libmp3lame"); // Specify MP3 codec
        command.add("-ab");
        command.add("192k"); // Set audio bitrate (optional)
        command.add("-y"); // Overwrite output file if it exists
        command.add(outputFile);

        System.out.println("Executing FFmpeg command: " + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge error and output streams

        Process process = processBuilder.start();

        // Read output/error stream
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FFmpeg output: " + line); // Log output for debugging
            }
        }

        // Wait for the process to complete
        boolean finished = process.waitFor(10, TimeUnit.MINUTES); // Add a timeout
        if (!finished) {
            process.destroy();
            System.err.println("FFmpeg process timed out.");
            return false;
        }

        int exitCode = process.exitValue();
        System.out.println("FFmpeg process finished with exit code: " + exitCode);

        return exitCode == 0;
    }

    // Optional: Add a method to check if FFmpeg exists
    public boolean checkFFmpegExists() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("Error checking FFmpeg: " + e.getMessage());
            return false;
        }
    }
}

