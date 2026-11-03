package com.archivalia.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "fine-service")
public interface FineClient {

    @GetMapping("/fines/internal/users/{userId}/deletion-check")
    Boolean canDeleteUser(
            @PathVariable("userId") Long userId
    );

    @DeleteMapping("/fines/internal/users/{userId}")
    String deleteUserFineHistory(
            @PathVariable("userId") Long userId
    );
}