package com.archivalia.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.archivalia.auth.service.SmsService;

@SpringBootTest(
    properties = {
        "textbee.api.key=test-key",
        "textbee.device.id=test-device",
        "textbee.api.url=http://localhost"
    }
)
class AuthIntegrationTest {

    @MockitoBean
    private SmsService smsService;

    @Test
    void applicationContext_shouldLoadSuccessfully() {
        assertThat(true).isTrue();
    }
}