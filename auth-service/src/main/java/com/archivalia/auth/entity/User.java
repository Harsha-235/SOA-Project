package com.archivalia.auth.entity;

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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;

    @Column(nullable = false)
    private Integer nameChangeCount;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String mobileNumber;

    private boolean emailVerified;

    private boolean mobileVerified;

    private String role;

    private String accountStatus;
    @Column(nullable = false)
    @Builder.Default
    private boolean deletionRequested = false;

    private LocalDateTime deletionRequestedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private String emailOtp;

    private LocalDateTime emailOtpExpiry;

    private String mobileOtp;

    private String passwordResetOtp;

    private LocalDateTime passwordResetOtpExpiry;

    private LocalDateTime mobileOtpExpiry;
    @Column(nullable = false)
    private boolean passwordResetOtpVerified;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        nameChangeCount = 0;
        passwordResetOtpVerified = false;
    }
}