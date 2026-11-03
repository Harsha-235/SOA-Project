package com.archivalia.borrow.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBorrowResponse {

    // Borrow request details
    private Long borrowId;
    private String status;
    private LocalDateTime requestedAt;

    // User details
    private Long userId;
    private String userName;
    private String userEmail;

    // Book details
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookCategory;

    // Borrow details
    private Integer borrowingDays;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
}