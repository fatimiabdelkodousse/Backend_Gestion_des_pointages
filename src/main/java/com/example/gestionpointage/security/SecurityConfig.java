package com.example.gestionpointage.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // 🔓 صفحة إعادة تعيين كلمة المرور (HTML)
                .requestMatchers(HttpMethod.GET, "/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/reset-password/**").permitAll()

                // 🔓 Auth API endpoints
                .requestMatchers("/auth/**").permitAll()

                // 🔓 Pointages (ESP32)
                .requestMatchers("/pointages/**").permitAll()

                // 🔓 Error page
                .requestMatchers("/error").permitAll()

                // 🔓 WebSocket
                .requestMatchers("/ws/**").permitAll()

                // 📁 Uploads
                .requestMatchers("/uploads/**")
                .hasAnyRole("ADMIN", "USER")

                // 👤 Admin only
                .requestMatchers("/users/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/sites/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}