package com.archivalia.borrow.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "fine-service")
public interface FineClient {

    // ==========================================
    // GET EXISTING FINE
    // ==========================================

    @GetMapping("/fines/borrow/{borrowId}")
    Map<String, Object> getFineByBorrowId(
            @PathVariable("borrowId") Long borrowId
    );

    // ==========================================
    // CALCULATE / CREATE FINE
    // ==========================================

    @PostMapping("/fines/calculate/{borrowId}")
    Map<String, Object> calculateFine(
            @PathVariable("borrowId") Long borrowId
    );
}