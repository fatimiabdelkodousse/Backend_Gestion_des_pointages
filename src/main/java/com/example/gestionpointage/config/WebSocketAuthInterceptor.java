package com.example.gestionpointage.config;

import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import com.example.gestionpointage.security.JwtService;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if (authHeaders == null || authHeaders.isEmpty()) {
                throw new IllegalArgumentException("Missing Authorization header");
            }

            String token = authHeaders.get(0).replace("Bearer ", "");

            String userId = jwtService.extractUserId(token);
            String role   = jwtService.extractRole(token);

            var auth = new org.springframework.security.authentication
                    .UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                    );

            accessor.setUser(auth);
        }

        return message;
    }
}
