package com.archivalia.auth.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SmsService {

    private final RestClient restClient;

    @Value("${textbee.api.url}")
    private String textbeeApiUrl;

    @Value("${textbee.api.key}")
    private String textbeeApiKey;

    @Value("${textbee.device.id}")
    private String textbeeDeviceId;

    public SmsService() {
        this.restClient = RestClient.create();
    }

    public void sendMobileOtp(String mobileNumber, String otp) {

        String recipient = mobileNumber;

        if (!recipient.startsWith("+")) {
            recipient = "+91" + recipient;
        }

        Map<String, Object> requestBody = Map.of(
                "recipients", List.of(recipient),
                "message", "Your Archivalia mobile verification OTP is: "
                        + otp
                        + ". This OTP is valid for 5 minutes.",
                "deviceId", textbeeDeviceId
        );

        restClient.post()
                .uri(textbeeApiUrl)
                .header("x-api-key", textbeeApiKey)
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }
}