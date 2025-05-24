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
                        .requestMatchers("/ws/**", "/upload").permitAll() // ahora permite también /upload
                        .anyRequest().authenticated()                     // el resto sigue requiriendo login
                )
                .csrf(csrf -> csrf.disable()); // CSRF desactivado para permitir POST sin token

        return http.build();
    }
}
