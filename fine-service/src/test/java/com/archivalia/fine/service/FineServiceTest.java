package com.archivalia.fine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.archivalia.fine.client.AuthClient;
import com.archivalia.fine.client.BookClient;
import com.archivalia.fine.client.BorrowClient;
import com.archivalia.fine.entity.Fine;
import com.archivalia.fine.repository.FineRepository;

class FineServiceTest {

    @Mock
    private FineRepository fineRepository;

    @Mock
    private BorrowClient borrowClient;

    @Mock
    private BookClient bookClient;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private FineService fineService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getFineById_shouldReturnFine() {

        Fine fine = Fine.builder()
                .fineId(1L)
                .borrowId(10L)
                .userId(24L)
                .bookId(1L)
                .fineAmount(120.0)
                .lateDays(4)
                .finePerDay(30.0)
                .status("UNPAID")
                .paymentStatus("NOT_STARTED")
                .build();

        when(fineRepository.findById(1L))
                .thenReturn(Optional.of(fine));

        Fine result = fineService.getFineById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFineId()).isEqualTo(1L);
        assertThat(result.getBorrowId()).isEqualTo(10L);
        assertThat(result.getUserId()).isEqualTo(24L);
        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getFineAmount()).isEqualTo(120.0);
        assertThat(result.getStatus()).isEqualTo("UNPAID");

        verify(fineRepository).findById(1L);
    }

    @Test
    void getFineById_shouldRejectMissingFine() {

        when(fineRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fineService.getFineById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fine not found");
    }

    @Test
    void getFinesByUser_shouldReturnUserFines() {

        Fine fine = Fine.builder()
                .fineId(2L)
                .borrowId(15L)
                .userId(24L)
                .bookId(1L)
                .fineAmount(30.0)
                .lateDays(1)
                .finePerDay(30.0)
                .status("PAID")
                .paymentStatus("APPROVED")
                .build();

        when(fineRepository.findByUserId(24L))
                .thenReturn(List.of(fine));

        List<Fine> result = fineService.getFinesByUser(24L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFineId()).isEqualTo(2L);
        assertThat(result.get(0).getUserId()).isEqualTo(24L);
        assertThat(result.get(0).getFineAmount()).isEqualTo(30.0);
        assertThat(result.get(0).getStatus()).isEqualTo("PAID");

        verify(fineRepository).findByUserId(24L);
    }
}