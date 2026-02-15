package com.example.gestionpointage.security;

import java.security.SecureRandom;
import java.util.Base64;

public class SecureTokenGenerator {

    private static final SecureRandom random = new SecureRandom();

    public static String generate() {
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
