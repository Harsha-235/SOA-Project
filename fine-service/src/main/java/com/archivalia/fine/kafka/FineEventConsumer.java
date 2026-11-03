package com.archivalia.fine.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.archivalia.fine.service.FineService;

@Service
public class FineEventConsumer {

    private final FineService fineService;

    public FineEventConsumer(FineService fineService) {
        this.fineService = fineService;
    }

    @KafkaListener(
            topics = "archivalia-events",
            groupId = "fine-service-group"
    )
    public void consumeEvent(String event) {

        System.out.println(
                "Kafka event received by Fine Service: " + event
        );

        try {

            if (event.contains("\"eventType\":\"BORROW_APPROVED\"")) {

                System.out.println(
                        "Fine Service received BORROW_APPROVED event."
                );

                /*
                 * No fine is calculated here.
                 * Fine calculation starts only when the borrowing
                 * becomes overdue / return processing requires it.
                 */

                return;
            }

            if (event.contains("\"eventType\":\"BOOK_RETURNED\"")) {

                System.out.println(
                        "Fine Service received BOOK_RETURNED event."
                );

                Long borrowId = extractLong(event, "borrowId");

                System.out.println(
                        "Calculating fine for returned borrow: "
                                + borrowId
                );

                fineService.calculateFine(borrowId);

                System.out.println(
                        "Fine calculation completed for borrow: "
                                + borrowId
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Error processing Kafka event: "
                            + e.getMessage()
            );
        }
    }

    private Long extractLong(String event, String fieldName) {

        String search = "\"" + fieldName + "\":";

        int start = event.indexOf(search);

        if (start == -1) {
            throw new RuntimeException(
                    fieldName + " not found in Kafka event"
            );
        }

        start += search.length();

        int end = event.indexOf(",", start);

        if (end == -1) {
            end = event.indexOf("}", start);
        }

        return Long.valueOf(
                event.substring(start, end).trim()
        );
    }
}