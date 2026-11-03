package com.archivalia.fine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.archivalia.fine.entity.Fine;

public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByUserId(Long userId);

    List<Fine> findByBorrowId(Long borrowId);

    Optional<Fine> findByBorrowIdAndUserId(
            Long borrowId,
            Long userId
    );

    List<Fine> findByStatus(String status);

    boolean existsByUserIdAndStatusIn(
            Long userId,
            List<String> statuses
    );
}