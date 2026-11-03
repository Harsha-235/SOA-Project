package com.archivalia.borrow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.archivalia.borrow.entity.Borrow;
import com.archivalia.borrow.repository.BorrowRepository;

class BorrowServiceTest {

    @Mock
    private BorrowRepository borrowRepository;

    @InjectMocks
    private BorrowService borrowService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getBorrowById_shouldReturnBorrow() {

        Borrow borrow = Borrow.builder()
                .borrowId(1L)
                .userId(24L)
                .bookId(1L)
                .borrowingDays(7)
                .issueDate(LocalDate.of(2026, 9, 10))
                .dueDate(LocalDate.of(2026, 9, 17))
                .status("BORROWED")
                .build();

        when(borrowRepository.findById(1L))
                .thenReturn(Optional.of(borrow));

        Borrow result = borrowService.getBorrowById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getBorrowId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(24L);
        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("BORROWED");

        verify(borrowRepository).findById(1L);
    }

    @Test
    void getBorrowById_shouldRejectMissingBorrow() {

        when(borrowRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowService.getBorrowById(999L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getBorrowById_shouldReturnCorrectBorrowDetails() {

        Borrow borrow = Borrow.builder()
                .borrowId(10L)
                .userId(24L)
                .bookId(1L)
                .borrowingDays(7)
                .status("BORROWED")
                .build();

        when(borrowRepository.findById(10L))
                .thenReturn(Optional.of(borrow));

        Borrow result = borrowService.getBorrowById(10L);

        assertThat(result.getBorrowId()).isEqualTo(10L);
        assertThat(result.getUserId()).isEqualTo(24L);
        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getBorrowingDays()).isEqualTo(7);
        assertThat(result.getStatus()).isEqualTo("BORROWED");

        verify(borrowRepository).findById(10L);
    }
}