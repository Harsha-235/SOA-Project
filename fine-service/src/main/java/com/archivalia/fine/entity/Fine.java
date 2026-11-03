package com.archivalia.fine.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fineId;

    @Column(nullable = false)
    private Long borrowId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private Double fineAmount;

    @Column(nullable = false)
    private Integer lateDays;

    @Column(nullable = false)
    private Double finePerDay;

    @Column(nullable = false)
    private String status;

    // ==========================================
    // PAYMENT DETAILS
    // ==========================================

    @Column(nullable = false)
    private String paymentStatus;

    private LocalDateTime paymentSubmittedAt;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();

        if (status == null) {
            status = "UNPAID";
        }

        if (paymentStatus == null) {
            paymentStatus = "NOT_STARTED";
        }
    }
}