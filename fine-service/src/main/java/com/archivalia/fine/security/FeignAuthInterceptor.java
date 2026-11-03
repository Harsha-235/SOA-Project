package com.archivalia.fine.security;

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

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes)
                            RequestContextHolder.getRequestAttributes();

            if (attributes != null) {

                String authorization =
                        attributes.getRequest()
                                .getHeader("Authorization");

                if (authorization != null
                        && !authorization.isBlank()) {

                    requestTemplate.header(
                            "Authorization",
                            authorization
                    );
                }
            }
        };
    }
}	