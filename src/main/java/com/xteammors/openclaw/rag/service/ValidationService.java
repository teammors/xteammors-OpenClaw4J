package com.xteammors.openclaw.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    private final ChatClient chatClient;

    public ValidationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 验证DeepSeek API连接
     */
    public boolean validateDeepSeekConnection() {
        try {
            ChatResponse response = chatClient.prompt()
                    .system("请回复'连接成功'")
                    .user("测试连接")
                    .call()
                    .chatResponse();

            String answer = response.getResult().getOutput().getText();
            log.info("DeepSeek API连接测试成功: {}", answer);
            return true;
        } catch (Exception e) {
            log.error("DeepSeek API连接测试失败", e);
            return false;
        }
    }
}