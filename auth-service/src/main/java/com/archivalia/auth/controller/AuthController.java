package com.archivalia.auth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.archivalia.auth.dto.ForgotPasswordRequest;
import com.archivalia.auth.dto.LoginRequest;
import com.archivalia.auth.dto.LoginResponse;
import com.archivalia.auth.dto.MobileOtpVerificationRequest;
import com.archivalia.auth.dto.OtpVerificationRequest;
import com.archivalia.auth.dto.RegisterRequest;
import com.archivalia.auth.dto.RegisterResponse;
import com.archivalia.auth.dto.ResendEmailOtpRequest;
import com.archivalia.auth.dto.ResendMobileOtpRequest;
import com.archivalia.auth.dto.ResetPasswordRequest;
import com.archivalia.auth.dto.UpdateProfileRequest;
import com.archivalia.auth.dto.VerifyPasswordResetOtpRequest;
import com.archivalia.auth.service.AuthService;

import jakarta.validation.Valid;
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =========================
    // REGISTER
    // =========================

    @PostMapping("/register")
    public RegisterResponse register(
            @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request) {

        return authService.login(request);
    }

    // =========================
    // TEST
    // =========================

    @GetMapping("/test")
    public String test() {

        return "JWT Authentication Successful";
    }

    // =========================
    // EMAIL OTP
    // =========================

    @PostMapping("/verify-email-otp")
    public String verifyEmailOtp(
            @RequestBody OtpVerificationRequest request) {

        return authService.verifyEmailOtp(request);
    }

    // =========================
    // MOBILE OTP
    // =========================

    @PostMapping("/verify-mobile-otp")
    public String verifyMobileOtp(
            @RequestBody MobileOtpVerificationRequest request) {

        return authService.verifyMobileOtp(request);
    }

    // =========================
    // RESEND EMAIL OTP
    // =========================

    @PostMapping("/resend-email-otp")
    public String resendEmailOtp(
            @RequestBody ResendEmailOtpRequest request) {

        return authService.resendEmailOtp(request);
    }

    // =========================
    // RESEND MOBILE OTP
    // =========================

    @PostMapping("/resend-mobile-otp")
    public String resendMobileOtp(
            @RequestBody ResendMobileOtpRequest request) {

        return authService.resendMobileOtp(request);
    }

    // =========================
    // GET USER BY ID
    // =========================

    @GetMapping("/users/{userId}")
    public RegisterResponse getUserById(
            @PathVariable Long userId) {

        return authService.getUserById(userId);
    }

    // =========================
    // UPDATE USER PROFILE
    // =========================

    @PutMapping("/users/{userId}/profile")
    public ResponseEntity<RegisterResponse> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request,
            org.springframework.security.core.Authentication authentication) {

        return ResponseEntity.ok(
                authService.updateUserProfile(
                        userId,
                        request,
                        authentication
                )
        );
    }

    // =========================
    // FORGOT PASSWORD
    // =========================

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.sendForgotPasswordOtp(
                request.getEmail()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password reset OTP sent successfully"
                )
        );
    }

    // =========================
    // VERIFY PASSWORD RESET OTP
    // =========================

    @PostMapping("/verify-password-reset-otp")
    public ResponseEntity<Map<String, String>> verifyPasswordResetOtp(
            @Valid @RequestBody VerifyPasswordResetOtpRequest request) {

        authService.verifyPasswordResetOtp(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password reset OTP verified successfully"
                )
        );
    }

    // =========================
    // RESET PASSWORD
    // =========================

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password reset successfully"
                )
        );
    }


    // =========================
    // USER - REQUEST ACCOUNT DELETION
    // =========================

    @PostMapping("/users/{userId}/delete-request")
    public ResponseEntity<Map<String, String>> requestAccountDeletion(
            @PathVariable Long userId,
            Authentication authentication) {

        authService.requestAccountDeletion(
                userId,
                authentication
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Account deletion request submitted successfully. "
                                + "Waiting for administrator approval."
                )
        );
    }

    // =========================
    // USER - GET DELETION REQUEST STATUS
    // =========================

    @GetMapping("/users/{userId}/delete-request")
    public ResponseEntity<Map<String, Object>> getDeletionRequestStatus(
            @PathVariable Long userId,
            Authentication authentication) {

        return ResponseEntity.ok(
                authService.getDeletionRequestStatus(
                        userId,
                        authentication
                )
        );
    }

    // =========================
    // ADMIN - GET DELETION REQUESTS
    // =========================

    @GetMapping("/admin/users/deletion-requests")
    public ResponseEntity<List<Map<String, Object>>> getDeletionRequests() {

        return ResponseEntity.ok(
                authService.getDeletionRequests()
        );
    }

    // =========================
    // ADMIN - REJECT DELETION REQUEST
    // =========================

    @PutMapping("/admin/users/{userId}/delete-request/reject")
    public ResponseEntity<Map<String, String>> rejectAccountDeletion(
            @PathVariable Long userId) {

        authService.rejectAccountDeletion(userId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Account deletion request rejected successfully."
                )
        );
    }

    // =========================
    // ADMIN - APPROVE DELETION REQUEST
    // =========================

    @PutMapping("/admin/users/{userId}/delete-request/approve")
    public ResponseEntity<Map<String, String>> approveAccountDeletion(
            @PathVariable Long userId) {

        authService.approveAccountDeletion(userId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Account deletion approved successfully."
                )
        );
    }
    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUserDirectly(
            @PathVariable Long userId) {

        authService.deleteUserDirectly(userId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "User account permanently deleted successfully."
                )
        );
    }

    // =========================
    // ADMIN - SUSPEND USER
    // =========================

    @PutMapping("/admin/users/{userId}/suspend")
    public ResponseEntity<Map<String, String>> suspendUser(
            @PathVariable Long userId) {

        authService.suspendUser(userId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "User account suspended successfully"
                )
        );
    }

    // =========================
    // ADMIN - REACTIVATE USER
    // =========================

    @PutMapping("/admin/users/{userId}/reactivate")
    public ResponseEntity<Map<String, String>> reactivateUser(
            @PathVariable Long userId) {

        authService.reactivateUser(userId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "User account reactivated successfully"
                )
        );
    }
    @PutMapping("/admin/users/{userId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @PathVariable Long userId) {

        authService.deactivateUser(userId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "User account deactivated successfully"
                )
        );
    }
 // =========================
 // ADMIN - GET ALL USERS
 // =========================

 @GetMapping("/admin/users")
 public ResponseEntity<List<RegisterResponse>> getAllUsers() {

     return ResponseEntity.ok(
             authService.getAllUsers()
     );
 }

    // =========================
    // INTERNAL - USER STATUS
    // =========================
    // Used by other microservices through Eureka + OpenFeign

    @GetMapping("/internal/users/{userId}/status")
    public ResponseEntity<Map<String, Object>> getUserStatus(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                authService.getUserStatus(userId)
        );
    }
}