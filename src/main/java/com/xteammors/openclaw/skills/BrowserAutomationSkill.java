package com.xteammors.openclaw.skills;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class BrowserAutomationSkill implements AgentSkill {

    private static final String PYTHON_SCRIPT_PATH = "skills/browser-automation/scripts/search_crawl.py";

    @Autowired
    private ChatClient chatClient;

    @Override
    public String getName() {
        return "browser-automation";
    }

    @Override
    public String getDescription() {
        return "Crawls and collects data for user-specified keywords using browser automation.";
    }

    @Override
    public String execute(String input, String chatId) {
        log.info("Executing BrowserAutomationSkill with input: {}", input);
        try {
            // 1. Extract keyword using LLM
            String prompt = """
                    Extract the search keyword from the following user input.
                    Return ONLY the keyword, no other text.
                    User Input: %s
                    """.formatted(input);

            String keyword = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content().trim();
            
            // Clean up keyword if it contains quotes
            keyword = keyword.replace("\"", "").replace("'", "");
            
            log.info("Extracted keyword: {}", keyword);

            // 2. Execute python script
            File scriptFile = new File(PYTHON_SCRIPT_PATH);
            String scriptPath = scriptFile.getAbsolutePath();
            
            // Default limit
            String limit = "5";

            String output = ShellUtils.exec(
                ShellUtils.getPythonCommand(), 
                scriptPath,
                "--keyword", keyword,
                "--limit", limit
            );
            
            log.info("Python script output: {}", output);

            if (output.startsWith("Error") || output.trim().isEmpty()) {
                 return "Failed to crawl data. " + output;
            }

            // 3. Format output
            try {
                // Find start of JSON array
                int jsonStart = output.indexOf("[");
                 if (jsonStart == -1) {
                     // Check if it is an error object
                     if (output.contains("error")) {
                         return "Error during crawl: " + output;
                     }
                     return "No data found or invalid format.";
                }
                String jsonContent = output.substring(jsonStart);
                
                JSONArray results = JSON.parseArray(jsonContent);
                if (results == null || results.isEmpty()) {
                    return "No results found for keyword: " + keyword;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Search Results for '").append(keyword).append("':\n\n");
                
                for (int i = 0; i < results.size(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    sb.append(i + 1).append(". ").append(item.getString("title")).append("\n");
                    sb.append("   Link: ").append(item.getString("link")).append("\n");
                    sb.append("   Summary: ").append(item.getString("snippet")).append("\n\n");
                }
                
                return sb.toString();

            } catch (Exception e) {
                log.error("Failed to parse crawl result JSON", e);
                return "Crawl Results (Raw):\n" + output;
            }

        } catch (Exception e) {
            log.error("Failed to execute BrowserAutomationSkill", e);
            return "Failed to execute browser automation: " + e.getMessage();
        }
    }
}
