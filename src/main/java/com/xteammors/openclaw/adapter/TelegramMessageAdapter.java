package com.xteammors.openclaw.adapter;

import com.xteammors.openclaw.proxy.TelegramMessageProxy;
import com.xteammors.openclaw.rag.service.RAGService;
import com.xteammors.openclaw.skills.GenericSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramMessageAdapter  implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(TelegramMessageAdapter.class);

    String chatId;
    String message;
    TelegramMessageProxy telegramMessageProxy;

    private final RAGService ragService;
    private final GenericSkill genericSkill;

    public TelegramMessageAdapter(String chatId, String message, RAGService ragService, GenericSkill genericSkill, TelegramMessageProxy telegramProxy) {
        this.chatId = chatId;
        this.message = message;
        this.ragService = ragService;
        this.genericSkill = genericSkill;
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

            try {
                // 使用通用技能执行器执行技能
                // 格式: "skillName params"
                String skillRequest = skillName + " " + message;
                completeMessage = genericSkill.execute(skillRequest, chatId);
            } catch (Exception e) {
                log.error("Error executing skill {}", skillName, e);
                completeMessage = "Error executing skill: " + e.getMessage();
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
