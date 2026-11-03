package com.archivalia.borrow.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "borrows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long borrowId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long bookId;

    // Will be assigned by ADMIN when the request is approved
    private Integer borrowingDays;

    // Null while request is PENDING
    private LocalDate issueDate;

    // Null while request is PENDING
    private LocalDate dueDate;

    private LocalDate returnDate;

    /*
     * PENDING  = User requested the book
     * BORROWED = Admin approved and assigned borrowing period
     * REJECTED = Admin rejected the request
     * RETURNED = User returned the book
     * FAILED   = Borrow operation failed
     */
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}