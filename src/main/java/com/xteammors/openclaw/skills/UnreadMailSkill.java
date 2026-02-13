package com.xteammors.openclaw.skills;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UnreadMailSkill implements AgentSkill {

    private static final String SKILL_CONFIG_PATH = "skills/unread-mail/SKILL.md";
    private static final String PYTHON_SCRIPT_PATH = "skills/unread-mail/scripts/get_unread_emails.py";

    @Override
    public String getName() {
        return "unread-mail";
    }

    @Override
    public String getDescription() {
        return "Retrieves unread emails from the mailbox. Returns the subject and sender of recent unread messages.";
    }

    @Override
    public String execute(String input, String chatId) {
        log.info("Executing UnreadMailSkill with input: {}", input);
        try {
            // 1. Load configuration from SKILL.md
            Map<String, Object> config = loadConfig();
            if (config == null) {
                return "Error: Email configuration not found in " + SKILL_CONFIG_PATH;
            }

            String host = (String) config.get("imap_host");
            Integer port = (Integer) config.get("imap_port");
            String username = (String) config.get("username");
            String password = (String) config.get("password");

            // 2. Execute python script to get unread emails
            File scriptFile = new File(PYTHON_SCRIPT_PATH);
            String scriptPath = scriptFile.getAbsolutePath();
            
            log.info("Executing python script: {}", scriptPath);

            // Default limit 5, could be extracted from input if needed
            String limit = "5"; 

            String output = ShellUtils.exec(
                ShellUtils.getPythonCommand(), 
                scriptPath,
                "--host", host,
                "--port", String.valueOf(port),
                "--username", username,
                "--password", password,
                "--limit", limit
            );
            
            log.info("Python script output: {}", output);

            if (output.startsWith("Error") || output.trim().isEmpty()) {
                 return "Failed to retrieve emails. " + output;
            }

            // 3. Format output
            try {
                // Find the start of the JSON array
                int jsonStartIndex = output.indexOf("[");
                if (jsonStartIndex == -1) {
                    throw new RuntimeException("No JSON array found in output");
                }
                
                String jsonContent = output.substring(jsonStartIndex);
                
                // Parse the JSON output from python script
                JSONArray emails = JSON.parseArray(jsonContent);
                if (emails == null || emails.isEmpty()) {
                    return "Here are your unread emails:\nUnRead Number: 0\nMail List: (Empty)";
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Here are your unread emails:\n");
                sb.append("UnRead Number: ").append(emails.size()).append("\n");
                sb.append("Mail List:\n");

                int count = emails.size();
                for (int i = 0; i < emails.size(); i++) {
                    JSONObject email = emails.getJSONObject(i);
                    String from = email.getString("from");
                    String subject = email.getString("subject");
                    count--;
                    if(count == 0){
                        sb.append(i + 1).append("、From：").append(from).append("  Subject：").append(subject);
                    }else {
                        sb.append(i + 1).append("、From：").append(from).append("  Subject：").append(subject).append("\n");
                    }

                }
                
                return sb.toString();
            } catch (Exception e) {
                log.error("Failed to parse email JSON", e);
                // Fallback to raw output if parsing fails
                return "Here are your unread emails (Raw output):\n" + output;
            }

        } catch (Exception e) {
            log.error("Failed to execute UnreadMailSkill", e);
            return "Failed to retrieve unread emails: " + e.getMessage();
        }
    }

    private Map<String, Object> loadConfig() {
        try {
            File file = new File(SKILL_CONFIG_PATH);
            if (!file.exists()) {
                log.error("Skill config file not found: {}", SKILL_CONFIG_PATH);
                return null;
            }

            String content = Files.readString(file.toPath());
            
            // Extract YAML block using regex
            Pattern pattern = Pattern.compile("```yaml\\s*(.*?)\\s*```", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                String yamlContent = matcher.group(1);
                Yaml yaml = new Yaml();
                Map<String, Object> obj = yaml.load(yamlContent);
                
                if (obj.containsKey("mail")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mailConfig = (Map<String, Object>) obj.get("mail");
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("imap_host", mailConfig.get("imap_host"));
                    result.put("imap_port", mailConfig.get("imap_port"));
                    result.put("username", mailConfig.get("username"));
                    result.put("password", mailConfig.get("password"));
                    
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Failed to load mail config from SKILL.md", e);
        }
        return null;
    }
}
