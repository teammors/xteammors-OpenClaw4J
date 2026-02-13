package com.xteammors.openclaw.utils;

import com.alibaba.fastjson.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnicodeDecoder {

    /**
     * 将Unicode编码的字符串解码为中文
     */
    public static String decodeUnicode(String unicodeStr) {
        if (unicodeStr == null || unicodeStr.isEmpty()) {
            return unicodeStr;
        }

        Pattern pattern = Pattern.compile("(\\\\u[0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(unicodeStr);

        StringBuffer decodedStr = new StringBuffer();
        while (matcher.find()) {
            String unicode = matcher.group(1);
            char ch = (char) Integer.parseInt(unicode.substring(2), 16);
            matcher.appendReplacement(decodedStr, String.valueOf(ch));
        }
        matcher.appendTail(decodedStr);

        return decodedStr.toString();
    }

    /**
     * 解析Dify响应并解码answer字段
     */
    public static String parseAndDecodeAnswer(String jsonResponse) {
        try {
            JSONObject json = JSONObject.parseObject(jsonResponse);
            if (json != null && json.containsKey("answer")) {
                String encodedAnswer = json.getString("answer");
                return decodeUnicode(encodedAnswer);
            }
            return "无法解析响应中的answer字段";
        } catch (Exception e) {
            System.err.println("解析响应失败: " + e.getMessage());
            return "解析响应失败: " + e.getMessage();
        }
    }
}