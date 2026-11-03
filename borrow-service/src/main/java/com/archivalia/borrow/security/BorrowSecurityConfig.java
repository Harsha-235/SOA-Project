package com.archivalia.borrow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class BorrowSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public BorrowSecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                // Internal service-to-service endpoint
            		.requestMatchers("/borrows/internal/**")
            		.permitAll()

                // Admin-only endpoints
                .requestMatchers("/borrows/admin/**")
                .hasRole("ADMIN")

                .requestMatchers("/borrows/*/approve")
                .hasRole("ADMIN")

                .requestMatchers("/borrows/*/reject")
                .hasRole("ADMIN")

                // All remaining borrow operations require login
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