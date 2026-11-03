package com.archivalia.borrow.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "book-service")
public interface BookClient {

    @GetMapping("/books/{bookId}")
    Map<String, Object> getBookById(
            @PathVariable("bookId") Long bookId
    );

    @PutMapping("/books/{bookId}/borrow")
    Map<String, Object> borrowBook(
            @PathVariable("bookId") Long bookId
    );

    @PutMapping("/books/{bookId}/return")
    Map<String, Object> returnBook(
            @PathVariable("bookId") Long bookId
    );
}