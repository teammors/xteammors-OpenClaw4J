package com.xteammors.openclaw.skills;

import com.alibaba.fastjson.JSONObject;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SendEmailSkill implements AgentSkill {

    private static final String SKILL_CONFIG_PATH = "skills/send-email/SKILL.md";
    private static final String PYTHON_SCRIPT_PATH = "skills/send-email/scripts/send_email.py";

    @Autowired
    private ChatClient chatClient;

    @Override
    public String getName() {
        return "send-email";
    }

    @Override
    public String getDescription() {
        return "Sends emails to specified recipients.";
    }

    @Override
    public String execute(String input, String chatId) {
        log.info("Executing SendEmailSkill with input: {}", input);
        try {
            // 1. Load configuration from SKILL.md
            Map<String, Object> config = loadConfig();
            if (config == null) {
                return "Error: Email configuration not found in " + SKILL_CONFIG_PATH;
            }

            String host = (String) config.get("host");
            Integer port = (Integer) config.get("port");
            String username = (String) config.get("username");
            String password = (String) config.get("password");

            // 2. Extract email details using LLM
            String prompt = """
                    You are a helper that extracts email details from natural language text.
                    Extract the following fields from the user input:
                    - to: The recipient's email address
                    - subject: The email subject
                    - body: The email body content

                    Return the result as a strictly valid JSON object. Do not include any markdown formatting or code blocks.
                    Example: {"to": "user@example.com", "subject": "Hello", "body": "This is a test."}

                    User Input: %s
                    """.formatted(input);

            String jsonResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // Clean up potential markdown code blocks
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();

            JSONObject emailData = JSONObject.parseObject(jsonResponse);
            String to = emailData.getString("to");
            String subject = emailData.getString("subject");
            String body = emailData.getString("body");

            if (to == null || to.isEmpty()) {
                return "Error: Could not determine the recipient email address.";
            }

            // 3. Send the email using python script
            File scriptFile = new File(PYTHON_SCRIPT_PATH);
            String scriptPath = scriptFile.getAbsolutePath();
            
            log.info("Executing python script: {}", scriptPath);

            String output = ShellUtils.exec(
                ShellUtils.getPythonCommand(), 
                scriptPath,
                "--host", host,
                "--port", String.valueOf(port),
                "--username", username,
                "--password", password,
                "--to", to,
                "--subject", subject != null ? subject : "No Subject",
                "--body", body != null ? body : ""
            );
            
            log.info("Python script output: {}", output);

            if (output.contains("Error")) {
                 return "Failed to send email: " + output;
            }
            
            return "Email sent successfully to " + to;

        } catch (Exception e) {
            log.error("Failed to send email", e);
            return "Failed to send email: " + e.getMessage();
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
                    result.put("host", mailConfig.get("host"));
                    result.put("port", mailConfig.get("port"));
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
