package com.pfc.suaoficina.authdemo.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
@Component
public class AesEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12;        // bytes por recomendação da NIST SP 800-38D

    private static byte[] secretKeyBytes;

    @Value("${encryption.secret}")
    public void setSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("encryption.secret não pode ser nulo ou vazio.");
        }
        byte[] raw = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // Deriva chave de 256 bits com padding seguro ou truncamento
        secretKeyBytes = java.util.Arrays.copyOf(raw, 32);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(secretKeyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(
                    attribute.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Layout persistido IV (12 bytes) e ciphertext + tag GCM
            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Falha na cifragem do atributo.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(dbData));

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(secretKeyBytes, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Falha na decifragem do atributo.", e);
        }
    }
}