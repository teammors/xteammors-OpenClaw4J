package com.xteammors.openclaw.skills;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xteammors.openclaw.property.SkillsDirProperty;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class KenoWinningNumbersSkill implements AgentSkill {

    private static final String PYTHON_SCRIPT_PATH = "/keno-winning-numbers/scripts/get_keno_results.py";

    @Autowired
    SkillsDirProperty skillsDirProperty;

    @Override
    public String getName() {
        return "keno-winning-numbers";
    }

    @Override
    public String getDescription() {
        return "Retrieves the latest 10 Keno winning numbers from PlayNow.";
    }

    @Override
    public String execute(String input, String chatId) {
        log.info("Executing KenoWinningNumbersSkill");
        try {

            String scPath = skillsDirProperty.getDir()+PYTHON_SCRIPT_PATH;
            File scriptFile = new File(scPath);

            if (!scriptFile.exists()) {
                return "Error: Script file not found at " + scPath;
            }
            
            String output = ShellUtils.exec(
                ShellUtils.getPythonCommand(), 
                scriptFile.getAbsolutePath()
            );
            
            log.info("Python script output: {}", output);

            if (output.trim().startsWith("{") && output.contains("\"error\"")) {
                 JSONObject err = JSON.parseObject(output);
                 if (err.containsKey("error")) {
                     return "Failed to retrieve Keno numbers: " + err.getString("error");
                 }
            }

            try {
                // Find JSON start
                int jsonStart = output.indexOf("[");
                if (jsonStart == -1) {
                     jsonStart = output.indexOf("{");
                }
                
                if (jsonStart == -1) {
                    return "Error parsing output: No JSON found.\nRaw output: " + output;
                }
                
                String jsonContent = output.substring(jsonStart);
                Object parsed = JSON.parse(jsonContent);
                
                if (parsed instanceof JSONArray) {
                    JSONArray draws = (JSONArray) parsed;
                    if (draws.isEmpty()) return "No Keno draws found.";
                    
                    StringBuilder sb = new StringBuilder("🎱 **Latest 10 Keno Draws**\n\n");
                    
                    for (int i = 0; i < draws.size(); i++) {
                        JSONObject draw = draws.getJSONObject(i);
                        
                        // Updated keys based on user-provided JSON sample
                        String date = draw.getString("drawDate");
                        String time = draw.getString("drawTime");
                        String number = draw.getString("drawNbr");
                        JSONArray winning = draw.getJSONArray("drawNbrs");
                        Double bonus = draw.getDouble("drawBonus");
                        
                        // Fallbacks
                        if (date == null) date = draw.getString("date");
                        if (number == null) number = draw.getString("drawNumber");
                        if (winning == null) winning = draw.getJSONArray("numbers");
                        if (winning == null) winning = draw.getJSONArray("results");
                        if (winning == null) winning = draw.getJSONArray("winningNumbers");
                        
                        String dateTime = (time != null) ? date + " " + time : date;
                        
                        sb.append(String.format("**Draw #%s** (%s)\n", number != null ? number : "N/A", dateTime != null ? dateTime : "N/A"));
                        if (winning != null) {
                            sb.append("Numbers: ").append(winning.toString()).append("\n");
                        }
                        if (bonus != null) {
                             sb.append("Bonus: ").append(bonus).append("\n");
                        } else if (draw.containsKey("bonusNumber")) {
                             sb.append("Bonus: ").append(draw.getString("bonusNumber")).append("\n");
                        }
                        sb.append("\n");
                    }
                    return sb.toString();
                } else if (parsed instanceof JSONObject) {
                    return "Received object instead of list:\n" + parsed.toString();
                } else {
                    return "Unexpected data format: " + parsed.toString();
                }

            } catch (Exception e) {
                log.error("Failed to parse Keno JSON", e);
                return "Raw Output:\n" + output;
            }

        } catch (Exception e) {
            log.error("Failed to execute KenoWinningNumbersSkill", e);
            return "Failed to execute Keno skill: " + e.getMessage();
        }
    }
}
