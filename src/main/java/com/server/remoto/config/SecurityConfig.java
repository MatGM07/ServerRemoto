package com.server.remoto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authz) -> authz
                        .requestMatchers("/ws/**").permitAll() // permite WebSocket sin login
                        .anyRequest().authenticated()          // el resto sí requiere login
                )
                .csrf(csrf -> csrf.disable());// desactivá CSRF para permitir WebSocket

        return http.build();
    }
}
