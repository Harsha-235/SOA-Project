package com.archivalia.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.archivalia.auth.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
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
                // PUBLIC AUTH ENDPOINTS
                // ==========================================

                .requestMatchers(
                        "/auth/register",
                        "/auth/login",
                        "/auth/verify-email-otp",
                        "/auth/verify-mobile-otp",
                        "/auth/resend-email-otp",
                        "/auth/resend-mobile-otp",

                        // Forgot password / password reset
                        "/auth/forgot-password",
                        "/auth/verify-password-reset-otp",
                        "/auth/reset-password"
                )
                .permitAll()


                // ==========================================
                // INTERNAL USER STATUS
                // ==========================================
                // Used by Book/Borrow/Fine services
                // through Feign

                .requestMatchers(
                        "/auth/internal/users/{userId}/status"
                )
                .permitAll()


                // ==========================================
                // INTERNAL USER LOOKUP
                // ==========================================
                // Used by other microservices through Feign

                .requestMatchers(
                        "/auth/users/{userId}"
                )
                .permitAll()


                // ==========================================
                // ADMIN-ONLY ENDPOINTS
                // ==========================================

                .requestMatchers("/auth/admin/**")
                .hasRole("ADMIN")


                // ==========================================
                // USER PROFILE
                // ==========================================
                // Requires JWT authentication.
                // Ownership/admin permission is enforced
                // inside AuthService.

                .requestMatchers(
                        "/auth/users/*/profile"
                )
                .authenticated()


                // ==========================================
                // ALL OTHER ENDPOINTS
                // ==========================================

                .anyRequest()
                .authenticated()
            )

            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}