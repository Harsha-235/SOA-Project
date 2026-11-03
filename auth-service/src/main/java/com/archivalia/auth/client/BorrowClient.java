package com.archivalia.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "borrow-service")
public interface BorrowClient {

    @GetMapping("/borrows/internal/users/{userId}/deletion-check")
    Boolean canDeleteUser(
            @PathVariable("userId") Long userId
    );

    @DeleteMapping("/borrows/internal/users/{userId}")
    String deleteUserBorrowHistory(
            @PathVariable("userId") Long userId
    );
}