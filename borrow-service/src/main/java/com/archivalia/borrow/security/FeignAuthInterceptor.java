package com.archivalia.borrow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;

@Configuration
public class FeignAuthInterceptor {

    @Bean
    public RequestInterceptor requestInterceptor() {

        return requestTemplate -> {

            // ==========================================
            // DO NOT FORWARD USER JWT TO INTERNAL
            // AUTH STATUS ENDPOINT
            // ==========================================

            String requestPath = requestTemplate.path();

            if (requestPath != null &&
                    requestPath.startsWith("/auth/internal/")) {

                return;
            }

            // ==========================================
            // FOR OTHER FEIGN REQUESTS
            // FORWARD USER JWT
            // ==========================================

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes)
                            RequestContextHolder.getRequestAttributes();

            if (attributes != null) {

                String authorization =
                        attributes.getRequest()
                                .getHeader("Authorization");

                if (authorization != null &&
                        !authorization.isBlank()) {

                    requestTemplate.header(
                            "Authorization",
                            authorization
                    );
                }
            }
        };
    }
}