package com.archivalia.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	    "textbee.api.key=test-key",
	    "textbee.device.id=test-device",
	    "textbee.api.url=http://localhost"
	})
class AuthApiIntegrationTest {

    @Test
    void protectedEndpoint_withoutJwt_shouldReturnForbidden() throws Exception {

        URI uri = URI.create("http://localhost:8084/auth/test");

        HttpURLConnection connection =
                (HttpURLConnection) uri.toURL().openConnection();

        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        assertThat(statusCode).isEqualTo(403);

        connection.disconnect();
    }
}