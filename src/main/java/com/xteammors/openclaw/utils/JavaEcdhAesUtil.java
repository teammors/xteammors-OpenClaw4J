package com.xteammors.openclaw.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Base64;

public class JavaEcdhAesUtil {
    // ECDH 曲线选择（双方必须一致，推荐 secp256r1/P-256）
    private static final String EC_CURVE = "secp256r1";

    /**
     * 1. 生成 ECDH 密钥对（私钥+公钥）
     */
    public static KeyPair generateEcdhKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(EC_CURVE);
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 2. 将公钥转为字节（用于网络传输）
     */
    public static byte[] publicKeyToBytes(PublicKey publicKey) throws Exception {
        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        return ecPublicKey.getEncoded(); // X.509 格式
    }

    /**
     * 2.1 将公钥转为原始点编码（用于与 Flutter 兼容）
     */
    public static byte[] publicKeyToRawBytes(PublicKey publicKey) throws Exception {
        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        ECPoint point = ecPublicKey.getW();
        
        // 对于 secp256r1 曲线，坐标应该是 32 字节
        byte[] x = normalizeCoordinate(point.getAffineX().toByteArray());
        byte[] y = normalizeCoordinate(point.getAffineY().toByteArray());
        
        // 构建非压缩格式的点编码（0x04 + x + y）
        byte[] result = new byte[1 + 32 + 32];
        result[0] = 0x04; // 非压缩格式标记
        System.arraycopy(x, 0, result, 1, 32);
        System.arraycopy(y, 0, result, 33, 32);
        return result;
    }
    
    /**
     * 标准化坐标字节数组，确保长度为 32 字节
     */
    private static byte[] normalizeCoordinate(byte[] input) {
        byte[] result = new byte[32];
        if (input.length == 32) {
            // 正好 32 字节，直接复制
            System.arraycopy(input, 0, result, 0, 32);
        } else if (input.length > 32) {
            // 超过 32 字节，取后 32 字节
            System.arraycopy(input, input.length - 32, result, 0, 32);
        } else {
            // 不足 32 字节，前面补 0
            System.arraycopy(input, 0, result, 32 - input.length, input.length);
        }
        return result;
    }

    /**
     * 4.1 将公钥转为原始点编码的 Base64 字符串（用于与 Flutter 兼容）
     */
    public static String publicKeyToRawString(PublicKey publicKey) throws Exception {
        byte[] bytes = publicKeyToRawBytes(publicKey);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 3. 将字节转回公钥
     */
    public static PublicKey bytesToPublicKey(byte[] bytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(new X509EncodedKeySpec(bytes));
    }

    /**
     * 4. 将公钥转为 Base64 字符串（用于保存和传输）
     */
    public static String publicKeyToString(PublicKey publicKey) throws Exception {
        byte[] bytes = publicKeyToBytes(publicKey);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 5. 将 Base64 字符串转回公钥（支持 X.509 格式和原始点编码格式）
     */
    public static PublicKey stringToPublicKey(String publicKeyStr) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(publicKeyStr);
        
        // 尝试解析 X.509 格式
        try {
            return bytesToPublicKey(bytes);
        } catch (Exception e) {
            // 如果 X.509 解析失败，尝试解析原始点编码格式
            try {
                return rawBytesToPublicKey(bytes);
            } catch (Exception e2) {
                throw new Exception("Failed to parse public key in either X.509 or raw format", e2);
            }
        }
    }
    
    /**
     * 5.1 将原始点编码格式字节转回公钥
     */
    private static PublicKey rawBytesToPublicKey(byte[] rawBytes) throws Exception {
        // 原始点编码格式：0x04 + 32字节x + 32字节y
        if (rawBytes[0] != 0x04) {
            throw new Exception("Not an uncompressed point format");
        }
        
        // 提取 x 和 y 坐标
        byte[] x = new byte[32];
        byte[] y = new byte[32];
        System.arraycopy(rawBytes, 1, x, 0, 32);
        System.arraycopy(rawBytes, 33, y, 0, 32);
        
        // 创建 ECPublicKeySpec
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        
        // 生成参数规范
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(ecSpec);
        
        // 临时生成密钥对来获取参数
        KeyPair tempKeyPair = keyPairGenerator.generateKeyPair();
        ECPublicKey tempPublicKey = (ECPublicKey) tempKeyPair.getPublic();
        ECParameterSpec parameterSpec = tempPublicKey.getParams();
        
        // 创建公钥
        ECPoint point = new ECPoint(new java.math.BigInteger(1, x), new java.math.BigInteger(1, y));
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, parameterSpec);
        
        // 生成公钥
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(pubSpec);
    }

    /**
     * 6. 将私钥转为 Base64 字符串（用于保存和传输）
     */
    public static String privateKeyToString(PrivateKey privateKey) throws Exception {
        byte[] bytes = privateKey.getEncoded(); // PKCS#8 格式
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 7. 将 Base64 字符串转回私钥
     */
    public static PrivateKey stringToPrivateKey(String privateKeyStr) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(privateKeyStr);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    /**
     * 8. 计算共享密钥（私钥 + 对方公钥）
     */
    public static byte[] computeSharedSecret(PrivateKey privateKey, PublicKey peerPublicKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(peerPublicKey, true);
        return keyAgreement.generateSecret();
    }

    /**
     * 9. HMAC-SHA256 衍生 AES 密钥（将原始共享密钥转为 16 字节 AES-128 密钥）
     */
    public static byte[] hkdfDeriveKey(byte[] sharedSecret, byte[] salt) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        if (salt == null || salt.length == 0) {
            // 如果没有盐值，使用共享密钥作为 HMAC 密钥
            hmac.init(new SecretKeySpec(sharedSecret, "HmacSHA256"));
        } else {
            hmac.init(new SecretKeySpec(salt, "HmacSHA256"));
        }
        byte[] info = "ecdh-aes".getBytes("UTF-8");
        byte[] input = new byte[sharedSecret.length + info.length];
        System.arraycopy(sharedSecret, 0, input, 0, sharedSecret.length);
        System.arraycopy(info, 0, input, sharedSecret.length, info.length);
        byte[] output = hmac.doFinal(input);
        // 取前 16 字节作为 AES-128 密钥
        byte[] key = new byte[16];
        System.arraycopy(output, 0, key, 0, 16);
        return key;
    }

    /**
     * 10. AES-GCM 加密（返回：密文 + 随机数(iv) + 认证标签，Base64 编码的字符串拼接）
     */
    public static String aesGcmEncrypt(byte[] key, String plainText) throws Exception {
        byte[] plainBytes = plainText.getBytes("UTF-8");
        byte[] iv = new byte[12]; // GCM 推荐 iv 长度 12 字节
        new SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128 位认证标签
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), gcmSpec);
        byte[] cipherBytes = cipher.doFinal(plainBytes);
        
        // 分离密文和标签（GCM 模式下，标签附加在密文后面）
        int tagLength = 16; // 128 位 = 16 字节
        byte[] cipherText = new byte[cipherBytes.length - tagLength];
        byte[] tag = new byte[tagLength];
        System.arraycopy(cipherBytes, 0, cipherText, 0, cipherText.length);
        System.arraycopy(cipherBytes, cipherText.length, tag, 0, tag.length);
        
        // 将结果转换为 Base64 编码的字符串拼接（格式：iv:cipherText:tag）
        String ivBase64 = Base64.getEncoder().encodeToString(iv);
        String cipherTextBase64 = Base64.getEncoder().encodeToString(cipherText);
        String tagBase64 = Base64.getEncoder().encodeToString(tag);
        return ivBase64 + ":" + cipherTextBase64 + ":" + tagBase64;
    }

    /**
     * 11. AES-GCM 解密（接受 Base64 编码的字符串拼接）
     */
    public static String aesGcmDecrypt(byte[] key, String encryptedData) throws Exception {
        String[] parts = encryptedData.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid encrypted data format");
        }
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] cipherText = Base64.getDecoder().decode(parts[1]);
        byte[] tag = Base64.getDecoder().decode(parts[2]);
        
        // 拼接密文和标签
        byte[] fullCipher = new byte[cipherText.length + tag.length];
        System.arraycopy(cipherText, 0, fullCipher, 0, cipherText.length);
        System.arraycopy(tag, 0, fullCipher, cipherText.length, tag.length);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), gcmSpec);
        byte[] plainBytes = cipher.doFinal(fullCipher);
        return new String(plainBytes, "UTF-8");
    }

    // 辅助函数：字节数组转十六进制字符串
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // 测试示例
    public static void main(String[] args) throws Exception {
        // 步骤1：A端生成密钥对
        KeyPair aKeyPair = generateEcdhKeyPair();
        
        // 将密钥转换为字符串格式（用于保存和传输）
        String aPublicKeyStr = publicKeyToString(aKeyPair.getPublic());
        String aPublicKeyRawStr = publicKeyToRawString(aKeyPair.getPublic()); // 原始点编码格式
        String aPrivateKeyStr = privateKeyToString(aKeyPair.getPrivate());
        System.out.println("A端公钥（X.509 Base64）：" + aPublicKeyStr);
        System.out.println("A端公钥（原始点编码 Base64）：" + aPublicKeyRawStr);
        System.out.println("A端私钥（Base64）：" + aPrivateKeyStr);

        // 步骤2：B端生成密钥对
        KeyPair bKeyPair = generateEcdhKeyPair();
        String bPublicKeyStr = publicKeyToString(bKeyPair.getPublic());
        String bPublicKeyRawStr = publicKeyToRawString(bKeyPair.getPublic()); // 原始点编码格式
        String bPrivateKeyStr = privateKeyToString(bKeyPair.getPrivate());
        System.out.println("B端公钥（X.509 Base64）：" + bPublicKeyStr);
        System.out.println("B端公钥（原始点编码 Base64）：" + bPublicKeyRawStr);
        System.out.println("B端私钥（Base64）：" + bPrivateKeyStr);

        // 步骤3：A端用自己的私钥 + B端公钥计算共享密钥
        PublicKey bPublicKey = stringToPublicKey(bPublicKeyStr);
        PrivateKey aPrivateKey = stringToPrivateKey(aPrivateKeyStr);
        byte[] aSharedSecret = computeSharedSecret(aPrivateKey, bPublicKey);
        byte[] aAesKey = hkdfDeriveKey(aSharedSecret, null);

        // 步骤4：B端用自己的私钥 + A端公钥计算共享密钥（与A端一致）
        PublicKey aPublicKey = stringToPublicKey(aPublicKeyStr);
        PrivateKey bPrivateKey = stringToPrivateKey(bPrivateKeyStr);
        byte[] bSharedSecret = computeSharedSecret(bPrivateKey, aPublicKey);
        byte[] bAesKey = hkdfDeriveKey(bSharedSecret, null);

        // 验证共享密钥是否一致
        System.out.println("AES密钥是否一致：" + bytesToHex(aAesKey).equals(bytesToHex(bAesKey)));

        // 步骤5：A端加密数据
        String plainText = "Hello ECDH AES!";
        String encryptResult = aesGcmEncrypt(aAesKey, plainText);
        System.out.println("加密结果（Base64 拼接）：" + encryptResult);

        // 步骤6：B端解密数据
        String decryptText = aesGcmDecrypt(bAesKey, encryptResult);
        System.out.println("解密结果：" + decryptText);
    }
}
