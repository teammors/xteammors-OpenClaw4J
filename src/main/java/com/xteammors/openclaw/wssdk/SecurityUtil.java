package com.xteammors.openclaw.wssdk;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

public class SecurityUtil {

    public static String getUidKey(String uid) {
        return SecureUtil.md5(uid);
    }

    public static String encrypt(String key, String decryptStr) {
        AES aes = SecureUtil.aes(key.getBytes());
        return aes.encryptBase64(decryptStr);
    }

    public static String decrypt(String key, String encryptStr) {
        AES aes = SecureUtil.aes(key.getBytes());
        return aes.decryptStr(encryptStr, StandardCharsets.UTF_8);
    }

}
