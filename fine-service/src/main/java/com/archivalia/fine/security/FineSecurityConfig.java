package com.archivalia.fine.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class FineSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public FineSecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                // ==========================================
                // INTERNAL SERVICE-TO-SERVICE ENDPOINTS
                // ==========================================

                .requestMatchers("/fines/internal/**")
                .permitAll()

                // ==========================================
                // ADMIN-ONLY PAYMENT MANAGEMENT
                // ==========================================

                .requestMatchers("/fines/*/payment-approve")
                .hasRole("ADMIN")

                .requestMatchers("/fines/*/payment-reject")
                .hasRole("ADMIN")

                // ==========================================
                // ADMIN FINE MANAGEMENT
                // ==========================================

                .requestMatchers("/fines/admin/**")
                .hasRole("ADMIN")

                // ==========================================
                // ALL OTHER FINE OPERATIONS
                // ==========================================

                .requestMatchers("/fines/**")
                .authenticated()

                .anyRequest()
                .authenticated()
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}