package com.xteammors.openclaw.wssdk;

import com.alibaba.fastjson.JSON;

public class XJSONUtils {

    // 最实用的方案：组合方法
    public static boolean isJsonFast(String str) {
        // 1. 快速预检查
        if (str == null || str.trim().isEmpty()) return false;

        String trimmed = str.trim();
        char first = trimmed.charAt(0);
        char last = trimmed.charAt(trimmed.length() - 1);

        // 快速检查起始和结束字符
        if (!((first == '{' && last == '}') ||
                (first == '[' && last == ']'))) {
            return false;
        }

        // 2. 使用高效库验证
        try {
            JSON.parse(trimmed);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
