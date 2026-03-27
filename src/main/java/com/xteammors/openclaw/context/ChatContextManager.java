package com.xteammors.openclaw.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天上下文管理器
 * 每个 chatId 维护最近 N 轮对话，超出自动淘汰最早的
 */
public class ChatContextManager {

    private static final Logger log = LoggerFactory.getLogger(ChatContextManager.class);

    private static final ChatContextManager INSTANCE = new ChatContextManager();

    /** 每个 chatId 保留的最多对话轮数 */
    private static final int MAX_ROUNDS = 10;

    /** chatId -> 对话记录（每条为 "role: content" 格式） */
    private final ConcurrentHashMap<String, LinkedList<String>> store = new ConcurrentHashMap<>();

    private ChatContextManager() {}

    public static ChatContextManager getInstance() {
        return INSTANCE;
    }

    /**
     * 添加一轮对话（用户消息 + AI 回复）
     */
    public void addExchange(String chatId, String userMessage, String aiReply) {
        if (chatId == null || chatId.isBlank()) return;
        if (userMessage == null || userMessage.isBlank()) return;
        if (aiReply == null || aiReply.isBlank()) return;

        // 截断过长的消息，避免上下文爆炸
        String truncatedUser = truncate(userMessage, 500);
        String truncatedAi = truncate(aiReply, 500);

        LinkedList<String> messages = store.computeIfAbsent(chatId, k -> new LinkedList<>());
        synchronized (messages) {
            messages.add("用户: " + truncatedUser);
            messages.add("助手: " + truncatedAi);

            // 保留最近 MAX_ROUNDS 轮（2 * MAX_ROUNDS 条消息）
            while (messages.size() > MAX_ROUNDS * 2) {
                messages.removeFirst();
            }
        }

        log.debug("Context recorded for chatId={}, total={} messages", chatId, messages.size());
    }

    /**
     * 获取格式化后的上下文文本，用于注入 LLM prompt
     */
    public String getContext(String chatId) {
        if (chatId == null || chatId.isBlank()) return "";

        LinkedList<String> messages = store.get(chatId);
        if (messages == null || messages.isEmpty()) return "";

        synchronized (messages) {
            return String.join("\n", messages);
        }
    }

    /**
     * 获取最近 N 轮对话
     */
    public List<String> getRecent(String chatId, int rounds) {
        if (chatId == null) return List.of();

        LinkedList<String> messages = store.get(chatId);
        if (messages == null || messages.isEmpty()) return List.of();

        int count = rounds * 2;
        synchronized (messages) {
            int fromIndex = Math.max(0, messages.size() - count);
            return new ArrayList<>(messages.subList(fromIndex, messages.size()));
        }
    }

    /**
     * 清空某用户的上下文
     */
    public void clear(String chatId) {
        if (chatId != null) {
            store.remove(chatId);
            log.info("Context cleared for chatId={}", chatId);
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}
