package com.archivalia.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MobileOtpVerificationRequest {

    private String email;
    private String otp;
}