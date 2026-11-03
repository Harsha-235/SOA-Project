package com.archivalia.fine.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "borrow-service")
public interface BorrowClient {

    @GetMapping("/borrows/internal/{borrowId}")
    Map<String, Object> getBorrowById(
            @PathVariable("borrowId") Long borrowId
    );
}