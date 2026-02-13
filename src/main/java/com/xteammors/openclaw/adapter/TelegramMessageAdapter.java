package com.xteammors.openclaw.adapter;

import com.xteammors.openclaw.proxy.TelegramMessageProxy;
import com.xteammors.openclaw.rag.service.RAGService;
import com.xteammors.openclaw.skills.base.AgentSkill;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TelegramMessageAdapter  implements Runnable{

    String chatId;
    String message;
    TelegramMessageProxy telegramMessageProxy;

    private final RAGService ragService;
    private final Map<String, AgentSkill> skillMap;

    public TelegramMessageAdapter(String chatId, String message, RAGService ragService, List<AgentSkill> skills,TelegramMessageProxy telegramProxy) {
        this.chatId = chatId;
        this.message = message;
        this.ragService = ragService;
        this.skillMap = skills.stream().collect(Collectors.toMap(s -> s.getName().toLowerCase(), s -> s));
        this.telegramMessageProxy = telegramProxy;
    }

    @Override
    public void run() {

        // 构建回复消息对象：SendMessage是Bot API的“发送消息”方法
        SendMessage replyMessage = new SendMessage();
        replyMessage.setChatId(chatId); // 聊天ID转字符串（API要求）
        replyMessage.enableMarkdownV2(false); // 暂时关闭Markdown，避免特殊字符报错


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

        replyMessage.setText(completeMessage);


        try {
            telegramMessageProxy.execute(replyMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
