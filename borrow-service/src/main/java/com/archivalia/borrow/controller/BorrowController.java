package com.archivalia.borrow.controller;

import java.util.List;

import com.archivalia.borrow.dto.AdminBorrowResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.archivalia.borrow.dto.AdminApproveBorrowRequest;
import com.archivalia.borrow.dto.BorrowRequest;
import com.archivalia.borrow.entity.Borrow;
import com.archivalia.borrow.security.JwtService;
import com.archivalia.borrow.service.BorrowService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/borrows")
public class BorrowController {

    private final BorrowService borrowService;
    private final JwtService jwtService;

    public BorrowController(
            BorrowService borrowService,
            JwtService jwtService) {

        this.borrowService = borrowService;
        this.jwtService = jwtService;
    }

    // =========================
    // USER: REQUEST TO BORROW
    // =========================

    @PostMapping("/request")
    public ResponseEntity<Borrow> requestBorrow(
            @Valid @RequestBody BorrowRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        Borrow borrow = borrowService.createBorrowRequest(
                userId,
                request.getBookId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(borrow);
    }

    // =========================
    // ADMIN: APPROVE BORROW
    // =========================

    @PutMapping("/{borrowId}/approve")
    public ResponseEntity<Borrow> approveBorrow(
            @PathVariable Long borrowId,
            @Valid @RequestBody AdminApproveBorrowRequest request,
            Authentication authentication) {

        requireAdmin(authentication);

        Borrow borrow = borrowService.approveBorrowRequest(
                borrowId,
                request.getBorrowingDays()
        );

        return ResponseEntity.ok(borrow);
    }

    // =========================
    // ADMIN: REJECT BORROW
    // =========================

    @PutMapping("/{borrowId}/reject")
    public ResponseEntity<Borrow> rejectBorrow(
            @PathVariable Long borrowId,
            Authentication authentication) {

        requireAdmin(authentication);

        Borrow borrow = borrowService.rejectBorrowRequest(
                borrowId
        );

        return ResponseEntity.ok(borrow);
    }

    // =========================
    // RETURN BOOK
    // =========================

    @PutMapping("/{borrowId}/return")
    public ResponseEntity<Borrow> returnBook(
            @PathVariable Long borrowId,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                borrowService.returnBook(
                        borrowId,
                        userId
                )
        );
    }

    // =========================
    // GET MY BORROWINGS
    // =========================

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Borrow>> getUserBorrowings(
            @PathVariable Long userId,
            Authentication authentication) {

        Long loggedInUserId = getUserId(authentication);

        if (!loggedInUserId.equals(userId)) {
            throw new RuntimeException(
                    "You are not authorized to access this user's borrowings"
            );
        }

        return ResponseEntity.ok(
                borrowService.getBorrowingsByUser(userId)
        );
    }

    // =========================
    // GET BORROW BY ID
    // =========================

    @GetMapping("/{borrowId}")
    public ResponseEntity<Borrow> getBorrowById(
            @PathVariable Long borrowId,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        Borrow borrow =
                borrowService.getBorrowById(borrowId);

        if (!borrow.getUserId().equals(userId)) {
            throw new RuntimeException(
                    "You are not authorized to access this borrow record"
            );
        }

        return ResponseEntity.ok(borrow);
    }
 // ==========================================
 // ADMIN: GET ALL BORROWINGS
 // ==========================================

 @GetMapping("/admin/all")
 public ResponseEntity<List<Borrow>> getAllBorrowingsForAdmin(
         Authentication authentication) {

     requireAdmin(authentication);

     return ResponseEntity.ok(
             borrowService.getAllBorrowingsForAdmin()
     );
 }
    // =========================
    // ADMIN: GET PENDING REQUESTS
    // =========================

    @GetMapping("/admin/pending")
    public ResponseEntity<List<AdminBorrowResponse>> getPendingBorrowRequests(
            Authentication authentication) {

        requireAdmin(authentication);

        return ResponseEntity.ok(
                borrowService.getPendingBorrowRequests()
        );
    }

    // =========================
    // INTERNAL - FINE SERVICE
    // =========================

    @GetMapping("/internal/{borrowId}")
    public ResponseEntity<Borrow> getBorrowByIdInternal(
            @PathVariable Long borrowId) {

        return ResponseEntity.ok(
                borrowService.getBorrowById(borrowId)
        );
    }

    // =========================
    // INTERNAL - BOOK SERVICE
    // =========================

    @GetMapping("/internal/book/{bookId}/active")
    public ResponseEntity<Boolean> hasActiveBorrowing(
            @PathVariable Long bookId) {

        return ResponseEntity.ok(
                borrowService.hasActiveBorrowing(bookId)
        );
    }

    // =========================
    // INTERNAL - AUTH SERVICE
    // =========================
    // Deletes the user's borrow history after
    // permanent account deletion has been approved.

    @DeleteMapping("/internal/users/{userId}")
    public ResponseEntity<String> deleteUserBorrowHistory(
            @PathVariable Long userId) {

        borrowService.deleteUserBorrowHistory(userId);

        return ResponseEntity.ok(
                "User borrow history deleted successfully"
        );
    }
 // ==========================================
 // INTERNAL - CHECK USER DELETION
 // ==========================================

 @GetMapping("/internal/users/{userId}/deletion-check")
 public ResponseEntity<Boolean> checkUserDeletion(
         @PathVariable Long userId) {

     return ResponseEntity.ok(
             borrowService.canDeleteUser(userId)
     );
 }

    // =========================
    // EXTRACT USER ID FROM JWT
    // =========================

    private Long getUserId(
            Authentication authentication) {

        if (authentication == null ||
                authentication.getCredentials() == null) {

            throw new RuntimeException(
                    "Invalid authentication"
            );
        }

        String token =
                authentication.getCredentials().toString();

        return jwtService.extractUserId(token);
    }

    // =========================
    // CHECK ADMIN ROLE
    // =========================

    private void requireAdmin(
            Authentication authentication) {

        if (authentication == null ||
                authentication.getAuthorities() == null ||
                authentication.getAuthorities()
                        .stream()
                        .noneMatch(authority ->
                                "ROLE_ADMIN".equals(
                                        authority.getAuthority()))) {

            throw new RuntimeException(
                    "Only administrators can perform this action"
            );
        }
    }
}