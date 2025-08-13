package com.gallery.sweeper.photo.cleaner.utis;

import android.util.Base64;

import com.daz.lib_base.utils.XLog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 项目名称：
 * 作者：wx
 * 时间：2025/7/27 14:26
 * 描述：
 */
// AESUtil.java - AES加密工具类
public class AESUtil {
    private static final String TAG = "AESUtil";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "AES";

    /**
     * 加密数据
     *
     * @param data 要加密的数据
     * @param key 加密密钥
     * @return 加密后的Base64编码字符串
     */
    public static String encrypt(String data, String key) {
        try {
            // 生成密钥
            SecretKeySpec secretKey = generateKey(key);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));

            // 执行加密
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            XLog.e(TAG, "加密失败："+e.getMessage());
            return data; // 加密失败返回原始数据
        }
    }

    /**
     * 解密数据
     *
     * @param encryptedData 加密后的Base64编码字符串
     * @param key 加密密钥
     * @return 解密后的原始数据
     */
    public static String decrypt(String encryptedData, String key) {
        try {
            // 生成密钥
            SecretKeySpec secretKey = generateKey(key);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));

            // 执行解密
            byte[] decodedBytes = Base64.decode(encryptedData, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            XLog.e(TAG, "解密失败："+e.getMessage());
            return encryptedData; // 解密失败返回加密数据
        }
    }

    /**
     * 生成AES密钥
     */
    private static SecretKeySpec generateKey(String key) throws NoSuchAlgorithmException {
        // 使用SHA-256哈希确保密钥长度符合AES要求
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] hashedBytes = sha.digest(keyBytes);

        // 截取前32字节作为AES-256密钥
        byte[] aesKeyBytes = Arrays.copyOf(hashedBytes, 32);
        return new SecretKeySpec(aesKeyBytes, ALGORITHM);
    }
}