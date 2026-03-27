package com.xteammors.openclaw.comm;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CommParameters {

    private static CommParameters commParameters;

    private CommParameters() {
    }

    public static CommParameters instance() {
        if (null == commParameters) {
            commParameters = new CommParameters();
        }
        return commParameters;
    }

    private ConcurrentHashMap<String, List<JSONObject>> chatHistory = new ConcurrentHashMap<>();

    private boolean isStarted = false;
    private boolean redisStarted = false;

    private String teammorsBotToken;

    private String telegramBotId;
    private String telegramBotToken;
    private String telegramBotName;

    // Getters and Setters
    public ConcurrentHashMap<String, List<JSONObject>> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(ConcurrentHashMap<String, List<JSONObject>> chatHistory) {
        this.chatHistory = chatHistory;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public boolean isRedisStarted() {
        return redisStarted;
    }

    public void setRedisStarted(boolean redisStarted) {
        this.redisStarted = redisStarted;
    }

    public String getTeammorsBotToken() {
        return teammorsBotToken;
    }

    public void setTeammorsBotToken(String teammorsBotToken) {
        this.teammorsBotToken = teammorsBotToken;
    }

    public String getTelegramBotId() {
        return telegramBotId;
    }

    public void setTelegramBotId(String telegramBotId) {
        this.telegramBotId = telegramBotId;
    }

    public String getTelegramBotToken() {
        return telegramBotToken;
    }

    public void setTelegramBotToken(String telegramBotToken) {
        this.telegramBotToken = telegramBotToken;
    }

    public String getTelegramBotName() {
        return telegramBotName;
    }

    public void setTelegramBotName(String telegramBotName) {
        this.telegramBotName = telegramBotName;
    }

    @Override
    public String toString() {
        return "CommParameters{" +
                "chatHistory=" + chatHistory +
                ", isStarted=" + isStarted +
                ", redisStarted=" + redisStarted +
                ", teammorsBotToken='" + teammorsBotToken + '\'' +
                ", telegramBotId='" + telegramBotId + '\'' +
                ", telegramBotToken='" + telegramBotToken + '\'' +
                ", telegramBotName='" + telegramBotName + '\'' +
                '}';
    }
}
