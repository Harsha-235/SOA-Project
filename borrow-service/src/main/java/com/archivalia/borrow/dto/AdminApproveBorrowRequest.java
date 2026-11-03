package com.archivalia.borrow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminApproveBorrowRequest {

    @NotNull(message = "Borrowing period is required")
    @Min(value = 1, message = "Borrowing period must be at least 1 day")
    @Max(value = 60, message = "Borrowing period cannot exceed 60 days")
    private Integer borrowingDays;
}