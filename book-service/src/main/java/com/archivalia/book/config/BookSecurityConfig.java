package com.archivalia.book.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.archivalia.book.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class BookSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public BookSecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .exceptionHandling(exception -> exception
                .accessDeniedHandler(accessDeniedHandler())
            )

            .authorizeHttpRequests(auth -> auth

                // =========================
                // ADMIN ONLY
                // =========================

                .requestMatchers(HttpMethod.POST, "/books")
                .hasRole("ADMIN")

                .requestMatchers(HttpMethod.PUT, "/books/*")
                .hasRole("ADMIN")

                .requestMatchers(HttpMethod.DELETE, "/books/*")
                .hasRole("ADMIN")


                // =========================
                // INTERNAL SERVICE ACCESS
                // =========================
                // Fine Service uses this endpoint
                // to retrieve historical fine rates
                // while processing Kafka events.

                .requestMatchers(
                    HttpMethod.GET,
                    "/books/*/fine-rates"
                )
                .permitAll()


                // =========================
                // USER + ADMIN
                // =========================

                .requestMatchers(HttpMethod.GET, "/books")
                .hasAnyRole("USER", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/books/search")
                .hasAnyRole("USER", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/books/*")
                .hasAnyRole("USER", "ADMIN")


                // =========================
                // BORROW / RETURN
                // =========================

                .requestMatchers(
                    "/books/*/borrow",
                    "/books/*/return"
                )
                .hasAnyRole("USER", "ADMIN")


                // =========================
                // EVERYTHING ELSE
                // =========================

                .anyRequest().authenticated()
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }


    @Bean
    public AccessDeniedHandler accessDeniedHandler() {

        return (request, response, accessDeniedException) -> {

            response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
            );

            response.setContentType("application/json");

            response.getWriter().write("""
                {
                    "status": 403,
                    "error": "Forbidden",
                    "message": "You are not authorized to access this resource"
                }
                """);
        };
    }
}