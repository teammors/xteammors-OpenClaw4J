package com.xteammors.openclaw.skills;

import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.skills.task.ScheduledTask;
import com.xteammors.openclaw.skills.task.ScheduledTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

@Service
public class ScheduledTaskSkill implements AgentSkill {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskSkill.class);

    @Autowired
    private ScheduledTaskManager taskManager;

    @Autowired
    private ChatClient chatClient;

    @Override
    public String getName() {
        return "scheduled-task";
    }

    @Override
    public String getDescription() {
        return "定时任务技能，用于创建和管理定时执行的任务";
    }

    @Override
    public String execute(String input, String chatId) {
        log.info("Executing ScheduledTaskSkill with input: {}", input);

        try {
            // 解析用户输入
            TaskRequest taskRequest = parseTaskRequest(input);
            if (taskRequest == null) {
                return "无法解析定时任务请求，请提供正确的时间间隔和任务描述";
            }

            // 处理任务请求
            if (taskRequest.isListRequest) {
                return listTasks(chatId);
            } else if (taskRequest.isCancelAllRequest) {
                return cancelAllTasks(chatId);
            } else if (taskRequest.isCancelRequest) {
                return cancelTask(chatId, taskRequest.description);
            } else {
                return createTask(chatId, taskRequest);
            }

        } catch (Exception e) {
            log.error("Failed to execute ScheduledTaskSkill", e);
            return "执行定时任务失败: " + e.getMessage();
        }
    }

    /**
     * 解析任务请求
     */
    private TaskRequest parseTaskRequest(String input) {
        log.info("Parsing task request: {}", input);
        
        if (input == null || input.trim().isEmpty()) {
            log.info("Input is null or empty");
            return null;
        }

        String trimmedInput = input.trim();
        log.info("Trimmed input: {}", trimmedInput);

        // 移除 "scheduled-task" 前缀
        String taskInput = trimmedInput;
        if (taskInput.startsWith("scheduled-task")) {
            taskInput = taskInput.substring("scheduled-task".length()).trim();
            log.info("Removed scheduled-task prefix, new input: {}", taskInput);
        }

        // 使用AI解析用户意图
        TaskRequest request = parseWithAI(taskInput);
        if (request != null) {
            log.info("AI parsed task request: {}", request);
            return request;
        }

        // 如果AI解析失败，回退到手动解析
        return parseManually(taskInput, trimmedInput);
    }

    /**
     * 使用AI解析用户意图
     */
    private TaskRequest parseWithAI(String input) {
        String prompt = "请分析用户的定时任务请求，并按照以下JSON格式输出：\n" +
                "{\n" +
                "  \"intent\": \"create\" | \"list\" | \"cancel\" | \"cancel_all\",\n" +
                "  \"interval\": \"时间间隔，如 2m, 5s, 1h\",\n" +
                "  \"skill\": \"技能名称\",\n" +
                "  \"params\": \"技能参数\",\n" +
                "  \"description\": \"任务描述\"\n" +
                "}\n\n" +
                "用户输入: " + input + "\n\n" +
                "说明：\n" +
                "- intent：create表示创建任务，list表示列出任务，cancel表示取消任务，cancel_all表示取消所有任务\n" +
                "- interval：仅当intent为create时需要，格式为数字+单位(s/m/h/d)\n" +
                "- skill：仅当intent为create时需要，技能的名称\n" +
                "- params：仅当intent为create时需要，技能的参数\n" +
                "- description：任务的描述，当intent为cancel时，用于匹配要取消的任务\n" +
                "- 当用户输入包含\"所有\"、\"全部\"等词时，intent应设为cancel_all\n" +
                "\n" +
                "请只输出JSON，不要输出其他任何内容。";

        try {
            String response = chatClient.prompt(prompt).call().content();
            log.info("AI response for task parsing: {}", response);
            
            // 解析JSON
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSONObject.parseObject(response);
            String intent = json.getString("intent");
            
            TaskRequest request = new TaskRequest();
            
            switch (intent) {
                case "create":
                    request.interval = json.getString("interval");
                    request.skill = json.getString("skill");
                    request.params = json.getString("params");
                    request.description = json.getString("description");
                    break;
                case "list":
                    request.isListRequest = true;
                    break;
                case "cancel":
                    request.isCancelRequest = true;
                    request.description = json.getString("description");
                    break;
                case "cancel_all":
                    request.isCancelAllRequest = true;
                    break;
                default:
                    return null;
            }
            
            return request;
        } catch (Exception e) {
            log.error("Error parsing task request with AI", e);
            return null;
        }
    }

    /**
     * 手动解析任务请求（回退方案）
     */
    private TaskRequest parseManually(String taskInput, String trimmedInput) {
        // 检查是否是取消所有任务的请求
        if ((trimmedInput.contains("取消") || trimmedInput.contains("停止")) && 
            (trimmedInput.contains("所有") || trimmedInput.contains("全部"))) {
            log.info("Detected cancel all request");
            TaskRequest request = new TaskRequest();
            request.isCancelAllRequest = true;
            return request;
        }

        // 检查是否是取消任务的请求
        if (trimmedInput.contains("取消") || trimmedInput.contains("停止")) {
            log.info("Detected cancel request");
            TaskRequest request = new TaskRequest();
            request.isCancelRequest = true;
            request.description = trimmedInput.replaceAll("取消|停止", "").trim();
            return request;
        }

        // 检查是否是列出任务的请求
        if (trimmedInput.contains("列出") || trimmedInput.contains("查看")) {
            log.info("Detected list request");
            TaskRequest request = new TaskRequest();
            request.isListRequest = true;
            return request;
        }

        // 解析创建任务的请求
        // 匹配时间间隔模式：数字 + 单位 (秒/分钟/小时/天)
        // 增加对中文数字的支持
        Pattern intervalPattern = Pattern.compile("(\\d+)\\s*(秒|分钟|小时|天|s|m|h|d)", Pattern.CASE_INSENSITIVE);
        Matcher intervalMatcher = intervalPattern.matcher(taskInput);

        if (!intervalMatcher.find()) {
            log.info("No interval pattern found");
            // 尝试更宽松的匹配
            Pattern loosePattern = Pattern.compile("(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher looseMatcher = loosePattern.matcher(taskInput);
            if (looseMatcher.find()) {
                log.info("Found loose interval pattern");
                // 默认使用分钟
                String intervalValue = looseMatcher.group(1);
                String interval = intervalValue + "m";
                
                // 提取技能和参数
                String skillInfo = extractSkillInfo(taskInput);
                String[] skillParts = skillInfo.split("\\s+", 2);
                String skill = skillParts[0];
                String params = skillParts.length > 1 ? skillParts[1] : "";

                TaskRequest request = new TaskRequest();
                request.interval = interval;
                request.skill = skill;
                request.params = params;
                request.description = trimmedInput;

                return request;
            }
            return null;
        }

        // 提取时间间隔
        String intervalValue = intervalMatcher.group(1);
        String intervalUnit = intervalMatcher.group(2).toLowerCase();
        log.info("Found interval: {} {}", intervalValue, intervalUnit);

        // 转换为标准格式
        String interval;
        switch (intervalUnit) {
            case "秒":
            case "s":
                interval = intervalValue + "s";
                break;
            case "分钟":
            case "m":
                interval = intervalValue + "m";
                break;
            case "小时":
            case "h":
                interval = intervalValue + "h";
                break;
            case "天":
            case "d":
                interval = intervalValue + "d";
                break;
            default:
                log.info("Unknown interval unit: {}", intervalUnit);
                return null;
        }

        // 提取任务描述
        String description = trimmedInput;

        // 提取技能和参数（使用AI解析）
        String skillInfo = extractSkillInfo(taskInput);
        log.info("Extracted skill info: {}", skillInfo);
        
        String[] skillParts = skillInfo.split("\\s+", 2);
        String skill = skillParts[0];
        String params = skillParts.length > 1 ? skillParts[1] : "";
        log.info("Skill: {}, Params: {}", skill, params);

        TaskRequest request = new TaskRequest();
        request.interval = interval;
        request.skill = skill;
        request.params = params;
        request.description = description;

        return request;
    }

    /**
     * 使用AI提取技能信息
     */
    private String extractSkillInfo(String input) {
        String prompt = "请从以下用户输入中提取要执行的技能名称和参数，只输出技能名称和参数，不要输出其他任何内容。\n\n" +
                "用户输入: " + input + "\n\n" +
                "例如：\n" +
                "输入: 帮我每间隔2分钟收集一次BTC，ETH，SOL的实时价格\n" +
                "输出: crypto-price BTC,ETH,SOL\n" +
                "输入: 每小时提醒我检查邮件\n" +
                "输出: send-email 提醒检查邮件";

        try {
            String response = chatClient.prompt(prompt).call().content();
            log.info("AI response for skill extraction: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error extracting skill info with AI", e);
            // 回退到默认处理
            if (input.contains("价格") || input.contains("crypto")) {
                return "crypto-price BTC,ETH,SOL";
            } else if (input.contains("邮件")) {
                return "send-email";
            } else if (input.contains("状态")) {
                return "system-status";
            } else {
                return "crypto-price BTC";
            }
        }
    }

    /**
     * 创建定时任务
     */
    private String createTask(String chatId, TaskRequest request) {
        try {
            ScheduledTask task = new ScheduledTask(
                    chatId,
                    request.interval,
                    request.skill,
                    request.params,
                    request.description
            );

            task = taskManager.addTask(task);

            // 格式化输出
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String nextRun = task.getNextRun().format(formatter);

            return "定时任务已创建成功！\n\n" +
                    "任务ID: " + task.getTaskId() + "\n" +
                    "描述: " + task.getDescription() + "\n" +
                    "间隔: " + formatInterval(task.getInterval()) + "\n" +
                    "下次执行: " + nextRun;

        } catch (Exception e) {
            log.error("Error creating scheduled task", e);
            return "创建定时任务失败: " + e.getMessage();
        }
    }

    /**
     * 列出用户的定时任务
     */
    private String listTasks(String chatId) {
        List<ScheduledTask> tasks = taskManager.getTasksByChatId(chatId);

        if (tasks.isEmpty()) {
            return "您当前没有定时任务";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("您当前的定时任务：\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < tasks.size(); i++) {
            ScheduledTask task = tasks.get(i);
            sb.append((i + 1)).append(". 任务ID: ").append(task.getTaskId()).append("\n");
            sb.append("   描述: ").append(task.getDescription()).append("\n");
            sb.append("   间隔: ").append(formatInterval(task.getInterval())).append("\n");
            sb.append("   状态: ").append(task.isActive() ? "运行中" : "已暂停").append("\n");
            sb.append("   下次执行: ").append(task.getNextRun().format(formatter)).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 取消定时任务
     */
    private String cancelTask(String chatId, String description) {
        log.info("Attempting to cancel task with description: {}", description);
        
        // 尝试通过描述取消任务
        boolean cancelled = taskManager.cancelTaskByDescription(chatId, description);
        
        // 如果精确匹配失败，尝试模糊匹配
        if (!cancelled) {
            List<ScheduledTask> userTasks = taskManager.getTasksByChatId(chatId);
            for (ScheduledTask task : userTasks) {
                // 增强模糊匹配：提取关键词
                if (isTaskDescriptionMatch(task.getDescription(), description)) {
                    log.info("Found matching task by fuzzy match: {} vs {}", task.getDescription(), description);
                    cancelled = taskManager.cancelTask(task.getTaskId());
                    if (cancelled) {
                        break;
                    }
                }
            }
        }
        
        if (cancelled) {
            return "定时任务已取消";
        } else {
            return "未找到指定的定时任务，请使用'列出定时任务'查看当前任务";
        }
    }
    
    /**
     * 判断任务描述是否匹配（支持模糊匹配）
     */
    private boolean isTaskDescriptionMatch(String taskDesc, String userDesc) {
        log.info("Fuzzy matching task description - User: '{}', Task: '{}'", userDesc, taskDesc);
        
        // 回退到原始的包含关系检查
        if (taskDesc.contains(userDesc) || userDesc.contains(taskDesc)) {
            return true;
        }
        
        // 提取关键实词进行匹配
        // 将中文描述按常见虚词分隔
        String[] separators = {"的", "了", "我", "要", "帮", "请", "一个", "一条", "个", "条", "推", "送"};
        
        String userText = userDesc.toLowerCase();
        String taskText = taskDesc.toLowerCase();
        
        // 逐步移除分隔符，保留实词
        for (String sep : separators) {
            userText = userText.replace(sep, " ");
            taskText = taskText.replace(sep, " ");
        }
        
        // 清理多余空格
        userText = userText.replaceAll("\\s+", " ").trim();
        taskText = taskText.replaceAll("\\s+", " ").trim();
        
        log.info("After removing separators - User: '{}', Task: '{}'", userText, taskText);
        
        // 按空格分词
        String[] userWords = userText.split(" ");
        String[] taskWords = taskText.split(" ");
        
        // 统计匹配的词汇数量
        int matchCount = 0;
        for (String uWord : userWords) {
            if (uWord.length() < 1) continue;
            for (String tWord : taskWords) {
                if (tWord.length() < 1) continue;
                // 如果用户词包含在任务词中，或者完全匹配
                if (uWord.equals(tWord) || (uWord.length() >= 2 && tWord.contains(uWord))) {
                    matchCount++;
                    log.info("Matched word: user='{}', task='{}'", uWord, tWord);
                    break;
                }
            }
        }
        
        log.info("Word match count: {} (user words: {}, task words: {})", 
                 matchCount, userWords.length, taskWords.length);
        
        // 如果有至少 1 个匹配的词，且用户输入较短 (<=3 个词),则认为匹配
        if (matchCount >= 1 && userWords.length <= 3) {
            return true;
        }
        
        // 特殊处理：如果用户说"取消系统状态"而任务是"2 分钟推送一条系统状态"
        // 只要包含"系统状态"就应该匹配
        if (userText.contains("系统状态") && taskText.contains("系统状态")) {
            log.info("Special case matched: both contain '系统状态'");
            return true;
        }
        if (userText.contains("天气") && taskText.contains("天气")) {
            log.info("Special case matched: both contain '天气'");
            return true;
        }
        
        return false;
    }

    /**
     * 取消所有定时任务
     */
    private String cancelAllTasks(String chatId) {
        int cancelledCount = taskManager.cancelAllTasks(chatId);
        return "已取消 " + cancelledCount + " 个定时任务";
    }

    /**
     * 格式化时间间隔
     */
    private String formatInterval(String interval) {
        int value = Integer.parseInt(interval.replaceAll("\\D", ""));
        String unit = interval.replaceAll("\\d", "");

        switch (unit) {
            case "s":
                return value + "秒";
            case "m":
                return value + "分钟";
            case "h":
                return value + "小时";
            case "d":
                return value + "天";
            default:
                return interval;
        }
    }

    /**
     * 任务请求类
     */
    private static class TaskRequest {
        boolean isListRequest;
        boolean isCancelRequest;
        boolean isCancelAllRequest;
        String interval;
        String skill;
        String params;
        String description;
    }
}
