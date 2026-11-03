package com.archivalia.fine.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.archivalia.fine.entity.Fine;
import com.archivalia.fine.security.JwtService;
import com.archivalia.fine.service.FineService;

@RestController
@RequestMapping("/fines")
public class FineController {

    private final FineService fineService;
    private final JwtService jwtService;

    public FineController(
            FineService fineService,
            JwtService jwtService) {
        this.fineService = fineService;
        this.jwtService = jwtService;
    }

    // ==========================================
    // CALCULATE FINE
    // ==========================================

    @PostMapping("/calculate/{borrowId}")
    public ResponseEntity<?> calculateFine(
            @PathVariable Long borrowId) {

        return fineService.calculateFineResponse(borrowId);
    }

 // ==========================================
 // USER - I HAVE PAID
 // ==========================================

 @PutMapping("/{fineId}/payment-submitted")
 public ResponseEntity<?> submitPayment(
         @PathVariable Long fineId,
         Authentication authentication) {

     Long loggedInUserId = getUserId(authentication);

     fineService.submitPayment(
             fineId,
             loggedInUserId
     );

     return ResponseEntity.ok(
             Map.of(
                     "message",
                     "Payment submitted successfully. Waiting for admin approval."
             )
     );
 }
    // ==========================================
    // ADMIN - APPROVE PAYMENT
    // ==========================================

    @PutMapping("/{fineId}/payment-approve")
    public ResponseEntity<?> approvePayment(
            @PathVariable Long fineId) {

        fineService.approvePayment(fineId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Payment approved successfully. Fine is now marked as PAID."
                )
        );
    }
 // ==========================================
 // ADMIN - REJECT PAYMENT
 // ==========================================

 @PutMapping("/{fineId}/payment-reject")
 public ResponseEntity<?> rejectPayment(
         @PathVariable Long fineId) {

     fineService.rejectPayment(fineId);

     return ResponseEntity.ok(
             Map.of(
                     "message",
                     "Payment rejected successfully. Fine remains unpaid."
             )
     );
 }

    // ==========================================
    // GET FINE BY ID
    // ==========================================

    @GetMapping("/{fineId}")
    public ResponseEntity<Fine> getFineById(
            @PathVariable Long fineId,
            Authentication authentication) {

        Fine fine = fineService.getFineById(fineId);

        authorizeFineAccess(fine, authentication);

        return ResponseEntity.ok(fine);
    }

    // ==========================================
    // GET FINE BY BORROW ID
    // ==========================================

    @GetMapping("/borrow/{borrowId}")
    public ResponseEntity<?> getFineByBorrowId(
            @PathVariable Long borrowId,
            Authentication authentication) {

        Fine fine = fineService.getFineByBorrowId(borrowId);

        authorizeFineAccess(fine, authentication);

        return ResponseEntity.ok(fine);
    }

    // ==========================================
    // GET USER FINES
    // ==========================================

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Fine>> getFinesByUser(
            @PathVariable Long userId,
            Authentication authentication) {

        Long loggedInUserId = getUserId(authentication);

        if (!isAdmin(authentication)
                && !loggedInUserId.equals(userId)) {

            throw new RuntimeException(
                    "You are not authorized to access this user's fines"
            );
        }

        return ResponseEntity.ok(
                fineService.getFinesByUser(userId)
        );
    }

 // ==========================================
 // INTERNAL - CHECK USER DELETION
 // ==========================================

 @GetMapping("/internal/users/{userId}/deletion-check")
 public ResponseEntity<Boolean> checkUserDeletion(
         @PathVariable Long userId) {

     return ResponseEntity.ok(
             fineService.canDeleteUser(userId)
     );
 }

    // ==========================================
    // INTERNAL - AUTH SERVICE
    // ==========================================
    // Deletes the user's fine/payment history after
    // permanent account deletion has been approved.

    @DeleteMapping("/internal/users/{userId}")
    public ResponseEntity<String> deleteUserFineHistory(
            @PathVariable Long userId) {

        fineService.deleteUserFineHistory(userId);

        return ResponseEntity.ok(
                "User fine history deleted successfully"
        );
    }

    // ==========================================
    // AUTHORIZATION HELPERS
    // ==========================================

    private Long getUserId(Authentication authentication) {

        if (authentication == null
                || authentication.getCredentials() == null) {

            throw new RuntimeException("Invalid authentication");
        }

        String token = authentication
                .getCredentials()
                .toString();

        return jwtService.extractUserId(token);
    }

    private boolean isAdmin(Authentication authentication) {

        if (authentication == null
                || authentication.getAuthorities() == null) {

            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()
                        )
                );
    }

    private void authorizeFineAccess(
            Fine fine,
            Authentication authentication) {

        if (fine == null) {
            throw new RuntimeException("Fine not found");
        }

        if (isAdmin(authentication)) {
            return;
        }

        Long loggedInUserId = getUserId(authentication);

        if (!loggedInUserId.equals(fine.getUserId())) {
            throw new RuntimeException(
                    "You are not authorized to access this fine"
            );
        }
    }
}