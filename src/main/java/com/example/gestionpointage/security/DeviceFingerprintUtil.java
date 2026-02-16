package com.example.gestionpointage.security;

import org.apache.commons.codec.digest.DigestUtils;

public class DeviceFingerprintUtil {

    public static String generate(String ip, String userAgent) {
        String raw = ip + "|" + userAgent;
        return DigestUtils.sha256Hex(raw);
    }
}
