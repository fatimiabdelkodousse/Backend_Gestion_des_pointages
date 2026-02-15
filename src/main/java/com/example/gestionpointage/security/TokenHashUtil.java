package com.example.gestionpointage.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class TokenHashUtil {

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Token hash error", e);
        }
    }
}
