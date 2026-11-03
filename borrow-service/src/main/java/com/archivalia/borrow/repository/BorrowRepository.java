package com.archivalia.borrow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.archivalia.borrow.entity.Borrow;

public interface BorrowRepository extends JpaRepository<Borrow, Long> {

    List<Borrow> findByUserId(Long userId);

    List<Borrow> findByBookId(Long bookId);

    Optional<Borrow> findByBorrowIdAndUserId(
            Long borrowId,
            Long userId);

    boolean existsByUserIdAndBookIdAndStatus(
            Long userId,
            Long bookId,
            String status);

    // Get all pending borrow requests for admin
    List<Borrow> findByStatus(String status);

    /*
     * Used by Book Service before archiving a book.
     * Only PENDING and BORROWED records block deletion.
     * RETURNED / REJECTED / FAILED history does not.
     */
    boolean existsByBookIdAndStatusIn(
            Long bookId,
            List<String> statuses);
}