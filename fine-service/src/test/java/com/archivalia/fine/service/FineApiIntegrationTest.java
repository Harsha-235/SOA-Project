package com.archivalia.fine.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class FineApiIntegrationTest {

    @MockitoBean
    private EmailService emailService;

    @Test
    void finesEndpoint_withoutJwt_shouldReturnForbidden() throws Exception {

        URI uri = URI.create("http://localhost:8083/fines/user/24");

        HttpURLConnection connection =
                (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        assertThat(statusCode).isEqualTo(403);

        connection.disconnect();
    }
}