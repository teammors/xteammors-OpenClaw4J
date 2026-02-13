package com.xteammors.openclaw.skills;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class SystemStatusSkill implements AgentSkill {

    private static final String PYTHON_SCRIPT_PATH = "skills/system-status/scripts/get_system_status.py";

    @Override
    public String getName() {
        return "system-status";
    }

    @Override
    public String getDescription() {
        return "Retrieves current system status: CPU, Memory, Disk usage, and active process count.";
    }

    @Override
    public String execute(String input, String chatId) {
        log.info("Executing SystemStatusSkill with input: {}", input);
        try {
            // Execute python script
            File scriptFile = new File(PYTHON_SCRIPT_PATH);
            String scriptPath = scriptFile.getAbsolutePath();
            
            log.info("Executing python script: {}", scriptPath);

            String output = ShellUtils.exec(ShellUtils.getPythonCommand(), scriptPath);
            
            log.info("Python script output: {}", output);

            if (output.startsWith("Error") || output.trim().isEmpty()) {
                 return "Failed to retrieve system status. " + output;
            }

            // Format output
            try {
                // Find start of JSON
                int jsonStart = output.indexOf("{");
                if (jsonStart == -1) {
                     return "Failed to parse system status: No JSON found.";
                }
                String jsonContent = output.substring(jsonStart);
                
                JSONObject status = JSON.parseObject(jsonContent);
                
                if (status.containsKey("error")) {
                    return "Error retrieving status: " + status.getString("error");
                }

                StringBuilder sb = new StringBuilder();
                sb.append("System Status Report:\n");
                
                // CPU
                sb.append("--------------------------------\n");
                sb.append("CPU Usage: ").append(status.getString("cpu_usage")).append("\n");
                
                // Memory
                JSONObject mem = status.getJSONObject("memory");
                sb.append("Memory: \n");
                sb.append("▶Total: ").append(mem.getString("total"))
                  .append("  Used: ").append(mem.getString("used"))
                  .append("  Free: ").append(mem.getString("available")) // Available is more useful than free usually
                  .append("  (").append(mem.getString("percent")).append(")\n");
                  
                // Disk
                JSONObject disk = status.getJSONObject("disk");
                sb.append("Disk Usage: \n");
                sb.append("▶Total: ").append(disk.getString("total"))
                  .append("  Used: ").append(disk.getString("used"))
                  .append("  Free: ").append(disk.getString("free"))
                  .append("  (").append(disk.getString("percent")).append(")\n");
                  
                // Processes
                sb.append("Processes: \n");
                sb.append("▶Total Running: ").append(status.getString("total_processes")).append("\n");
                sb.append("▶User Processes: ").append(status.getString("user_processes")).append("\n");
                sb.append("--------------------------------");

                return sb.toString();

            } catch (Exception e) {
                log.error("Failed to parse status JSON", e);
                return "System Status (Raw):\n" + output;
            }

        } catch (Exception e) {
            log.error("Failed to execute SystemStatusSkill", e);
            return "Failed to retrieve system status: " + e.getMessage();
        }
    }
}
