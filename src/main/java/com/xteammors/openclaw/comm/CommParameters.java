package com.xteammors.openclaw.comm;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Data
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

    ConcurrentHashMap<String, List<JSONObject>> chatHistory = new ConcurrentHashMap<>();


    boolean isStarted = false;
    boolean redisStarted = false;

    String teammorsBotToken;

    String telegramBotId;
    String telegramBotToken;
    String telegramBotName;



}
