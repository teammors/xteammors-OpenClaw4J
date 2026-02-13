package com.xteammors.openclaw.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShellUtils {

    private ShellUtils() {
    }

    public static String getPythonCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "python";
        }
        return "python3";
    }

    /**
     * Executes a shell command and returns the output as a string.
     * @param command The command to execute (e.g., "ls -la")
     * @return The standard output of the command
     */
    public static String exec(String command) {
        StringBuilder output = new StringBuilder();
        try {
            log.info("Executing command: {}", command);
            // Use array to handle arguments with spaces properly if needed, 
            // but for simple cases, string is fine. For sh -c it's better.
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            processBuilder.redirectErrorStream(true); // Merge stderr into stdout

            Process process = processBuilder.start();
            
            // Read output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean exitCode = process.waitFor(10, TimeUnit.SECONDS);
            if (!exitCode) {
                log.warn("Command timed out: {}", command);
                process.destroy();
            }

        } catch (Exception e) {
            log.error("Error executing command: " + command, e);
            return "Error: " + e.getMessage();
        }
        return output.toString().trim();
    }

    /**
     * Executes a command with arguments and returns the output as a string.
     * Safer for arguments containing spaces or special characters.
     * @param command The command and its arguments
     * @return The standard output of the command
     */
    public static String exec(String... command) {
        StringBuilder output = new StringBuilder();
        try {
            log.info("Executing command: {}", String.join(" ", command));
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean exitCode = process.waitFor(30, TimeUnit.SECONDS);
            if (!exitCode) {
                log.warn("Command timed out");
                process.destroy();
            }
            
            if (process.exitValue() != 0) {
                 return "Error (Exit code " + process.exitValue() + "): " + output.toString().trim();
            }

        } catch (Exception e) {
            log.error("Error executing command", e);
            return "Error: " + e.getMessage();
        }
        return output.toString().trim();
    }
}
