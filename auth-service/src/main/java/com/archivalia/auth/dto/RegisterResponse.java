package com.archivalia.auth.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterResponse {

    private Long userId;
    private String name;
    private String email;
    private String mobileNumber;
    private boolean emailVerified;
    private boolean mobileVerified;
    private String role;
    private String accountStatus;
    private LocalDateTime createdAt;
}