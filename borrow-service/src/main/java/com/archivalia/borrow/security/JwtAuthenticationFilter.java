package com.archivalia.borrow.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.archivalia.borrow.client.AuthClient;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthClient authClient;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AuthClient authClient) {

        this.jwtService = jwtService;
        this.authClient = authClient;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // ==========================================
        // 1. CHECK AUTHORIZATION HEADER
        // ==========================================

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            // ==========================================
            // 2. EXTRACT JWT DETAILS
            // ==========================================

            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            Long userId = jwtService.extractUserId(token);

            System.out.println("JWT email: " + email);
            System.out.println("JWT userId: " + userId);
            System.out.println("JWT role: " + role);

            if (email == null || userId == null) {

                System.out.println(
                        "JWT/Auth verification failed: User ID or email missing"
                );

                sendForbiddenResponse(
                        response,
                        "Invalid authentication token."
                );

                return;
            }

            // ==========================================
            // 3. CALL AUTH SERVICE THROUGH FEIGN
            //    FEIGN → EUREKA → AUTH-SERVICE
            // ==========================================

            Map<String, Object> user =
                    authClient.getUserById(userId);

            System.out.println(
                    "Auth Service response: " + user
            );

            if (user == null) {

                System.out.println(
                        "JWT/Auth verification failed: User not found"
                );

                sendForbiddenResponse(
                        response,
                        "User account not found."
                );

                return;
            }

            // ==========================================
            // 4. CHECK CURRENT ACCOUNT STATUS
            // ==========================================

            String accountStatus =
                    (String) user.get("accountStatus");

            System.out.println(
                    "Current account status: " + accountStatus
            );

            if (!"ACTIVE".equals(accountStatus)) {

                System.out.println(
                        "User is suspended. Blocking request."
                );

                SecurityContextHolder.clearContext();

                sendForbiddenResponse(
                        response,
                        "Your account has been suspended. Please contact the administrator."
                );

                return;
            }

            // ==========================================
            // 5. AUTHENTICATE USER
            // ==========================================

            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

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

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

            // ==========================================
            // 6. CONTINUE REQUEST
            // ==========================================

            filterChain.doFilter(request, response);

        } catch (Exception e) {

            // ==========================================
            // 7. AUTHENTICATION / FEIGN ERROR
            // ==========================================

            SecurityContextHolder.clearContext();

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "JWT/Auth verification failed:"
            );

            System.out.println(
                    "Exception Type: "
                            + e.getClass().getName()
            );

            System.out.println(
                    "Exception Message: "
                            + e.getMessage()
            );

            System.out.println(
                    "=========================================="
            );

            sendForbiddenResponse(
                    response,
                    "Your authentication is invalid or your account could not be verified."
            );
        }
    }

    // ==========================================
    // COMMON 403 RESPONSE
    // ==========================================

    private void sendForbiddenResponse(
            HttpServletResponse response,
            String message)
            throws IOException {

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );

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