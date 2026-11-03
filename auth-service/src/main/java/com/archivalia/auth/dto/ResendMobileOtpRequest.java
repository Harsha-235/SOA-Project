package com.archivalia.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendMobileOtpRequest {

    private String email;
}