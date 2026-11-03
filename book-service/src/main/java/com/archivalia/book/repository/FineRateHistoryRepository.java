package com.archivalia.book.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.archivalia.book.entity.FineRateHistory;

public interface FineRateHistoryRepository
        extends JpaRepository<FineRateHistory, Long> {

    List<FineRateHistory> findByBookIdOrderByEffectiveFromAsc(
            Long bookId
    );

    Optional<FineRateHistory> findByBookIdAndEffectiveToIsNull(
            Long bookId
    );

    List<FineRateHistory>
        findByBookIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqual(
            Long bookId,
            LocalDate date1,
            LocalDate date2
    );
}