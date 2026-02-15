package com.example.gestionpointage.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginProtectionService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 10 * 60; // 10 min

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public void check(String key) {

        long now = Instant.now().getEpochSecond();
        Attempt a = attempts.get(key);

        if (a == null || now - a.firstTime > WINDOW_SECONDS) {
            attempts.put(key, new Attempt(1, now));
            return;
        }

        // ⏱️ progressive delay
        if (a.count >= 3) {
            try {
                Thread.sleep(a.count * 1000L);
            } catch (InterruptedException ignored) {}
        }

        if (a.count >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many login attempts. Try again later."
            );
        }

        a.count++;
    }

    public void success(String key) {
        attempts.remove(key);
    }

    private static class Attempt {
        int count;
        long firstTime;

        Attempt(int count, long firstTime) {
            this.count = count;
            this.firstTime = firstTime;
        }
    }
}
