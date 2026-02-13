package com.xteammors.openclaw.skills;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.PositiveIntegerValidator;
import com.xteammors.openclaw.utils.ShellUtils;
import com.teammors.robot.ws.TRobotClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class CryptoPriceSkill implements AgentSkill {

    private static final String PYTHON_SCRIPT_PATH = "skills/crypto-price/scripts/get_crypto_price.py";

    @Autowired
    private ChatClient chatClient;

    @Override
    public String getName() {
        return "crypto-price";
    }

    @Override
    public String getDescription() {
        return "Retrieves current cryptocurrency prices (BTC, ETH, etc.).";
    }

    @Override
    public String execute(String input, String chatId) {
        log.info("Executing CryptoPriceSkill with input: {}", input);
        try {
            // 1. Extract symbols using LLM
            String prompt = """
                    Extract cryptocurrency symbols from the following user input.
                    Return ONLY the symbols separated by comma, e.g. "BTC,ETH,SOL".
                    If no specific symbols are mentioned, return "BTC,ETH".
                    Map common names to symbols (e.g. Bitcoin -> BTC).
                    User Input: %s
                    """.formatted(input);

            String symbols = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content().trim();
            
            // Clean up
            symbols = symbols.replace("\"", "").replace("'", "").replace(" ", "");
            
            log.info("Extracted symbols: {}", symbols);
            
            // Notify user about potential delay
            sendMessage(chatId, "Checking crypto prices for " + symbols + "... This might take a moment if dependencies are updating.");

            // 2. Execute python script
            File scriptFile = new File(PYTHON_SCRIPT_PATH);
            String scriptPath = scriptFile.getAbsolutePath();
            
            String output = ShellUtils.exec(
                ShellUtils.getPythonCommand(), 
                scriptPath,
                "--symbols", symbols
            );
            
            log.info("Python script output: {}", output);

            if (output.startsWith("Error") || output.trim().isEmpty()) {
                 return "Failed to retrieve prices. " + output;
            }

            // 3. Format output
            try {
                int jsonStart = output.indexOf("[");
                if (jsonStart == -1) {
                     if (output.contains("error")) {
                         return "Error: " + output;
                     }
                     return "No data returned.";
                }
                String jsonContent = output.substring(jsonStart);
                
                JSONArray results = JSON.parseArray(jsonContent);
                if (results == null || results.isEmpty()) {
                    return "No prices found.";
                }

                StringBuilder sb = new StringBuilder();
                int count = results.size();
                for (int i = 0; i < results.size(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    if (item.containsKey("error")) {
                         sb.append("▶Symbol:").append(item.getString("symbol")).append("\n");
                         sb.append("  Error: ").append(item.getString("error")).append("\n\n");
                         continue;
                    }
                    
                    sb.append("▶Symbol:").append(item.getString("symbol")).append("\n");
                    sb.append("  price: ").append(item.getString("price")).append("\n");
                    
                    if (item.containsKey("change_24h_percent") && item.get("change_24h_percent") != null) {
                        sb.append("  24h Change Percent: ").append(String.format("%.4f", item.getDouble("change_24h_percent"))).append("%\n");
                    } else {
                        sb.append("  24h Change Percent: N/A\n");
                    }
                    
                    sb.append("  24h High: ").append(item.containsKey("high_24h") ? item.getString("high_24h") : "N/A").append("\n");
                    sb.append("  24h Low: ").append(item.containsKey("low_24h") ? item.getString("low_24h") : "N/A").append("\n");

                    count--;
                    if(count == 0){
                        sb.append("  volume: ").append(item.containsKey("volume") ? item.getString("volume") : "N/A");
                    }else {
                        sb.append("  volume: ").append(item.containsKey("volume") ? item.getString("volume") : "N/A").append("\n");
                    }

                }
                
                return sb.toString();

            } catch (Exception e) {
                log.error("Failed to parse price JSON", e);
                return "Price Data (Raw):\n" + output;
            }

        } catch (Exception e) {
            log.error("Failed to execute CryptoPriceSkill", e);
            return "Failed to execute crypto price check: " + e.getMessage();
        }
    }
    
    private void sendMessage(String chatId, String message) {
        if (chatId == null || message == null) return;
        
        try {
            if (PositiveIntegerValidator.isPositiveInteger(chatId)) {
                String toUid = TRobotClient.instance().mId + "_" + chatId;
                TRobotClient.instance().sendSingleUserTxtMessage(message, toUid, 1);
            } else {
                TRobotClient.instance().sendToGroupTxtMessage(message, chatId, 1);
            }
        } catch (Exception e) {
            log.error("Failed to send intermediate message", e);
        }
    }
}
