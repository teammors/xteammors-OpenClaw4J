package com.xteammors.openclaw.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;

public class JsonUtils {

    private JsonUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Safely parse JSON, returns null if parsing fails
     */
    public static Object safeParse(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }

        try {
            return JSON.parse(jsonStr.trim());
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Check if string is valid JSON
     */
    public static boolean isValid(String jsonStr) {
        return safeParse(jsonStr) != null;
    }

    /**
     * Check if string is a JSON object
     */
    public static boolean isJsonObject(String jsonStr) {
        Object obj = safeParse(jsonStr);
        return obj instanceof com.alibaba.fastjson.JSONObject;
    }

    /**
     * Check if string is a JSON array
     */
    public static boolean isJsonArray(String jsonStr) {
        Object obj = safeParse(jsonStr);
        return obj instanceof com.alibaba.fastjson.JSONArray;
    }
}