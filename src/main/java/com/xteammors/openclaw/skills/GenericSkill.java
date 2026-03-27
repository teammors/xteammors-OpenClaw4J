package com.xteammors.openclaw.skills;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xteammors.openclaw.property.SkillsDirProperty;
import com.xteammors.openclaw.skills.ScheduledTaskSkill;
import com.xteammors.openclaw.skills.SkillGeneratorSkill;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.ShellUtils;
import com.xteammors.openclaw.wssdk.XMessageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class GenericSkill implements AgentSkill {
    private static final Logger log = LoggerFactory.getLogger(GenericSkill.class);

    @Autowired
    private SkillsDirProperty skillsDirProperty;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ScheduledTaskSkill scheduledTaskSkill;
    
    @Autowired
    private SkillGeneratorSkill skillGeneratorSkill;

    // 火山引擎大模型配置（传给 Python 脚本使用）
    @Value("${volcengine.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String volcengineBaseUrl;

    @Value("${volcengine.api-key:}")
    private String volcengineApiKey;

    @Value("${volcengine.model:doubao-seed-2-0-pro-260215}")
    private String volcengineModel;

    // Java 端 ChatClient 仍使用 spring.ai.openai 配置（DeepSeek）

    @Override
    public String getName() {
        return "generic-skill";
    }

    @Override
    public String getDescription() {
        return "通用技能执行器，用于执行各种技能脚本";
    }

    @Override
    public String execute(String input, String chatId) {
        log.info("Executing GenericSkill with input: {}", input);

        try {
            // 首先检查是否有技能生成器的待处理状态
            if (skillGeneratorSkill.hasPendingRequest(chatId)) {
                // 如果用户有待处理的技能生成请求，直接交给技能生成器处理
                return skillGeneratorSkill.execute(input, chatId);
            }

            // 1. 提取技能名称和参数
            SkillRequest skillRequest = parseSkillRequest(input);
            if (skillRequest == null) {
                return "无法解析技能请求，请提供正确的技能名称和参数";
            }

            String skillName = skillRequest.getSkillName();
            String skillParams = skillRequest.getParams();

            log.info("Skill name: {}, params: {}", skillName, skillParams);

            // 2. 处理特殊技能
            if ("scheduled-task".equals(skillName)) {
                // 调用定时任务技能
                return scheduledTaskSkill.execute(input, chatId);
            } else if ("skill-generator".equals(skillName)) {
                // 调用技能生成器技能
                return skillGeneratorSkill.execute(skillParams, chatId);
            }

            // 3. 查找技能脚本
            String scriptPath = findSkillScript(skillName);
            if (scriptPath == null) {
                // 技能未找到，调用技能生成器
                return skillGeneratorSkill.execute(input, chatId);
            }

            // 3. 执行技能脚本
            String output = executeSkillScript(scriptPath, skillParams,chatId);

            // 4. 处理技能输出
            return processSkillOutput(output);

        } catch (Exception e) {
            log.error("Failed to execute GenericSkill", e);
            return "执行技能失败: " + e.getMessage();
        }
    }

    /**
     * 解析技能请求
     * 格式: 技能名称 [参数]
     */
    private SkillRequest parseSkillRequest(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmedInput = input.trim();
        int firstSpaceIndex = trimmedInput.indexOf(' ');

        if (firstSpaceIndex == -1) {
            // 只有技能名称，无参数
            return new SkillRequest(trimmedInput, "");
        } else {
            // 有技能名称和参数
            String skillName = trimmedInput.substring(0, firstSpaceIndex);
            String params = trimmedInput.substring(firstSpaceIndex + 1).trim();
            return new SkillRequest(skillName, params);
        }
    }

    /**
     * 查找技能脚本
     */
    private String findSkillScript(String skillName) {
        String skillsDir = skillsDirProperty.getDir();
        if (skillsDir == null || skillsDir.isEmpty()) {
            log.warn("Skills directory not configured");
            return null;
        }

        // 技能目录结构: skills/{skillName}/scripts/{scriptName}.py
        String scriptPath = skillsDir + File.separator + skillName + File.separator + "scripts";

        // 查找脚本文件
        File scriptDir = new File(scriptPath);
        if (!scriptDir.exists() || !scriptDir.isDirectory()) {
            log.warn("Script directory not found: {}", scriptPath);
            return null;
        }

        // 查找 Python 脚本文件
        File[] scriptFiles = scriptDir.listFiles((dir, name) -> name.endsWith(".py"));
        if (scriptFiles == null || scriptFiles.length == 0) {
            log.warn("No Python scripts found in: {}", scriptPath);
            return null;
        }

        // 返回第一个脚本文件
        return scriptFiles[0].getAbsolutePath();
    }

    /**
     * 执行技能脚本
     * 所有Python脚本都会通过环境变量获取火山引擎大模型配置，
     * 任何Python脚本都可以直接调用大模型
     */
    private String executeSkillScript(String scriptPath, String params, String chatId) {
        log.info("Executing skill script: {} with params: {}", scriptPath, params);

        String pythonCommand = ShellUtils.getPythonCommand();

        // 对于聊天记录总结技能，走独立的两次AI调用流程
        if (scriptPath.contains("chat-summary")) {
            return executeChatSummarySkill(scriptPath, params, chatId);
        }

        // 准备环境变量：火山引擎大模型配置，所有Python脚本可用
        Map<String, String> env = buildLlmEnv();

        // 根据技能类型构建不同的命令参数
        String[] command;
        if (scriptPath.contains("crypto-price")) {
            String symbols = extractCryptoSymbols(params);
            command = new String[]{pythonCommand, scriptPath, "--symbols", symbols};
        } else if (scriptPath.contains("send-email")) {
            String emailParams = parseEmailParamsWithAI(params);
            if (params == null || params.isEmpty()) {
                command = new String[]{pythonCommand, scriptPath};
            } else {
                command = new String[]{pythonCommand, scriptPath, emailParams};
            }
        } else if (scriptPath.contains("weather-forecast")) {
            String weatherParams = getCityCoordinatesWithAI(params);
            if (params == null || params.isEmpty()) {
                command = new String[]{pythonCommand, scriptPath};
            } else {
                command = new String[]{pythonCommand, scriptPath, weatherParams};
            }
        } else {
            // 默认：直接传参（包括 ppt-generator 等）
            if (params == null || params.isEmpty()) {
                command = new String[]{pythonCommand, scriptPath};
            } else {
                command = new String[]{pythonCommand, scriptPath, params};
            }
        }

        // 所有脚本都通过 execWithEnv 执行，确保大模型环境变量可用
        return ShellUtils.execWithEnv(env, command);
    }

    /**
     * 构建大模型环境变量，供任何 Python 脚本调用火山引擎 API
     */
    private Map<String, String> buildLlmEnv() {
        Map<String, String> env = new HashMap<>();
        if (volcengineBaseUrl != null && !volcengineBaseUrl.isBlank()) {
            env.put("VOLCENGINE_BASE_URL", volcengineBaseUrl);
        }
        if (volcengineApiKey != null && !volcengineApiKey.isBlank()) {
            env.put("VOLCENGINE_API_KEY", volcengineApiKey);
        }
        if (volcengineModel != null && !volcengineModel.isBlank()) {
            env.put("VOLCENGINE_MODEL", volcengineModel);
        }
        return env;
    }

    /**
     * 执行聊天记录总结技能
     * 需要两次AI调用：1. 构建SQL 2. 总结内容
     */
    private String executeChatSummarySkill(String scriptPath, String userInput, String chatId) {
        log.info("Executing chat-summary skill with input: {}, chatId: {}", userInput, chatId);
        
        try {
            // Step 1: 使用AI构建SQL查询语句
            String sql = buildChatSummarySQLWithAI(userInput, chatId);
            if (sql == null || sql.isEmpty()) {
                return "无法构建SQL查询语句";
            }
            log.info("Generated SQL: {}", sql);
            
            // Step 2: 执行Python脚本查询数据库
            String pythonCommand = ShellUtils.getPythonCommand();
            String queryScriptPath = scriptPath.replace("summarize_chat.py", "query_chat.py");
            String[] command = new String[]{pythonCommand, queryScriptPath, sql};
            String dbOutput = ShellUtils.exec(command);
            
            if (dbOutput == null || dbOutput.isEmpty() || dbOutput.startsWith("Error")) {
                return "查询数据库失败: " + dbOutput;
            }
            
            log.info("Database query returned {} characters", dbOutput.length());
            
            // Step 3: 使用AI总结聊天内容
            String summary = summarizeChatWithAI(dbOutput, userInput);
            return summary;
            
        } catch (Exception e) {
            log.error("Error executing chat-summary skill", e);
            return "执行聊天记录总结失败: " + e.getMessage();
        }
    }
    
    /**
     * 使用AI构建聊天记录总结的SQL查询
     */
    private String buildChatSummarySQLWithAI(String userInput, String chatId) {
        String currentTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        String prompt = "你是一个SQL专家。请根据用户的需求和数据库表结构，构建一个MySQL查询语句。\n\n" +
                "当前时间: " + currentTime + "\n" +
                "用户输入: " + userInput + "\n" +
                "聊天ID (chatId): " + chatId + "\n\n" +
                "数据库表结构:\n" +
                "表名: chat_messages\n" +
                "关键字段:\n" +
                "- id: varchar(255), 消息ID\n" +
                "- text: text, 消息内容文本\n" +
                "- timestamp: varchar(50), 入库时间，格式: 2026-03-25 00:59:56\n" +
                "- mediaUrls: text, 媒体文件URL\n" +
                "- chatId: varchar(255), 聊天ID\n\n" +
                "用户需求分析:\n" +
                "- \"刚才\"、\"最近\"通常指最近1-2小时\n" +
                "- \"下午\"指12:00-18:00\n" +
                "- \"上午\"指08:00-12:00\n" +
                "- \"今天\"指今天00:00到现在\n" +
                "- \"昨天\"指昨天00:00-23:59\n\n" +
                "请构建一个SQL查询语句，要求:\n" +
                "1. 只查询 text、timestamp、mediaUrls 三个字段\n" +
                "2. 必须包含 WHERE chatId = '" + chatId + "' 条件\n" +
                "3. 根据用户的时间要求添加时间过滤条件\n" +
                "4. 按 timestamp 升序排列\n" +
                "5. 最多返回500条记录\n\n" +
                "请只输出SQL语句，不要有任何其他文字或解释。";
        
        try {
            String aiResponse = chatClient.prompt(prompt).call().content();
            log.info("AI response for SQL building: {}", aiResponse);
            
            // 清理SQL语句，移除markdown代码块标记
            String sql = aiResponse.replaceAll("(?s)```sql\\s*", "").replaceAll("(?s)```\\s*", "").trim();
            return sql;
        } catch (Exception e) {
            log.error("Error building SQL with AI", e);
            return null;
        }
    }
    
    /**
     * 使用AI总结聊天内容
     */
    private String summarizeChatWithAI(String chatData, String userInput) {
        // 限制数据长度，避免超出token限制
        if (chatData.length() > 10000) {
            chatData = chatData.substring(0, 10000) + "\n... (数据已截断)";
        }
        
        String prompt = "请总结以下聊天记录的要点。\n\n" +
                "用户原始需求: " + userInput + "\n\n" +
                "聊天记录数据 (JSON格式):\n" + chatData + "\n\n" +
                "请按照以下格式输出总结:\n\n" +
                "时间段: [开始时间] - [结束时间]\n\n" +
                "聊天内容要点:\n" +
                "1. 要点1\n" +
                "2. 要点2\n" +
                "3. 要点3\n" +
                "...\n\n" +
                "要求:\n" +
                "1. 提取关键信息和决策\n" +
                "2. 按重要性排序\n" +
                "3. 简明扼要，每个要点一句话\n" +
                "4. 如果涉及媒体文件，请说明类型\n" +
                "5. 不要遗漏重要信息";
        
        try {
            String aiResponse = chatClient.prompt(prompt).call().content();
            log.info("AI response for chat summary: {}", aiResponse);
            return aiResponse;
        } catch (Exception e) {
            log.error("Error summarizing chat with AI", e);
            return "总结聊天内容失败: " + e.getMessage();
        }
    }
    
    /**
     * 使用AI解析邮件参数
     */
    private String parseEmailParamsWithAI(String userInput) {
        log.info("Parsing email params with AI for input: {}", userInput);
        
        // 构建提示词，让AI提取邮件参数
        String prompt = "请从以下用户输入中提取邮件的收件人、主题和内容，并按照以下格式输出：\n" +
                "收件人: <邮箱地址>\n" +
                "主题: <邮件主题>\n" +
                "内容: <邮件内容>\n\n" +
                "用户输入：" + userInput + "\n\n" +
                "注意：\n" +
                "1. 只提取实际的邮件内容，不要包含帮我发邮件等指令性话语\n" +
                "2. 如果用户没有明确指定主题，请根据内容自动生成一个合适的主题\n" +
                "3. 邮件内容应该是正式的，不包含用户的指令性语言\n" +
                "4. 只输出提取的信息，不要输出其他任何内容\n" +
                "5. 确保输出格式严格按照要求，不要有任何额外的文字\n";
        
        try {
            // 调用AI大模型
            String aiResponse = chatClient.prompt(prompt).call().content();
            log.info("AI response for email parsing: {}", aiResponse);
            
            // 提取AI返回的信息
            return aiResponse;
        } catch (Exception e) {
            log.error("Error parsing email params with AI", e);
            // 如果AI解析失败，返回原始输入
            return userInput;
        }
    }

    /**
     * 从用户输入中提取加密货币符号
     */
    private String extractCryptoSymbols(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "BTC,ETH"; // 默认符号
        }

        // 简单的符号提取逻辑
        String[] commonSymbols = {"BTC", "ETH", "SOL", "DOGE", "ADA", "DOT", "BCH", "LTC", "LINK", "XLM"};
        StringBuilder symbols = new StringBuilder();
        String inputUpper = input.toUpperCase();

        for (String symbol : commonSymbols) {
            if (inputUpper.contains(symbol)) {
                if (symbols.length() > 0) {
                    symbols.append(",");
                }
                symbols.append(symbol);
            }
        }

        // 如果没有找到符号，使用默认值
        if (symbols.length() == 0) {
            return "BTC,ETH";
        }

        return symbols.toString();
    }
    
    /**
     * 使用AI获取城市经纬度
     */
    private String getCityCoordinatesWithAI(String userInput) {
        log.info("Getting city coordinates with AI for input: {}", userInput);
        
        // 构建提示词，让AI提取城市名称并返回经纬度
        String prompt = "请从以下用户输入中提取城市名称，并返回该城市的经纬度。\n\n" +
                "用户输入：" + userInput + "\n\n" +
                "请按照以下格式输出：\n" +
                "城市：<城市名称>\n" +
                "纬度：<纬度数值>\n" +
                "经度：<经度数值>\n\n" +
                "注意：\n" +
                "1. 只提取实际的城市名称，不要包含其他信息\n" +
                "2. 确保经纬度数值是正确的，使用小数格式\n" +
                "3. 只输出提取的信息，不要输出其他任何内容\n" +
                "4. 如果无法确定城市，请返回一个常见城市的经纬度\n";
        
        try {
            // 调用AI大模型
            String aiResponse = chatClient.prompt(prompt).call().content();
            log.info("AI response for city coordinates: {}", aiResponse);
            
            // 提取AI返回的信息
            return aiResponse;
        } catch (Exception e) {
            log.error("Error getting city coordinates with AI", e);
            // 如果AI调用失败，返回原始输入
            return userInput;
        }
    }

    /**
     * 处理技能输出
     */
    private String processSkillOutput(String output) {
        if (output == null || output.trim().isEmpty()) {
            return "技能未返回任何结果";
        }

        // 直接返回脚本的原始输出，因为脚本已经直接输出了格式化的文本
        return output;
    }

    /**
     * 技能请求类
     */
    private static class SkillRequest {
        private final String skillName;
        private final String params;

        public SkillRequest(String skillName, String params) {
            this.skillName = skillName;
            this.params = params;
        }


        public String getSkillName() {
            return skillName;
        }

        public String getParams() {
            return params;
        }
    }
}
