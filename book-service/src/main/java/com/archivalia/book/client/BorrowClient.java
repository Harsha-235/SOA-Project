package com.archivalia.book.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/*
 * Book Service -> Borrow Service
 *
 * Used only for deletion protection. The Book Service does not
 * access borrow_db directly because each microservice owns its DB.
 */
@FeignClient(name = "borrow-service")
public interface BorrowClient {

    @GetMapping("/borrows/internal/book/{bookId}/active")
    Boolean hasActiveBorrowing(
            @PathVariable("bookId") Long bookId);
}
