package com.archivalia.fine.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class FineIntegrationTest {

    @MockitoBean
    private EmailService emailService;

    @Test
    void applicationContext_shouldLoadSuccessfully() {
        assertThat(true).isTrue();
    }
}