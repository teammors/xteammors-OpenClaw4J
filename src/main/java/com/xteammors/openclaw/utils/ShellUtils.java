package com.xteammors.openclaw.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellUtils {
    private static final Logger log = LoggerFactory.getLogger(ShellUtils.class);

    private ShellUtils() {
    }

    // 宿主机 SSH 配置
    private static final String HOST_MACHINE = "localhost";
    private static final String SSH_USER = "root";
    // 可以通过环境变量或配置文件获取
    private static final String SSH_KEY_PATH = "/root/.ssh/id_rsa";

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
            // 不再合并标准错误流，只读取标准输出

            Process process = processBuilder.start();
            
            // 读取标准输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 读取标准错误并记录到日志
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    log.info("Command error output: {}", line);
                }
            }

            boolean exitCode = process.waitFor(30, TimeUnit.SECONDS);
            if (!exitCode) {
                log.warn("Command timed out");
                process.destroy();
                return "Error: Command timed out";
            }
            
            if (process.exitValue() != 0) {
                return "Error (Exit code " + process.exitValue() + ")";
            }

        } catch (Exception e) {
            log.error("Error executing command", e);
            return "Error: " + e.getMessage();
        }
        return output.toString().trim();
    }

    /**
     * Executes a command with arguments and extra environment variables.
     * Merges stderr into stdout for full output capture.
     * @param env Extra environment variables to set
     * @param command The command and its arguments
     * @return The combined output (stdout + stderr)
     */
    public static String execWithEnv(Map<String, String> env, String... command) {
        StringBuilder output = new StringBuilder();
        try {
            log.info("Executing command with env: {}", String.join(" ", command));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // merge stderr into stdout
            if (env != null) {
                pb.environment().putAll(env);
            }

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("Command timed out");
                process.destroyForcibly();
                return "Error: Command timed out after 120 seconds";
            }

            if (process.exitValue() != 0) {
                String result = output.toString().trim();
                if (result.isEmpty()) result = "Exit code " + process.exitValue();
                return "Error: " + result;
            }

        } catch (Exception e) {
            log.error("Error executing command with env", e);
            return "Error: " + e.getMessage();
        }
        return output.toString().trim();
    }

    /**
     * Executes a command on the host machine via SSH.
     * @param command The command to execute on the host
     * @return The standard output of the command
     */
    public static String execOnHost(String command) {
        StringBuilder output = new StringBuilder();
        try {
            log.info("Executing command on host: {}", command);
            
            // 构建 SSH 命令
            String[] sshCommand = {
                "ssh",
                "-i", SSH_KEY_PATH,
                "-o", "StrictHostKeyChecking=no",
                "-o", "UserKnownHostsFile=/dev/null",
                SSH_USER + "@" + HOST_MACHINE,
                command
            };
            
            ProcessBuilder processBuilder = new ProcessBuilder(sshCommand);
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
                log.warn("SSH command timed out: {}", command);
                process.destroy();
            }
            
            if (process.exitValue() != 0) {
                 return "Error (Exit code " + process.exitValue() + "): " + output.toString().trim();
            }

        } catch (Exception e) {
            log.error("Error executing command on host: " + command, e);
            return "Error: " + e.getMessage();
        }
        return output.toString().trim();
    }

    /**
     * Executes a command with arguments on the host machine via SSH.
     * @param command The command and its arguments
     * @return The standard output of the command
     */
    public static String execOnHost(String... command) {
        // 构建完整命令字符串
        String fullCommand = String.join(" ", command);
        return execOnHost(fullCommand);
    }
}
