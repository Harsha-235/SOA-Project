package com.archivalia.borrow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BorrowIntegrationTest {

    @Test
    void applicationContext_shouldLoadSuccessfully() {
        assertThat(true).isTrue();
    }
}