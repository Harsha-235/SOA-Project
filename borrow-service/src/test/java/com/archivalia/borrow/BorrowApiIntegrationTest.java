package com.archivalia.borrow;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BorrowApiIntegrationTest {

    @Test
    void borrowsEndpoint_withoutJwt_shouldReturnForbidden() throws Exception {

        URI uri = URI.create("http://localhost:8082/borrows/user/24");

        HttpURLConnection connection =
                (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        assertThat(statusCode).isEqualTo(403);

        connection.disconnect();
    }
}