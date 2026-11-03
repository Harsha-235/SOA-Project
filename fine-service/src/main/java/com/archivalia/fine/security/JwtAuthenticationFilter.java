package com.archivalia.fine.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No JWT present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            // Extract user information from JWT
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            // Validate extracted information
            if (email == null || role == null) {
                sendForbiddenResponse(
                        response,
                        "Invalid authentication token."
                );
                return;
            }

            // Create Spring Security authentication
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            token,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role
                                    )
                            )
                    );

            // Store authentication in SecurityContext
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            // Continue request
            filterChain.doFilter(request, response);

        } catch (Exception e) {

            // Clear authentication
            SecurityContextHolder.clearContext();

            // Print actual error in Fine Service console
            System.out.println("========== FINE JWT ERROR ==========");
            System.out.println(
                    "Exception Type: " + e.getClass().getName()
            );
            System.out.println(
                    "Exception Message: " + e.getMessage()
            );
            e.printStackTrace();
            System.out.println("====================================");

            // Return 403 response
            sendForbiddenResponse(
                    response,
                    "Your authentication is invalid or expired."
            );
        }
    }

    private void sendForbiddenResponse(
            HttpServletResponse response,
            String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        response.getWriter().write("""
            {
                "status": 403,
                "error": "Forbidden",
                "message": "%s"
            }
            """.formatted(message));
    }
}