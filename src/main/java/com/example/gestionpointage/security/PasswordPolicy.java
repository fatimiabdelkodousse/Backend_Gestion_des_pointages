package com.example.gestionpointage.security;

public class PasswordPolicy {

    public static void validate(String password) {

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Weak password");
        }

        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit  = password.matches(".*\\d.*");

        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("Weak password");
        }
    }
}
