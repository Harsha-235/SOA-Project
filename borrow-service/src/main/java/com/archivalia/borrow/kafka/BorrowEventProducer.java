package com.archivalia.borrow.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class BorrowEventProducer {

    private static final String TOPIC = "archivalia-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public BorrowEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // ==========================================
    // BORROW APPROVED EVENT
    // ==========================================

    public void sendBorrowApprovedEvent(
            Long borrowId,
            Long userId,
            Long bookId) {

        String event =
                "{"
                + "\"eventType\":\"BORROW_APPROVED\","
                + "\"borrowId\":" + borrowId + ","
                + "\"userId\":" + userId + ","
                + "\"bookId\":" + bookId
                + "}";

        kafkaTemplate.send(TOPIC, event);

        System.out.println(
                "Kafka event sent: " + event);
    }


    // ==========================================
    // BOOK RETURNED EVENT
    // ==========================================

    public void sendBookReturnedEvent(
            Long borrowId,
            Long userId,
            Long bookId) {

        String event =
                "{"
                + "\"eventType\":\"BOOK_RETURNED\","
                + "\"borrowId\":" + borrowId + ","
                + "\"userId\":" + userId + ","
                + "\"bookId\":" + bookId
                + "}";

        kafkaTemplate.send(TOPIC, event);

        System.out.println(
                "Kafka event sent: " + event);
    }
}