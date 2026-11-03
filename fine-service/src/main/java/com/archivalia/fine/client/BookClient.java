package com.archivalia.fine.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service")
public interface BookClient {

    @GetMapping("/books/{bookId}")
    Map<String, Object> getBookById(
            @PathVariable("bookId") Long bookId
    );

    // ==========================================
    // GET FINE RATE HISTORY
    // ==========================================

    @GetMapping("/books/{bookId}/fine-rates")
    List<Map<String, Object>> getFineRateHistory(
            @PathVariable("bookId") Long bookId
    );
}