package com.archivalia.borrow.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/auth/users/{userId}")
    Map<String, Object> getUserById(
            @PathVariable("userId") Long userId);
}