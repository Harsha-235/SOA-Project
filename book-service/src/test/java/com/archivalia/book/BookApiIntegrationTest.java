package com.archivalia.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookApiIntegrationTest {

    @Test
    void booksEndpoint_withoutJwt_shouldReturnForbidden() throws Exception {

        URI uri = URI.create("http://localhost:8081/books");

        HttpURLConnection connection =
                (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        assertThat(statusCode).isEqualTo(403);

        connection.disconnect();
    }
}