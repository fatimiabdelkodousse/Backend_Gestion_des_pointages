package com.example.gestionpointage.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashUtil {

    private static final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder(12); // cost = 12 (ممتاز)

    // Hash password
    public static String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // Verify password
    public static boolean verify(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}

