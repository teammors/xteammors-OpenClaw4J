package com.xteammors.openclaw.adapter;

import com.xteammors.openclaw.context.ChatContextManager;
import com.xteammors.openclaw.rag.service.RAGService;
import com.xteammors.openclaw.skills.GenericSkill;
import com.xteammors.openclaw.skills.SkillGeneratorSkill;
import com.xteammors.openclaw.utils.PositiveIntegerValidator;
import com.xteammors.openclaw.wssdk.XMessageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class TeammorsMessageAdapter implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TeammorsMessageAdapter.class);

    String chatId;
    String message;

    private final RAGService ragService;
    private final GenericSkill genericSkill;
    private final SkillGeneratorSkill skillGeneratorSkill;

    public TeammorsMessageAdapter(String chatId, String message, RAGService ragService, GenericSkill genericSkill, SkillGeneratorSkill skillGeneratorSkill) {
        this.chatId = chatId;
        this.message = message;
        this.ragService = ragService;
        this.genericSkill = genericSkill;
        this.skillGeneratorSkill = skillGeneratorSkill;
    }

    @Override
    public void run() {
        askTeammors();
    }

    private boolean askTeammors() {
        try {
            if (message == null || message.isEmpty()) return false;



            String requestId = UUID.randomUUID().toString();
            log.info("TeamMbot Request [{}]: {}", requestId, message);
            
            // Optional: Send "AI is processing..." message
             String preMessage = "AI is processing...";
             processResponse(chatId, requestId, preMessage);

            String completeMessage = "";
            
            // 首先检查是否有技能生成器的待处理请求（保持这个逻辑，因为这是用户正在与技能生成器交互的情况）
            if (skillGeneratorSkill != null && skillGeneratorSkill.hasPendingRequest(chatId)) {
                log.info("Detected pending skill generator request for chatId: {}", chatId);
                completeMessage = skillGeneratorSkill.execute(message, chatId);
            } else {
                // 1. 先通过 RAG 确定是否有对应的 skill
                String ragResponse = ragService.answerQuestion(message, chatId);
                log.info("TeamMbot Answer [{}]: {}", requestId, ragResponse);
                                
                // 2. Check for Skill Chain Invocation (优先检查技能链)
                int skillChainCallIndex = ragResponse.indexOf("CALL_SKILL_CHAIN:");
                if (skillChainCallIndex != -1) {
                    // 解析技能链
                    String skillChain = extractSkillChain(ragResponse, skillChainCallIndex);
                    log.info("RAG suggested calling skill chain: {}", skillChain);
                                    
                    try {
                        // 执行技能链
                        completeMessage = executeSkillChain(skillChain, message, chatId);
                    } catch (Exception e) {
                        log.error("Error executing skill chain", e);
                        completeMessage = "Error executing skill chain: " + e.getMessage();
                    }
                } else {
                    // 3. Check for Single Skill Invocation
                    int skillCallIndex = ragResponse.indexOf("CALL_SKILL:");
                    if (skillCallIndex != -1) {
                        // 如果有对应的 skill，直接执行
                        String skillName = extractSkillName(ragResponse, skillCallIndex);
                        log.info("RAG suggested calling skill: {}", skillName);
                                        
                        try {
                            // 使用通用技能执行器执行技能
                            // 格式："skillName params"
                            String skillRequest = skillName + " " + message;
                            completeMessage = genericSkill.execute(skillRequest, chatId);
                        } catch (Exception e) {
                            log.error("Error executing skill {}", skillName, e);
                            completeMessage = "Error executing skill: " + e.getMessage();
                        }
                    } else {
                        // 4. 如果没有对应的 skill，调用创建技能的技能
                        log.info("No existing skill found, calling skill generator");
                        try {
                            completeMessage = skillGeneratorSkill.execute(message, chatId);
                        } catch (Exception e) {
                            log.error("Error executing SkillGeneratorSkill", e);
                            completeMessage = ragResponse;
                        }
                    }
                }
            }
            
            if (completeMessage != null && !completeMessage.trim().isEmpty()) {
                processResponse(chatId, requestId, completeMessage);

                // 记录用户消息和AI最终回复到聊天上下文
                ChatContextManager.getInstance().addExchange(chatId, message, completeMessage);
            }

        } catch (Exception e) {
            log.error("Error asking TeamMbot", e);
            e.printStackTrace();
        }
        return true;
    }

    private boolean processResponse(String chatId, String requestId, String responseBody) {

        if(PositiveIntegerValidator.isPositiveInteger(chatId)) {
            String toUid = XMessageClient.instance().mId+"_"+chatId;
            return XMessageClient.instance().sendSingleUserTxtMessage(responseBody,toUid,1);
        }else {
            return XMessageClient.instance().sendToGroupTxtMessage(responseBody,chatId,1);
        }

    }
    
    /**
     * 从 RAG 响应中提取技能链
     */
    private String extractSkillChain(String response, int startIndex) {
        String skillChain = response.substring(startIndex + "CALL_SKILL_CHAIN:".length()).trim();
        // 提取技能链，直到遇到换行或句号
        int endIndex = skillChain.length();
        for (int i = 0; i < skillChain.length(); i++) {
            char c = skillChain.charAt(i);
            if (c == '\n' || c == '。') {
                endIndex = i;
                break;
            }
        }
        skillChain = skillChain.substring(0, endIndex).trim();
        return skillChain;
    }
    
    /**
     * 从 RAG 响应中提取技能名称
     */
    private String extractSkillName(String response, int startIndex) {
        String skillName = response.substring(startIndex + "CALL_SKILL:".length()).trim().toLowerCase();
        // 提取技能名称，直到遇到换行或其他非字母数字字符
        int endIndex = skillName.length();
        for (int i = 0; i < skillName.length(); i++) {
            char c = skillName.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_') {
                endIndex = i;
                break;
            }
        }
        skillName = skillName.substring(0, endIndex).trim();
        return skillName;
    }
    
    /**
     * 执行技能链
     * @param skillChain 技能链字符串，格式："skill1 -> skill2 -> skill3"
     * @param originalMessage 原始用户消息
     * @param chatId 聊天 ID
     * @return 最终执行结果
     */
    private String executeSkillChain(String skillChain, String originalMessage, String chatId) throws Exception {
        // 解析技能链
        String[] skills = skillChain.split("\\s*->\\s*");
        
        if (skills.length == 0) {
            throw new IllegalArgumentException("Invalid skill chain format");
        }
        
        log.info("Executing skill chain with {} skills: {}", skills.length, String.join(", ", skills));
        
        // 关键改进：保留原始消息用于参数提取，同时累积技能结果
        String currentInput = originalMessage;
        StringBuilder accumulatedResults = new StringBuilder();
        String lastResult = null;
        
        // 按顺序执行每个技能
        for (int i = 0; i < skills.length; i++) {
            String skillName = skills[i].trim();
            log.info("Executing skill {}/{}: {}", i + 1, skills.length, skillName);
            
            try {
                // 构造技能请求：始终包含原始消息，这样后续技能也能访问完整参数
                String skillRequest = skillName + " " + currentInput;
                
                log.info("Skill {} request: {}", skillName, skillRequest);
                
                // 执行技能
                String result = genericSkill.execute(skillRequest, chatId);
                
                log.info("Skill {} result: {}", skillName, result);
                
                // 保存结果
                lastResult = result;
                
                // 累积结果用于下一个技能
                if (i < skills.length - 1) {
                    // 对于非最后一个技能，将结果添加到累积器
                    accumulatedResults.append(result).append("\n\n");
                    
                    // 下一个技能的输入 = 原始消息 + 之前技能的结果
                    // 这样可以确保所有原始参数 (如邮箱地址) 仍然可用
                    currentInput = originalMessage + "\n\n[前序技能执行结果]:\n" + accumulatedResults.toString();
                }
                
            } catch (Exception e) {
                log.error("Error executing skill {} in chain", skillName, e);
                throw new RuntimeException("Failed to execute skill " + skillName + ": " + e.getMessage(), e);
            }
        }
        
        // 构建综合回复：结合所有技能的结果和最终操作确认
        StringBuilder finalResponse = new StringBuilder();
        
        // 如果有累积的中间结果，先添加这些结果
        if (!accumulatedResults.isEmpty()) {
            finalResponse.append(accumulatedResults);
        }
        
        // 添加最后一个技能的结果 (如果与之前的结果不同)
        if (lastResult != null && !lastResult.equals(accumulatedResults.toString().trim())) {
            // 检查最后一个技能的结果是否是成功消息
            if (lastResult.contains("sent successfully") || lastResult.contains("Email sent")) {
                // 如果是邮件发送成功的消息，提取邮箱地址并生成友好的确认消息
                String emailMatch = extractEmailsFromSuccessMessage(lastResult);
                if (emailMatch != null) {
                    finalResponse.append("已发送给：").append(emailMatch);
                } else {
                    finalResponse.append(lastResult);
                }
            } else {
                // 其他类型的结果直接添加
                finalResponse.append(lastResult);
            }
        }
        
        return finalResponse.toString().trim();
    }
    
    /**
     * 从邮件发送成功消息中提取邮箱地址列表
     */
    private String extractEmailsFromSuccessMessage(String message) {
        if (message == null) return null;
        
        // 尝试匹配 "Successfully sent emails to N recipients: xxx@xxx.com, yyy@yyy.com" 格式
        java.util.regex.Pattern multiPattern = java.util.regex.Pattern.compile("recipients:\\s*(.+)");
        java.util.regex.Matcher multiMatcher = multiPattern.matcher(message);
        
        if (multiMatcher.find()) {
            // 找到了多个邮箱地址
            return multiMatcher.group(1).trim();
        }
        
        // 尝试匹配 "Email sent successfully to xxx@xxx.com" 格式
        java.util.regex.Pattern singlePattern = java.util.regex.Pattern.compile("to\\s+([\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,})");
        java.util.regex.Matcher singleMatcher = singlePattern.matcher(message);
        
        if (singleMatcher.find()) {
            return singleMatcher.group(1);
        }
        
        return null;
    }
}
