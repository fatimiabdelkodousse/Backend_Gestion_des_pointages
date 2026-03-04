package com.example.gestionpointage.security;

import jakarta.servlet.http.HttpServletRequest;

public class RequestUtil {

    public static String getClientIp(HttpServletRequest request) {

        // Railway / Render / Reverse Proxy
        String xff = request.getHeader("X-Forwarded-For");

        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For: clientIP, proxy1, proxy2
            return xff.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}