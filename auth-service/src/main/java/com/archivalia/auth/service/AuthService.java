package com.archivalia.auth.service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
import com.archivalia.auth.client.BorrowClient;
import com.archivalia.auth.client.FineClient;
import com.archivalia.auth.entity.User;
import com.archivalia.auth.repository.UserRepository;
@Service
public class AuthService {

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final EmailService emailService;

    private final SmsService smsService;

    private final BorrowClient borrowClient;

    private final FineClient fineClient;

    private final Random random = new Random();

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            EmailService emailService,
            SmsService smsService,
            BorrowClient borrowClient,
            FineClient fineClient) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.borrowClient = borrowClient;
        this.fineClient = fineClient;
    }

    // =========================
    // LOGIN
    // =========================

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new RuntimeException("Account is not active");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "Please verify your email before login");
        }

        if (!user.isMobileVerified()) {
            throw new RuntimeException(
                    "Please verify your mobile number before login");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        emailService.sendLoginNotification(
                user.getEmail(),
                user.getName()
        );

        return new LoginResponse(
                token,
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    // =========================
    // REGISTER
    // =========================

    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.findByMobileNumber(
                request.getMobileNumber()).isPresent()) {

            throw new RuntimeException(
                    "Mobile number is already registered");
        }

        if (userRepository.findByEmail(
                request.getEmail()).isPresent()) {

            throw new RuntimeException(
                    "Email is already registered");
        }

        // Generate email OTP
        String emailOtp = String.format(
                "%06d",
                random.nextInt(1000000)
        );

        // Generate mobile OTP
        String mobileOtp = String.format(
                "%06d",
                random.nextInt(1000000)
        );

        // Hash OTPs before storing them
        String emailOtpHash =
                passwordEncoder.encode(emailOtp);

        String mobileOtpHash =
                passwordEncoder.encode(mobileOtp);

        // Create user
        User user = User.builder()
                .name(request.getName())

                .email(request.getEmail())

                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )

                .mobileNumber(request.getMobileNumber())

                .emailVerified(false)

                .mobileVerified(false)

                .emailOtp(emailOtpHash)

                .emailOtpExpiry(
                        LocalDateTime.now().plusMinutes(5)
                )

                .mobileOtp(mobileOtpHash)

                .mobileOtpExpiry(
                        LocalDateTime.now().plusMinutes(5)
                )

                .role("USER")

                .accountStatus("ACTIVE")

                .build();

        // Save user
        User savedUser = userRepository.save(user);

        // Send actual OTP to email
        emailService.sendEmailOtp(
                savedUser.getEmail(),
                emailOtp
        );

        // Send actual OTP to mobile
        smsService.sendMobileOtp(
                savedUser.getMobileNumber(),
                mobileOtp
        );

        // Prepare response
        RegisterResponse response = new RegisterResponse();

        response.setUserId(savedUser.getUserId());

        response.setName(savedUser.getName());

        response.setEmail(savedUser.getEmail());

        response.setMobileNumber(
                savedUser.getMobileNumber()
        );

        response.setEmailVerified(
                savedUser.isEmailVerified()
        );

        response.setMobileVerified(
                savedUser.isMobileVerified()
        );

        response.setRole(savedUser.getRole());

        response.setAccountStatus(
                savedUser.getAccountStatus()
        );

        response.setCreatedAt(
                savedUser.getCreatedAt()
        );

        return response;
    }

    // =========================
    // GET USER BY ID
    // =========================

    public RegisterResponse getUserById(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        RegisterResponse response = new RegisterResponse();

        response.setUserId(user.getUserId());

        response.setName(user.getName());

        response.setEmail(user.getEmail());

        response.setMobileNumber(
                user.getMobileNumber()
        );

        response.setEmailVerified(
                user.isEmailVerified()
        );

        response.setMobileVerified(
                user.isMobileVerified()
        );

        response.setRole(user.getRole());

        response.setAccountStatus(
                user.getAccountStatus()
        );

        response.setCreatedAt(
                user.getCreatedAt()
        );

        return response;
    }
 // =========================
 // ADMIN - GET ALL USERS
 // =========================

 public List<RegisterResponse> getAllUsers() {

     return userRepository.findAll()
             .stream()
             .map(user -> {

                 RegisterResponse response = new RegisterResponse();

                 response.setUserId(user.getUserId());
                 response.setName(user.getName());
                 response.setEmail(user.getEmail());
                 response.setMobileNumber(user.getMobileNumber());

                 response.setEmailVerified(
                         user.isEmailVerified()
                 );

                 response.setMobileVerified(
                         user.isMobileVerified()
                 );

                 response.setRole(user.getRole());

                 response.setAccountStatus(
                         user.getAccountStatus()
                 );

                 response.setCreatedAt(
                         user.getCreatedAt()
                 );

                 return response;
             })
             .toList();
 }

    // =========================
    // UPDATE USER PROFILE
    // =========================

//=========================
//UPDATE USER PROFILE
//=========================

public RegisterResponse updateUserProfile(
      Long userId,
      UpdateProfileRequest request,
      Authentication authentication) {

  User targetUser = userRepository.findById(userId)
          .orElseThrow(() ->
                  new RuntimeException("User not found"));

  String loggedInEmail = authentication.getName();

  User loggedInUser = userRepository.findByEmail(loggedInEmail)
          .orElseThrow(() ->
                  new RuntimeException("Logged-in user not found"));

  boolean isAdmin = "ADMIN".equals(loggedInUser.getRole());

  // ==========================================
  // USER CAN ONLY UPDATE THEIR OWN PROFILE
  // ADMIN CAN UPDATE ANY USER
  // ==========================================

  if (!isAdmin &&
          !loggedInUser.getUserId().equals(targetUser.getUserId())) {

      throw new RuntimeException(
              "You are not authorized to update this user's profile");
  }

  // ==========================================
  // ACCOUNT STATUS
  // ==========================================

  if (!"ACTIVE".equals(targetUser.getAccountStatus())) {

      throw new RuntimeException(
              "Only active users can update their profile");
  }

  // ==========================================
  // NAME VALIDATION
  // ==========================================

  String newName = request.getName().trim();

  if (newName.equals(targetUser.getName())) {

      throw new RuntimeException(
              "New name must be different from current name");
  }

  // ==========================================
  // USER: MAXIMUM 2 NAME CHANGES
  // ADMIN: UNLIMITED
  // ==========================================

  if (!isAdmin &&
          targetUser.getNameChangeCount() >= 2) {

      throw new RuntimeException(
              "You have reached the maximum of 2 name changes");
  }

  targetUser.setName(newName);

  // Only count changes made by the user themselves.
  if (!isAdmin) {

      targetUser.setNameChangeCount(
              targetUser.getNameChangeCount() + 1
      );
  }

  User savedUser =
          userRepository.save(targetUser);

  return getUserById(
          savedUser.getUserId()
  );
}
    // =========================
    // VERIFY EMAIL OTP
    // =========================

    public String verifyEmailOtp(
            OtpVerificationRequest request) {

        User user = userRepository.findByEmail(
                request.getEmail()
        ).orElseThrow(() ->
                new RuntimeException("User not found"));

        if (user.isEmailVerified()) {

            return "Email is already verified";
        }

        if (user.getEmailOtp() == null) {

            throw new RuntimeException(
                    "No email OTP available");
        }

        // Check expiry first
        if (user.getEmailOtpExpiry() == null ||
                LocalDateTime.now().isAfter(
                        user.getEmailOtpExpiry())) {

            throw new RuntimeException(
                    "OTP has expired");
        }

        // Compare entered OTP with stored HASH
        if (!passwordEncoder.matches(
                request.getOtp(),
                user.getEmailOtp())) {

            throw new RuntimeException(
                    "Invalid OTP");
        }

        // Verification successful
        user.setEmailVerified(true);

        // Remove OTP after successful verification
        user.setEmailOtp(null);

        user.setEmailOtpExpiry(null);

        userRepository.save(user);

        return "Email verified successfully";
    }

    // =========================
    // VERIFY MOBILE OTP
    // =========================

    public String verifyMobileOtp(
            MobileOtpVerificationRequest request) {

        User user = userRepository.findByEmail(
                request.getEmail()
        ).orElseThrow(() ->
                new RuntimeException("User not found"));

        if (user.isMobileVerified()) {

            return "Mobile number is already verified";
        }

        if (user.getMobileOtp() == null) {

            throw new RuntimeException(
                    "No mobile OTP available");
        }

        // Check expiry first
        if (user.getMobileOtpExpiry() == null ||
                LocalDateTime.now().isAfter(
                        user.getMobileOtpExpiry())) {

            throw new RuntimeException(
                    "Mobile OTP has expired");
        }

        // Compare entered OTP with stored HASH
        if (!passwordEncoder.matches(
                request.getOtp(),
                user.getMobileOtp())) {

            throw new RuntimeException(
                    "Invalid mobile OTP");
        }

        // Verification successful
        user.setMobileVerified(true);

        // Remove OTP after successful verification
        user.setMobileOtp(null);

        user.setMobileOtpExpiry(null);

        userRepository.save(user);

        return "Mobile number verified successfully";
    }

    // =========================
    // RESEND EMAIL OTP
    // =========================

    public String resendEmailOtp(
            ResendEmailOtpRequest request) {

        User user = userRepository.findByEmail(
                request.getEmail()
        ).orElseThrow(() ->
                new RuntimeException("User not found"));

        if (user.isEmailVerified()) {

            return "Email is already verified";
        }

        // Generate new OTP
        String newOtp = String.format(
                "%06d",
                random.nextInt(1000000)
        );

        // Hash new OTP
        String newOtpHash =
                passwordEncoder.encode(newOtp);

        // Store HASH
        user.setEmailOtp(newOtpHash);

        user.setEmailOtpExpiry(
                LocalDateTime.now().plusMinutes(5)
        );

        userRepository.save(user);

        // Send actual OTP
        emailService.sendEmailOtp(
                user.getEmail(),
                newOtp
        );

        return "New email OTP sent successfully";
    }

    // =========================
    // FORGOT PASSWORD
    // =========================

    public void sendForgotPasswordOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Unable to process request"));

        // Generate 6-digit OTP
        String otp = String.format(
                "%06d",
                random.nextInt(1000000)
        );

        // Hash OTP before storing
        user.setPasswordResetOtp(
                passwordEncoder.encode(otp)
        );

        // OTP valid for 5 minutes
        user.setPasswordResetOtpExpiry(
                LocalDateTime.now().plusMinutes(5)
        );

        // IMPORTANT:
        // Every new OTP requires fresh verification
        user.setPasswordResetOtpVerified(false);

        userRepository.save(user);

        // Send actual OTP through Gmail
        emailService.sendPasswordResetOtp(
                user.getEmail(),
                user.getName(),
                otp
        );
    }

    // =========================
    // VERIFY PASSWORD RESET OTP
    // =========================

    public void verifyPasswordResetOtp(
            VerifyPasswordResetOtpRequest request) {

        User user = userRepository.findByEmail(
                request.getEmail()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Invalid OTP request"));

        if (user.getPasswordResetOtp() == null ||
                user.getPasswordResetOtpExpiry() == null) {

            throw new RuntimeException(
                    "No password reset OTP found");
        }

        // Check expiry
        if (LocalDateTime.now().isAfter(
                user.getPasswordResetOtpExpiry())) {

            throw new RuntimeException(
                    "Password reset OTP has expired");
        }

        // Compare entered OTP with stored HASH
        if (!passwordEncoder.matches(
                request.getOtp(),
                user.getPasswordResetOtp())) {

            throw new RuntimeException(
                    "Invalid password reset OTP");
        }

        // OTP successfully verified
        user.setPasswordResetOtpVerified(true);

        // OTP should no longer be usable
        user.setPasswordResetOtp(null);

        user.setPasswordResetOtpExpiry(null);

        userRepository.save(user);
    }

    // =========================
    // RESET PASSWORD
    // =========================

    public void resetPassword(
            ResetPasswordRequest request) {

        User user = userRepository.findByEmail(
                request.getEmail()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Invalid password reset request"));

        // OTP verification is mandatory
        if (!user.isPasswordResetOtpVerified()) {

            throw new RuntimeException(
                    "Please verify the password reset OTP first");
        }

        // Store new password as BCrypt hash
        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        // Clear password-reset state
        user.setPasswordResetOtp(null);

        user.setPasswordResetOtpExpiry(null);

        user.setPasswordResetOtpVerified(false);

        userRepository.save(user);
    }

    // =========================
    // RESEND MOBILE OTP
    // =========================

    public String resendMobileOtp(
            ResendMobileOtpRequest request) {

        User user = userRepository.findByEmail(
                request.getEmail()
        ).orElseThrow(() ->
                new RuntimeException("User not found"));

        if (user.isMobileVerified()) {

            return "Mobile number is already verified";
        }

        // Generate new OTP
        String newOtp = String.format(
                "%06d",
                random.nextInt(1000000)
        );

        // Hash new OTP
        String newOtpHash =
                passwordEncoder.encode(newOtp);

        // Store HASH
        user.setMobileOtp(newOtpHash);

        user.setMobileOtpExpiry(
                LocalDateTime.now().plusMinutes(5)
        );

        userRepository.save(user);

        // Send actual OTP through textbee
        smsService.sendMobileOtp(
                user.getMobileNumber(),
                newOtp
        );

        return "New mobile OTP sent successfully";
    }

    // =========================
    // USER - REQUEST ACCOUNT DELETION
    // =========================

    public void requestAccountDeletion(
            Long userId,
            Authentication authentication) {

        String loggedInEmail = authentication.getName();

        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found"));

        if (!loggedInUser.getUserId().equals(userId)) {
            throw new RuntimeException(
                    "You are not authorized to request deletion for this account");
        }

        if ("ADMIN".equals(loggedInUser.getRole())) {
            throw new RuntimeException(
                    "Admin accounts cannot request account deletion");
        }

        if (!"ACTIVE".equals(loggedInUser.getAccountStatus())) {
            throw new RuntimeException(
                    "Only active users can request account deletion");
        }

        if (loggedInUser.isDeletionRequested()) {
            throw new RuntimeException(
                    "Account deletion request is already pending");
        }

        loggedInUser.setDeletionRequested(true);
        loggedInUser.setDeletionRequestedAt(LocalDateTime.now());

        userRepository.save(loggedInUser);
    }

    // =========================
    // USER - GET DELETION REQUEST STATUS
    // =========================

    public Map<String, Object> getDeletionRequestStatus(
            Long userId,
            Authentication authentication) {

        String loggedInEmail = authentication.getName();

        User loggedInUser = userRepository.findByEmail(loggedInEmail)
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found"));

        if (!loggedInUser.getUserId().equals(userId)) {
            throw new RuntimeException(
                    "You are not authorized to view this account's deletion status");
        }

        return Map.of(
                "userId", loggedInUser.getUserId(),
                "deletionRequested", loggedInUser.isDeletionRequested(),
                "deletionRequestedAt",
                loggedInUser.getDeletionRequestedAt() == null
                        ? ""
                        : loggedInUser.getDeletionRequestedAt().toString()
        );
    }

    // =========================
    // ADMIN - GET DELETION REQUESTS
    // =========================

    public List<Map<String, Object>> getDeletionRequests() {

        return userRepository.findAll()
                .stream()
                .filter(User::isDeletionRequested)
                .map(user -> Map.<String, Object>of(
                        "userId", user.getUserId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "mobileNumber", user.getMobileNumber(),
                        "accountStatus", user.getAccountStatus(),
                        "deletionRequested", true,
                        "deletionRequestedAt",
                        user.getDeletionRequestedAt() == null
                                ? ""
                                : user.getDeletionRequestedAt().toString()
                ))
                .toList();
    }

    // =========================
    // ADMIN - REJECT ACCOUNT DELETION
    // =========================

    public void rejectAccountDeletion(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if ("ADMIN".equals(user.getRole())) {
            throw new RuntimeException(
                    "Admin accounts cannot have deletion requests");
        }

        if (!user.isDeletionRequested()) {
            throw new RuntimeException(
                    "No pending deletion request exists for this user");
        }

        user.setDeletionRequested(false);
        user.setDeletionRequestedAt(null);

        userRepository.save(user);
    }

    // =========================
    // ADMIN - APPROVE ACCOUNT DELETION
    // =========================

    public void approveAccountDeletion(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Admin accounts must never be deleted
        if ("ADMIN".equals(user.getRole())) {
            throw new RuntimeException(
                    "Admin accounts cannot be deleted");
        }

        // Approval is allowed only for a pending request
        if (!user.isDeletionRequested()) {
            throw new RuntimeException(
                    "No pending deletion request exists for this user");
        }

        // =====================================================
        // STEP 1 - SAFETY CHECK: ACTIVE BORROW RECORDS
        // =====================================================

        Boolean borrowSafe;

        try {
            borrowSafe = borrowClient.canDeleteUser(userId);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to verify borrow records. "
                    + "Account deletion was not performed.");
        }

        if (!Boolean.TRUE.equals(borrowSafe)) {
            throw new RuntimeException(
                    "Account cannot be permanently deleted because "
                    + "the user has an active or pending book borrowing.");
        }

        // =====================================================
        // STEP 2 - SAFETY CHECK: OUTSTANDING FINES
        // =====================================================

        Boolean fineSafe;

        try {
            fineSafe = fineClient.canDeleteUser(userId);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to verify fine records. "
                    + "Account deletion was not performed.");
        }

        if (!Boolean.TRUE.equals(fineSafe)) {
            throw new RuntimeException(
                    "Account cannot be permanently deleted because "
                    + "the user has an unpaid or pending-payment fine.");
        }

        // =====================================================
        // STEP 3 - DELETE BORROW HISTORY
        // =====================================================

        try {
            borrowClient.deleteUserBorrowHistory(userId);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Borrow history could not be deleted. "
                    + "Account deletion was not performed.");
        }

        // =====================================================
        // STEP 4 - DELETE FINE HISTORY
        // =====================================================

        try {
            fineClient.deleteUserFineHistory(userId);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Fine history could not be deleted. "
                    + "Account deletion was not completed. "
                    + "Please review the service logs.");
        }

        // =====================================================
        // STEP 5 - PERMANENTLY DELETE AUTH USER
        // =====================================================

        userRepository.delete(user);

        emailService.sendAccountDeletionEmail(
                user.getEmail(),
                user.getName()
        );
    }
 // =========================
 // ADMIN - DIRECT PERMANENT DELETE
 // =========================

 public void deleteUserDirectly(Long userId) {

     User user = userRepository.findById(userId)
             .orElseThrow(() ->
                     new RuntimeException("User not found"));

     // Admin accounts must never be deleted
     if ("ADMIN".equals(user.getRole())) {
         throw new RuntimeException(
                 "Admin accounts cannot be deleted");
     }

     // ==========================================
     // STEP 1 - CHECK ACTIVE BORROW
     // ==========================================

     Boolean borrowSafe;

     try {
         borrowSafe = borrowClient.canDeleteUser(userId);
     } catch (Exception e) {
         throw new RuntimeException(
                 "Unable to verify borrow records. "
                 + "Account deletion was not performed.");
     }

     if (!Boolean.TRUE.equals(borrowSafe)) {
         throw new RuntimeException(
                 "Account cannot be permanently deleted because "
                 + "the user has an active or pending book borrowing.");
     }

     // ==========================================
     // STEP 2 - CHECK OUTSTANDING FINE
     // ==========================================

     Boolean fineSafe;

     try {
         fineSafe = fineClient.canDeleteUser(userId);
     } catch (Exception e) {
         throw new RuntimeException(
                 "Unable to verify fine records. "
                 + "Account deletion was not performed.");
     }

     if (!Boolean.TRUE.equals(fineSafe)) {
         throw new RuntimeException(
                 "Account cannot be permanently deleted because "
                 + "the user has an unpaid or pending-payment fine.");
     }

     // ==========================================
     // STEP 3 - DELETE BORROW HISTORY
     // ==========================================

     try {
         borrowClient.deleteUserBorrowHistory(userId);
     } catch (Exception e) {
         throw new RuntimeException(
                 "Borrow history could not be deleted. "
                 + "Account deletion was not performed.");
     }

     // ==========================================
     // STEP 4 - DELETE FINE HISTORY
     // ==========================================

     try {
         fineClient.deleteUserFineHistory(userId);
     } catch (Exception e) {
         throw new RuntimeException(
                 "Fine history could not be deleted. "
                 + "Account deletion was not completed.");
     }

     // ==========================================
     // STEP 5 - PERMANENTLY DELETE USER
     // ==========================================

     userRepository.delete(user);

     emailService.sendAccountDeletionEmail(
             user.getEmail(),
             user.getName()
     );
 }

    // =========================
    // ADMIN - SUSPEND USER
    // =========================

    public void suspendUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Do not suspend an ADMIN account
        if ("ADMIN".equals(user.getRole())) {
            throw new RuntimeException(
                    "Admin accounts cannot be suspended");
        }

        // Already suspended
        if ("SUSPENDED".equals(user.getAccountStatus())) {
            throw new RuntimeException(
                    "User account is already suspended");
        }

        user.setAccountStatus("SUSPENDED");

        userRepository.save(user);

        // Send suspension notification email
        try {
            emailService.sendAccountSuspensionEmail(
                    user.getEmail(),
                    user.getName()
            );
        } catch (Exception emailException) {

            System.out.println(
                    "Suspension email failed: "
                    + emailException.getMessage()
            );
        }
    }


    // =========================
    // ADMIN - REACTIVATE USER
    // =========================

    public void reactivateUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Do not reactivate an ADMIN through this endpoint
        if ("ADMIN".equals(user.getRole())) {
            throw new RuntimeException(
                    "Admin accounts cannot be reactivated through this endpoint");
        }

        if ("ACTIVE".equals(user.getAccountStatus())) {
            throw new RuntimeException(
                    "User account is already active");
        }

        user.setAccountStatus("ACTIVE");

        userRepository.save(user);
    }
    public void deactivateUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Admin accounts cannot be deactivated
        if ("ADMIN".equals(user.getRole())) {
            throw new RuntimeException(
                    "Admin accounts cannot be deactivated"
            );
        }

        // Prevent unnecessary duplicate operation
        if ("DEACTIVATED".equals(user.getAccountStatus())) {
            throw new RuntimeException(
                    "User account is already deactivated"
            );
        }

        user.setAccountStatus("DEACTIVATED");

        userRepository.save(user);
    }
 // =========================
 // INTERNAL - USER STATUS
 // =========================

 public Map<String, Object> getUserStatus(Long userId) {

     User user = userRepository.findById(userId)
             .orElseThrow(() -> new RuntimeException("User not found"));

     return Map.of(
             "userId", user.getUserId(),
             "email", user.getEmail(),
             "accountStatus", user.getAccountStatus()
     );
 }
}
