package com.archivalia.fine.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.archivalia.fine.client.AuthClient;
import com.archivalia.fine.client.BookClient;
import com.archivalia.fine.client.BorrowClient;
import com.archivalia.fine.entity.Fine;
import com.archivalia.fine.repository.FineRepository;

@Service
public class FineService {

    private final FineRepository fineRepository;
    private final BorrowClient borrowClient;
    private final BookClient bookClient;
    private final EmailService emailService;
    private final AuthClient authClient;

    public FineService(
            FineRepository fineRepository,
            BorrowClient borrowClient,
            BookClient bookClient,
            EmailService emailService,
            AuthClient authClient) {

        this.fineRepository = fineRepository;
        this.borrowClient = borrowClient;
        this.bookClient = bookClient;
        this.emailService = emailService;
        this.authClient = authClient;
    }

    // ==========================================
    // CALCULATE FINE
    // ==========================================

    public Fine calculateFine(Long borrowId) {

        Map<String, Object> borrow =
                borrowClient.getBorrowById(borrowId);

        if (borrow == null) {
            throw new RuntimeException("Borrow record not found");
        }

        Long userId = getLongValue(borrow.get("userId"));
        Long bookId = getLongValue(borrow.get("bookId"));

        LocalDate dueDate =
                LocalDate.parse(borrow.get("dueDate").toString());

        LocalDate returnDate = null;

        if (borrow.get("returnDate") != null) {
            returnDate =
                    LocalDate.parse(borrow.get("returnDate").toString());
        }

        LocalDate calculationDate =
                returnDate != null
                        ? returnDate
                        : LocalDate.now();

        /*
         * Due date itself is not a late day.
         * Fine starts from the day after the due date.
         */
        long daysLate =
                ChronoUnit.DAYS.between(dueDate, calculationDate);

        int lateDays =
                (int) Math.max(daysLate, 0);

        if (lateDays == 0) {

            return fineRepository
                    .findByBorrowId(borrowId)
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        /*
         * Get fine-rate history from Book Service.
         */
        List<Map<String, Object>> rateHistory =
                bookClient.getFineRateHistory(bookId);

        if (rateHistory == null || rateHistory.isEmpty()) {
            throw new RuntimeException(
                    "Fine rate history not found for this book"
            );
        }

        /*
         * Calculate the fine separately for every overdue date.
         */
        double totalFine = 0.0;

        LocalDate overdueDate =
                dueDate.plusDays(1);

        while (!overdueDate.isAfter(calculationDate)) {

            Double applicableRate =
                    findApplicableRate(
                            rateHistory,
                            overdueDate
                    );

            if (applicableRate == null) {
                throw new RuntimeException(
                        "No fine rate found for "
                                + overdueDate
                );
            }

            totalFine += applicableRate;

            overdueDate = overdueDate.plusDays(1);
        }

        /*
         * Find existing fine if one already exists.
         */
        Fine fine =
                fineRepository
                        .findByBorrowId(borrowId)
                        .stream()
                        .findFirst()
                        .orElse(null);

        if (fine == null) {

            /*
             * Store the first applicable rate in finePerDay
             * for backward compatibility.
             * The actual fineAmount is calculated using
             * the complete rate history.
             */
            Double firstRate =
                    findApplicableRate(
                            rateHistory,
                            dueDate.plusDays(1)
                    );

            fine = Fine.builder()
                    .borrowId(borrowId)
                    .userId(userId)
                    .bookId(bookId)
                    .fineAmount(totalFine)
                    .lateDays(lateDays)
                    .finePerDay(firstRate)
                    .status("UNPAID")
                    .createdAt(LocalDateTime.now())
                    .build();

        } else {

            /*
             * Never recalculate an already paid or waived fine.
             */
            if ("PAID".equals(fine.getStatus())
                    || "WAIVED".equals(fine.getStatus())) {

                return fine;
            }

            /*
             * Update unpaid fine using the current
             * per-day rate history.
             */
            fine.setFineAmount(totalFine);
            fine.setLateDays(lateDays);
        }

        return fineRepository.save(fine);
    }


    // ==========================================
    // FIND APPLICABLE FINE RATE
    // ==========================================

    /*
     * Finds the rate that was effective on a particular date.
     *
     * Rule:
     * effectiveFrom <= date
     * AND
     * effectiveTo >= date
     *
     * If effectiveTo is null, the rate is currently active.
     */
    private Double findApplicableRate(
            List<Map<String, Object>> rateHistory,
            LocalDate date) {

        for (Map<String, Object> rate : rateHistory) {

            LocalDate effectiveFrom =
                    LocalDate.parse(
                            rate.get("effectiveFrom").toString()
                    );

            LocalDate effectiveTo = null;

            if (rate.get("effectiveTo") != null) {
                effectiveTo =
                        LocalDate.parse(
                                rate.get("effectiveTo").toString()
                        );
            }

            boolean startsBeforeOrOnDate =
                    !effectiveFrom.isAfter(date);

            boolean endsAfterOrOnDate =
                    effectiveTo == null
                            || !effectiveTo.isBefore(date);

            if (startsBeforeOrOnDate
                    && endsAfterOrOnDate) {

                return getDoubleValue(
                        rate.get("finePerDay")
                );
            }
        }

        return null;
    }


    // ==========================================
    // CALCULATE FINE RESPONSE
    // ==========================================

    public ResponseEntity<?> calculateFineResponse(
            Long borrowId) {

        Map<String, Object> borrow =
                borrowClient.getBorrowById(borrowId);

        if (borrow == null) {

            return ResponseEntity.status(404)
                    .body(Map.of(
                            "message",
                            "No current borrowing found."
                    ));
        }

        String status =
                String.valueOf(borrow.get("status"));

        if (!"BORROWED".equals(status)) {

            Fine existingFine =
                    fineRepository
                            .findByBorrowId(borrowId)
                            .stream()
                            .findFirst()
                            .orElse(null);

            if (existingFine == null) {

                return ResponseEntity.status(404)
                        .body(Map.of(
                                "message",
                                "No current borrowing found."
                        ));
            }

            return ResponseEntity.ok(existingFine);
        }

        Fine fine =
                calculateFine(borrowId);

        if (fine == null) {

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "No fine applicable. Borrowing is still within the due date."
                    )
            );
        }

        return ResponseEntity.ok(fine);
    }


    // ==========================================
    // USER - SUBMIT PAYMENT
    // ==========================================

    public void submitPayment(
            Long fineId,
            Long loggedInUserId) {

        Fine fine =
                fineRepository.findById(fineId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Fine not found"
                                ));

        // User can submit payment only for their own fine
        if (!fine.getUserId().equals(loggedInUserId)) {

            throw new RuntimeException(
                    "You are not authorized to submit payment for this fine"
            );
        }

        if ("PAID".equals(fine.getStatus())) {

            throw new RuntimeException(
                    "Fine has already been paid"
            );
        }

        if ("APPROVED".equals(fine.getPaymentStatus())) {

            throw new RuntimeException(
                    "Payment has already been approved"
            );
        }

        if ("PENDING_APPROVAL".equals(
                fine.getPaymentStatus())) {

            throw new RuntimeException(
                    "Payment is already waiting for admin approval"
            );
        }

        if ("WAIVED".equals(fine.getStatus())) {

            throw new RuntimeException(
                    "This fine has been waived"
            );
        }

        fine.setPaymentStatus("PENDING_APPROVAL");
        fine.setPaymentSubmittedAt(
                LocalDateTime.now()
        );

        fineRepository.save(fine);

        // ==========================================
        // SEND EMAIL TO ADMIN
        // ==========================================

        try {

            emailService.sendPaymentSubmittedEmail(
                    fine.getFineId(),
                    fine.getBorrowId(),
                    fine.getUserId(),
                    fine.getFineAmount()
            );

        } catch (Exception emailException) {

            /*
             * Payment submission should not fail
             * just because email delivery failed.
             */
            System.out.println(
                    "Payment submitted email failed: "
                            + emailException.getMessage()
            );
        }
    }


    // ==========================================
    // ADMIN - APPROVE PAYMENT
    // ==========================================

    public void approvePayment(Long fineId) {

        Fine fine =
                fineRepository
                        .findById(fineId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Fine not found"
                                ));

        if ("PAID".equals(fine.getStatus())) {

            throw new RuntimeException(
                    "Fine has already been paid"
            );
        }

        if ("WAIVED".equals(fine.getStatus())) {

            throw new RuntimeException(
                    "This fine has been waived"
            );
        }

        if (!"PENDING_APPROVAL".equals(
                fine.getPaymentStatus())) {

            throw new RuntimeException(
                    "Payment is not pending approval"
            );
        }

        // ==========================================
        // APPROVE PAYMENT
        // ==========================================

        fine.setPaymentStatus("APPROVED");

        // Mark fine as paid
        fine.setStatus("PAID");

        // Record payment time
        fine.setPaidAt(LocalDateTime.now());

        fineRepository.save(fine);

        // ==========================================
        // SEND APPROVAL EMAIL TO USER
        // ==========================================

        try {

            Map<String, Object> user =
                    authClient.getUserById(
                            fine.getUserId()
                    );

            String userEmail =
                    String.valueOf(
                            user.get("email")
                    );

            String userName =
                    String.valueOf(
                            user.get("name")
                    );

            emailService.sendPaymentApprovedEmail(
                    userEmail,
                    userName,
                    fine.getFineId(),
                    fine.getFineAmount()
            );

        } catch (Exception emailException) {

            /*
             * Payment approval should not fail
             * just because email delivery failed.
             */
            System.out.println(
                    "Payment approval email failed: "
                            + emailException.getMessage()
            );
        }
    }


    // ==========================================
    // ADMIN - REJECT PAYMENT
    // ==========================================

    public void rejectPayment(Long fineId) {

        Fine fine =
                fineRepository
                        .findById(fineId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Fine not found"
                                ));

        if ("PAID".equals(fine.getStatus())) {

            throw new RuntimeException(
                    "Fine has already been paid"
            );
        }

        if ("WAIVED".equals(fine.getStatus())) {

            throw new RuntimeException(
                    "This fine has been waived"
            );
        }

        if (!"PENDING_APPROVAL".equals(
                fine.getPaymentStatus())) {

            throw new RuntimeException(
                    "Payment is not pending approval"
            );
        }

        // ==========================================
        // REJECT PAYMENT
        // ==========================================

        fine.setPaymentStatus("REJECTED");

        fineRepository.save(fine);

        // ==========================================
        // SEND REJECTION EMAIL TO USER
        // ==========================================

        try {

            Map<String, Object> user =
                    authClient.getUserById(
                            fine.getUserId()
                    );

            String userEmail =
                    String.valueOf(
                            user.get("email")
                    );

            String userName =
                    String.valueOf(
                            user.get("name")
                    );

            emailService.sendPaymentRejectedEmail(
                    userEmail,
                    userName,
                    fine.getFineId(),
                    fine.getFineAmount()
            );

        } catch (Exception emailException) {

            /*
             * Payment rejection should not fail
             * just because email delivery failed.
             */
            System.out.println(
                    "Payment rejection email failed: "
                            + emailException.getMessage()
            );
        }
    }
 // ==========================================
 // CHECK USER FINE HISTORY FOR DELETION
 // ==========================================

 public boolean canDeleteUser(Long userId) {

     boolean hasOutstandingFine =
             fineRepository.existsByUserIdAndStatusIn(
                     userId,
                     List.of("UNPAID")
             );

     boolean hasPendingPayment =
             fineRepository.existsByUserIdAndStatusIn(
                     userId,
                     List.of("PENDING_APPROVAL")
             );

     return !hasOutstandingFine && !hasPendingPayment;
 }



    // ==========================================
    // INTERNAL - DELETE USER FINE HISTORY
    // ==========================================
    // Permanent account deletion is blocked if the user
    // still has an unpaid or pending-payment fine.
    // Otherwise all fine/payment history is permanently removed.

    public void deleteUserFineHistory(Long userId) {

        boolean hasOutstandingFine =
                fineRepository.existsByUserIdAndStatusIn(
                        userId,
                        List.of("UNPAID")
                );

        boolean hasPendingPayment =
                fineRepository.existsByUserIdAndStatusIn(
                        userId,
                        List.of("PENDING_APPROVAL")
                );

        if (hasOutstandingFine || hasPendingPayment) {
            throw new RuntimeException(
                    "User cannot be permanently deleted while unpaid "
                            + "or pending-payment fines exist"
            );
        }

        fineRepository.deleteAll(
                fineRepository.findByUserId(userId)
        );
    }


    // ==========================================
    // GET FINE BY BORROW ID
    // ==========================================

    public Fine getFineByBorrowId(Long borrowId) {

        return fineRepository
                .findByBorrowId(borrowId)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "No fine found for this borrow"
                        ));
    }


    // ==========================================
    // GET FINE BY ID
    // ==========================================

    public Fine getFineById(Long fineId) {

        return fineRepository
                .findById(fineId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Fine not found"
                        )
                );
    }


    // ==========================================
    // GET FINES BY USER
    // ==========================================

    public List<Fine> getFinesByUser(Long userId) {

        return fineRepository.findByUserId(userId);
    }


    // ==========================================
    // LONG VALUE CONVERTER
    // ==========================================

    private Long getLongValue(Object value) {

        if (value == null) {

            throw new RuntimeException(
                    "Required numeric value is missing"
            );
        }

        if (value instanceof Number) {

            return ((Number) value).longValue();
        }

        return Long.valueOf(value.toString());
    }


    // ==========================================
    // DOUBLE VALUE CONVERTER
    // ==========================================

    private Double getDoubleValue(Object value) {

        if (value == null) {

            throw new RuntimeException(
                    "Fine rate is missing"
            );
        }

        if (value instanceof Number) {

            return ((Number) value).doubleValue();
        }

        return Double.valueOf(value.toString());
    }
}