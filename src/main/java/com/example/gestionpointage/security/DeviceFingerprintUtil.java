package com.example.gestionpointage.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public class DeviceFingerprintUtil {

    public static String generate(String userAgent) {

        String raw = normalize(userAgent);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hash error", e);
        }
    }

    private static String normalize(String value) {
        if (value == null) return "unknown";
        return value.trim().toLowerCase();
    }
}