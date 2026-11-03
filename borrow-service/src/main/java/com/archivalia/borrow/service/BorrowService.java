package com.archivalia.borrow.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.archivalia.borrow.dto.AdminBorrowResponse;
import com.archivalia.borrow.client.AuthClient;
import com.archivalia.borrow.client.BookClient;
import com.archivalia.borrow.client.FineClient;
import com.archivalia.borrow.entity.Borrow;
import com.archivalia.borrow.repository.BorrowRepository;
import com.archivalia.borrow.kafka.BorrowEventProducer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BorrowService {

    private final BorrowRepository borrowRepository;
    private final BookClient bookClient;
    private final AuthClient authClient;
    private final FineClient fineClient;
    private final EmailService emailService;
    private final BorrowEventProducer borrowEventProducer;

    @Value("${spring.mail.username}")
    private String adminEmail;

    public BorrowService(
            BorrowRepository borrowRepository,
            BookClient bookClient,
            AuthClient authClient,
            FineClient fineClient,
            EmailService emailService,
            BorrowEventProducer borrowEventProducer) {

        this.borrowRepository = borrowRepository;
        this.bookClient = bookClient;
        this.authClient = authClient;
        this.fineClient = fineClient;
        this.emailService = emailService;
        this.borrowEventProducer = borrowEventProducer;
    }

    // ==========================================
    // BORROW BOOK
    // ==========================================

    public Borrow borrowBook(
            Long userId,
            Long bookId,
            Integer borrowingDays) {

        // ==========================================
        // 1. CHECK IF USER ALREADY BORROWED THIS BOOK
        // ==========================================

        boolean alreadyBorrowed =
                borrowRepository.existsByUserIdAndBookIdAndStatus(
                        userId,
                        bookId,
                        "BORROWED");

        if (alreadyBorrowed) {
            throw new RuntimeException(
                    "You have already borrowed this book");
        }

        // ==========================================
        // 2. ISSUE DATE
        // ==========================================

        LocalDate issueDate = LocalDate.now();

        Map<String, Object> book = null;
        Borrow borrow = null;
        boolean bookBorrowed = false;

        try {

            // ==========================================
            // 3. GET BOOK DETAILS
            // ==========================================

            book = bookClient.getBookById(bookId);

            String bookTitle =
                    String.valueOf(book.get("title"));

            Integer availableCopies =
                    ((Number) book.get("availableCopies"))
                            .intValue();

            // ==========================================
            // 4. VALIDATE BORROWING PERIOD
            // ==========================================

            if (borrowingDays == null) {
                throw new RuntimeException(
                        "Borrowing period is required");
            }

            if (borrowingDays < 1 || borrowingDays > 60) {
                throw new RuntimeException(
                        "Borrowing period must be between 1 and 60 days");
            }

            // ==========================================
            // 5. CHECK BOOK AVAILABILITY
            // ==========================================

            if (availableCopies <= 0) {
                throw new RuntimeException(
                        "No copies available for this book");
            }

            // ==========================================
            // 6. CALCULATE FIXED DUE DATE
            // ==========================================

            LocalDate dueDate =
                    issueDate.plusDays(borrowingDays);

            // ==========================================
            // 7. CREATE INITIAL FAILED RECORD
            // ==========================================

            borrow = Borrow.builder()
                    .userId(userId)
                    .bookId(bookId)
                    .issueDate(issueDate)
                    .dueDate(dueDate)
                    .status("FAILED")
                    .build();

            borrow = borrowRepository.save(borrow);

            // ==========================================
            // 8. GET USER DETAILS
            // ==========================================

            Map<String, Object> user =
                    authClient.getUserById(userId);

            String userName =
                    String.valueOf(user.get("name"));

            String userEmail =
                    String.valueOf(user.get("email"));

            // ==========================================
            // 9. DECREASE BOOK AVAILABILITY
            // ==========================================

            bookClient.borrowBook(bookId);

            bookBorrowed = true;

            // ==========================================
            // 10. MARK BORROWING SUCCESSFUL
            // ==========================================

            borrow.setStatus("BORROWED");

            Borrow savedBorrow =
                    borrowRepository.save(borrow);

            // ==========================================
            // 11. SEND KAFKA EVENT
            // ==========================================

            borrowEventProducer.sendBorrowApprovedEvent(
                    savedBorrow.getBorrowId(),
                    savedBorrow.getUserId(),
                    savedBorrow.getBookId()
            );

            // ==========================================
            // 12. SEND BORROW CONFIRMATION EMAIL
            // ==========================================

            try {

                emailService.sendBorrowConfirmation(
                        userEmail,
                        userName,
                        bookTitle,
                        issueDate.toString(),
                        dueDate.toString());

            } catch (Exception emailException) {

                System.out.println(
                        "Borrow confirmation email failed: "
                                + emailException.getMessage());
            }

            return savedBorrow;

        } catch (Exception exception) {

            // ==========================================
            // 13. ROLLBACK BOOK AVAILABILITY
            // ==========================================

            if (bookBorrowed) {

                try {

                    bookClient.returnBook(bookId);

                } catch (Exception rollbackException) {

                    System.out.println(
                            "WARNING: Failed to restore book availability: "
                                    + rollbackException.getMessage());
                }
            }

            // ==========================================
            // 14. KEEP FAILED BORROW RECORD
            // ==========================================

            if (borrow != null) {

                borrow.setStatus("FAILED");

                borrowRepository.save(borrow);
            }

            // ==========================================
            // 15. RETURN ERROR
            // ==========================================

            throw new RuntimeException(
                    "Borrow failed: "
                            + exception.getMessage());
        }
    }


    // ==========================================
    // INTERNAL - BOOK DELETE PROTECTION
    // ==========================================

    public boolean hasActiveBorrowing(Long bookId) {

        return borrowRepository.existsByBookIdAndStatusIn(
                bookId,
                List.of("PENDING", "BORROWED")
        );
    }


    // ==========================================
    // USER: CREATE BORROW REQUEST
    // ==========================================

    public Borrow createBorrowRequest(
            Long userId,
            Long bookId) {

        // ==========================================
        // 1. CHECK IF USER ALREADY HAS THIS BOOK
        // ==========================================

        boolean alreadyBorrowed =
                borrowRepository.existsByUserIdAndBookIdAndStatus(
                        userId,
                        bookId,
                        "BORROWED");

        if (alreadyBorrowed) {
            throw new RuntimeException(
                    "You have already borrowed this book");
        }

        // ==========================================
        // 2. CHECK IF THERE IS ALREADY A PENDING REQUEST
        // ==========================================

        boolean alreadyRequested =
                borrowRepository.existsByUserIdAndBookIdAndStatus(
                        userId,
                        bookId,
                        "PENDING");

        if (alreadyRequested) {
            throw new RuntimeException(
                    "You already have a pending request for this book");
        }

        // ==========================================
        // 3. CHECK BOOK EXISTS AND AVAILABILITY
        // ==========================================

        Map<String, Object> book =
                bookClient.getBookById(bookId);

        if (book == null) {
            throw new RuntimeException(
                    "Book not found");
        }

        if (Boolean.FALSE.equals(book.get("active"))) {
            throw new RuntimeException(
                    "This book is no longer available in the catalogue");
        }

        Integer availableCopies =
                ((Number) book.get("availableCopies"))
                        .intValue();

        if (availableCopies <= 0) {
            throw new RuntimeException(
                    "No copies available for this book");
        }

        // ==========================================
        // 4. CREATE PENDING REQUEST
        // ==========================================

        Borrow borrow =
                Borrow.builder()
                        .userId(userId)
                        .bookId(bookId)
                        .borrowingDays(null)
                        .issueDate(null)
                        .dueDate(null)
                        .returnDate(null)
                        .status("PENDING")
                        .build();

        Borrow savedBorrow =
                borrowRepository.save(borrow);

        // ==========================================
        // 5. SEND NEW BORROW REQUEST EMAIL TO ADMIN
        // ==========================================

        try {

            Map<String, Object> user =
                    authClient.getUserById(userId);

            String userName =
                    String.valueOf(user.get("name"));

            String userEmail =
                    String.valueOf(user.get("email"));

            String bookTitle =
                    String.valueOf(book.get("title"));

            emailService.sendNewBorrowRequestEmail(
                    adminEmail,
                    "Admin",
                    userName,
                    userEmail,
                    bookTitle,
                    LocalDate.now().toString());

        } catch (Exception emailException) {

            System.out.println(
                    "New borrow request email failed: "
                            + emailException.getMessage());
        }

        return savedBorrow;
    }


    // ==========================================
    // ADMIN: APPROVE BORROW REQUEST
    // ==========================================

    public Borrow approveBorrowRequest(
            Long borrowId,
            Integer borrowingDays) {

        // ==========================================
        // 1. FIND BORROW REQUEST
        // ==========================================

        Borrow borrow =
                borrowRepository.findById(borrowId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Borrow request not found"));

        // ==========================================
        // 2. CHECK REQUEST STATUS
        // ==========================================

        if (!"PENDING".equals(borrow.getStatus())) {

            throw new RuntimeException(
                    "Only pending borrow requests can be approved");
        }

        // ==========================================
        // 3. VALIDATE BORROWING PERIOD
        // ==========================================

        if (borrowingDays == null) {

            throw new RuntimeException(
                    "Borrowing period is required");
        }

        if (borrowingDays < 1 || borrowingDays > 60) {

            throw new RuntimeException(
                    "Borrowing period must be between 1 and 60 days");
        }

        // ==========================================
        // 4. CHECK BOOK AVAILABILITY AGAIN
        // ==========================================

        Map<String, Object> book =
                bookClient.getBookById(
                        borrow.getBookId());

        if (book == null) {

            throw new RuntimeException(
                    "Book not found");
        }

        if (Boolean.FALSE.equals(book.get("active"))) {

            throw new RuntimeException(
                    "This book is no longer available in the catalogue");
        }

        Integer availableCopies =
                ((Number) book.get("availableCopies"))
                        .intValue();

        if (availableCopies <= 0) {

            throw new RuntimeException(
                    "No copies available for this book");
        }

        // ==========================================
        // 5. GET USER DETAILS
        // ==========================================

        Map<String, Object> user =
                authClient.getUserById(
                        borrow.getUserId());

        String userName =
                String.valueOf(user.get("name"));

        String userEmail =
                String.valueOf(user.get("email"));

        String bookTitle =
                String.valueOf(book.get("title"));

        // ==========================================
        // 6. ISSUE DATE
        // ==========================================

        LocalDate issueDate =
                LocalDate.now();

        // ==========================================
        // 7. CALCULATE FIXED DUE DATE
        // ==========================================

        LocalDate dueDate =
                issueDate.plusDays(borrowingDays);

        // ==========================================
        // 8. DECREASE BOOK AVAILABILITY
        // ==========================================

        bookClient.borrowBook(
                borrow.getBookId());

        // ==========================================
        // 9. CONVERT REQUEST INTO BORROWED
        // ==========================================

        borrow.setBorrowingDays(borrowingDays);
        borrow.setIssueDate(issueDate);
        borrow.setDueDate(dueDate);
        borrow.setStatus("BORROWED");

        Borrow savedBorrow =
                borrowRepository.save(borrow);

        // ==========================================
        // 10. SEND KAFKA EVENT
        // ==========================================

        borrowEventProducer.sendBorrowApprovedEvent(
                savedBorrow.getBorrowId(),
                savedBorrow.getUserId(),
                savedBorrow.getBookId()
        );

        // ==========================================
        // 11. SEND BORROW CONFIRMATION EMAIL
        // ==========================================

        try {

            emailService.sendBorrowConfirmation(
                    userEmail,
                    userName,
                    bookTitle,
                    issueDate.toString(),
                    dueDate.toString());

        } catch (Exception emailException) {

            System.out.println(
                    "Borrow confirmation email failed: "
                            + emailException.getMessage());
        }

        return savedBorrow;
    }


    // ==========================================
    // ADMIN: REJECT BORROW REQUEST
    // ==========================================

    public Borrow rejectBorrowRequest(
            Long borrowId) {

        // ==========================================
        // 1. FIND REQUEST
        // ==========================================

        Borrow borrow =
                borrowRepository.findById(borrowId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Borrow request not found"));

        // ==========================================
        // 2. CHECK STATUS
        // ==========================================

        if (!"PENDING".equals(borrow.getStatus())) {

            throw new RuntimeException(
                    "Only pending borrow requests can be rejected");
        }

        // ==========================================
        // 3. MARK AS REJECTED
        // ==========================================

        borrow.setStatus("REJECTED");

        return borrowRepository.save(borrow);
    }


    // ==========================================
    // RETURN BOOK
    // ==========================================

    public Borrow returnBook(
            Long borrowId,
            Long userId) {

        // ==========================================
        // 1. FIND USER'S BORROW RECORD
        // ==========================================

        Borrow borrow =
                borrowRepository
                        .findByBorrowIdAndUserId(
                                borrowId,
                                userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Borrow record not found"));

        // ==========================================
        // 2. CHECK BORROW STATUS
        // ==========================================

        if (!"BORROWED".equals(borrow.getStatus())) {

            throw new RuntimeException(
                    "Book has already been returned or borrow failed");
        }

        // ==========================================
        // 3. CALCULATE / CHECK FINE BEFORE RETURN
        // ==========================================

        Map<String, Object> fine = null;

        try {

            /*
             * Ask Fine Service to calculate the fine.
             *
             * If the book is overdue:
             *     Fine Service creates/updates the fine.
             *
             * If the book is not overdue:
             *     Fine Service returns a message saying
             *     that no fine is applicable.
             */

            fine =
                    fineClient.calculateFine(borrowId);

            // ==========================================
            // HANDLE "NO FINE APPLICABLE" RESPONSE
            // ==========================================

            if (fine != null &&
                    fine.get("status") == null) {

                String message =
                        String.valueOf(
                                fine.get("message"));

                if (message.contains(
                        "No fine applicable")) {

                    fine = null;
                }
            }

        } catch (Exception exception) {

            String message =
                    exception.getMessage();

            // ==========================================
            // NO FINE APPLICABLE
            // ==========================================

            if (message != null &&
                    message.contains(
                            "No fine applicable")) {

                fine = null;
            }

            // ==========================================
            // NO CURRENT BORROWING
            // ==========================================

            else if (message != null &&
                    message.contains(
                            "No current borrowing found")) {

                throw new RuntimeException(
                        "Borrow record is no longer active.");
            }

            // ==========================================
            // OTHER FINE SERVICE ERROR
            // ==========================================

            else {

                throw new RuntimeException(
                        "Unable to verify fine status. "
                                + "Please try again later.");
            }
        }

        // ==========================================
        // 4. APPLY FINE PAYMENT RULE
        // ==========================================

        if (fine != null) {

            String fineStatus =
                    String.valueOf(
                            fine.get("status"));

            String paymentStatus =
                    String.valueOf(
                            fine.get("paymentStatus"));

            // ==========================================
            // UNPAID FINE
            // ==========================================

            if ("UNPAID".equals(fineStatus)) {

                // --------------------------------------
                // PAYMENT WAITING FOR ADMIN APPROVAL
                // --------------------------------------

                if ("PENDING_APPROVAL".equals(
                        paymentStatus)) {

                    throw new RuntimeException(
                            "Payment is waiting for admin approval. "
                                    + "Book return will be available after payment approval.");
                }

                // --------------------------------------
                // PAYMENT REJECTED
                // --------------------------------------

                if ("REJECTED".equals(
                        paymentStatus)) {

                    throw new RuntimeException(
                            "Your payment was rejected. "
                                    + "Please make the payment again.");
                }

                // --------------------------------------
                // NORMAL UNPAID FINE
                // --------------------------------------

                throw new RuntimeException(
                        "Outstanding fine must be paid "
                                + "before returning this book.");
            }

            // ==========================================
            // PAID FINE
            // ==========================================

            if ("PAID".equals(fineStatus)) {

                // Return is allowed.
            }

            // ==========================================
            // WAIVED FINE
            // ==========================================

            else if ("WAIVED".equals(fineStatus)) {

                // Return is allowed.
            }

            // ==========================================
            // UNKNOWN FINE STATUS
            // ==========================================

            else {

                throw new RuntimeException(
                        "Fine status does not allow book return.");
            }
        }

        // ==========================================
        // 5. GET BOOK DETAILS
        // ==========================================

        Map<String, Object> book =
                bookClient.getBookById(
                        borrow.getBookId());

        String bookTitle =
                String.valueOf(
                        book.get("title"));

        // ==========================================
        // 6. RETURN BOOK TO BOOK SERVICE
        // ==========================================

        bookClient.returnBook(
                borrow.getBookId());

        // ==========================================
        // 7. UPDATE BORROW RECORD
        // ==========================================

        borrow.setReturnDate(
                LocalDate.now());

        borrow.setStatus("RETURNED");

        Borrow savedBorrow =
                borrowRepository.save(borrow);

        // ==========================================
        // 8. SEND BOOK RETURNED KAFKA EVENT
        // ==========================================

        borrowEventProducer.sendBookReturnedEvent(
                savedBorrow.getBorrowId(),
                savedBorrow.getUserId(),
                savedBorrow.getBookId()
        );

        // ==========================================
        // 9. GET USER DETAILS
        // ==========================================

        Map<String, Object> user =
                authClient.getUserById(userId);

        String userName =
                String.valueOf(
                        user.get("name"));

        String userEmail =
                String.valueOf(
                        user.get("email"));

        // ==========================================
        // 10. GET FINAL FINE AMOUNT FOR EMAIL
        // ==========================================

        double fineAmount = 0.0;

        if (fine != null &&
                fine.get("fineAmount") != null) {

            fineAmount =
                    ((Number) fine.get("fineAmount"))
                            .doubleValue();
        }

        // ==========================================
        // 11. SEND RETURN CONFIRMATION EMAIL
        // ==========================================

        try {

            emailService.sendReturnConfirmation(
                    userEmail,
                    userName,
                    bookTitle,
                    borrow.getReturnDate().toString(),
                    fineAmount);

        } catch (Exception emailException) {

            System.out.println(
                    "Return confirmation email failed: "
                            + emailException.getMessage());
        }

        return savedBorrow;
    }
    


    // ==========================================
    // GET USER BORROWINGS
    // ==========================================

    public List<Borrow> getBorrowingsByUser(
            Long userId) {

        return borrowRepository
                .findByUserId(userId);
    }
 // ==========================================
 // ADMIN: GET ALL BORROWINGS
 // ==========================================

 public List<Borrow> getAllBorrowingsForAdmin() {
     return borrowRepository.findAll();
 }


    // ==========================================
    // ADMIN: GET ALL PENDING BORROW REQUESTS
    // WITH USER AND BOOK DETAILS
    // ==========================================

    public List<AdminBorrowResponse> getPendingBorrowRequests() {

        List<Borrow> pendingRequests =
                borrowRepository.findByStatus("PENDING");

        return pendingRequests.stream()
                .map(borrow -> {

                    // Get user details from Auth Service
                    Map<String, Object> user =
                            authClient.getUserById(
                                    borrow.getUserId());

                    // Get book details from Book Service
                    Map<String, Object> book =
                            bookClient.getBookById(
                                    borrow.getBookId());

                    return AdminBorrowResponse.builder()

                            // Borrow details
                            .borrowId(borrow.getBorrowId())
                            .status(borrow.getStatus())
                            .requestedAt(borrow.getCreatedAt())

                            // User details
                            .userId(borrow.getUserId())
                            .userName(
                                    String.valueOf(
                                            user.get("name")))
                            .userEmail(
                                    String.valueOf(
                                            user.get("email")))

                            // Book details
                            .bookId(borrow.getBookId())
                            .bookTitle(
                                    String.valueOf(
                                            book.get("title")))
                            .bookAuthor(
                                    String.valueOf(
                                            book.get("author")))
                            .bookCategory(
                                    String.valueOf(
                                            book.get("category")))

                            // Borrow details
                            .borrowingDays(
                                    borrow.getBorrowingDays())
                            .issueDate(
                                    borrow.getIssueDate())
                            .dueDate(
                                    borrow.getDueDate())
                            .returnDate(
                                    borrow.getReturnDate())

                            .build();
                })
                .toList();
    }


    // ==========================================
    // GET BORROW BY ID
    // ==========================================

    public Borrow getBorrowById(
            Long borrowId) {

        return borrowRepository
                .findById(borrowId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Borrow record not found"));
    }
 // ==========================================
 // CHECK USER BORROW HISTORY FOR DELETION
 // ==========================================

 public boolean canDeleteUser(Long userId) {

     List<Borrow> userBorrows =
             borrowRepository.findByUserId(userId);

     return userBorrows.stream()
             .noneMatch(borrow ->
                     "PENDING".equalsIgnoreCase(borrow.getStatus()) ||
                     "BORROWED".equalsIgnoreCase(borrow.getStatus()));
 }

    // ==========================================
    // DELETE USER BORROW HISTORY
    // Used by Auth Service during approved
    // permanent account deletion.
    // ==========================================

    public void deleteUserBorrowHistory(Long userId) {

        List<Borrow> userBorrows =
                borrowRepository.findByUserId(userId);

        boolean hasActiveBorrow =
                userBorrows.stream()
                        .anyMatch(borrow ->
                                "PENDING".equalsIgnoreCase(borrow.getStatus()) ||
                                "BORROWED".equalsIgnoreCase(borrow.getStatus()));

        if (hasActiveBorrow) {
            throw new RuntimeException(
                    "User cannot be permanently deleted while an active borrow request or active loan exists");
        }

        borrowRepository.deleteAll(userBorrows);
    }

}