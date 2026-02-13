package com.xteammors.openclaw.adapter;

import com.xteammors.openclaw.rag.service.RAGService;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.PositiveIntegerValidator;
import com.teammors.robot.ws.TRobotClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class TeammorsMessageAdapter implements Runnable {

    String chatId;
    String message;


    private final RAGService ragService;
    private final Map<String, AgentSkill> skillMap;

    public TeammorsMessageAdapter(String chatId, String message, RAGService ragService, List<AgentSkill> skills) {
        this.chatId = chatId;
        this.message = message;
        this.ragService = ragService;
        this.skillMap = skills.stream().collect(Collectors.toMap(s -> s.getName().toLowerCase(), s -> s));
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
            
            // Optional: Send "Processing..." message
            // String preMessage = "TeamMbot is processing...";
            // processResponse(chatId, requestId, preMessage);

            String completeMessage = "";
            
            // 1. Call RAG Service
            String ragResponse = ragService.answerQuestion(message);
            
            // 2. Check for Skill Invocation
            if (ragResponse.startsWith("CALL_SKILL:")) {
                String skillName = ragResponse.substring("CALL_SKILL:".length()).trim().toLowerCase();
                log.info("RAG suggested calling skill: {}", skillName);
                
                AgentSkill skill = skillMap.get(skillName);
                if (skill != null) {
                    try {
                        // Pass the original user message to the skill
                        completeMessage = skill.execute(message, chatId);
                    } catch (Exception e) {
                        log.error("Error executing skill {}", skillName, e);
                        completeMessage = "Error executing skill: " + e.getMessage();
                    }
                } else {
                    completeMessage = "Sorry, I don't know how to execute the skill: " + skillName;
                }
            } else {
                completeMessage = ragResponse;
            }
            
            if (completeMessage != null && !completeMessage.trim().isEmpty()) {
                processResponse(chatId, requestId, completeMessage);
            }

        } catch (Exception e) {
            log.error("Error asking TeamMbot", e);
            e.printStackTrace();
        }
        return true;
    }

    private boolean processResponse(String chatId, String requestId, String responseBody) {
        if (PositiveIntegerValidator.isPositiveInteger(chatId)) {
            String toUid = TRobotClient.instance().mId + "_" + chatId;
            return TRobotClient.instance().sendSingleUserTxtMessage(responseBody, toUid, 1);
        } else {
            return TRobotClient.instance().sendToGroupTxtMessage(responseBody, chatId, 1);
        }
    }
}
