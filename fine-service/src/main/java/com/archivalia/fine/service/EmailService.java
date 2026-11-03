package com.archivalia.fine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ---------------------------------------------------------
    // 1. EMAIL TO ADMIN WHEN USER SUBMITS PAYMENT
    // ---------------------------------------------------------
    public void sendPaymentSubmittedEmail(
            Long fineId,
            Long borrowId,
            Long userId,
            Double fineAmount) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(adminEmail);

        message.setSubject(
                "Archivalia - Fine Payment Pending Approval"
        );

        message.setText(
                "Dear Administrator,\n\n"

                + "A user has submitted a fine payment "
                + "for admin verification.\n\n"

                + "Payment Details:\n"
                + "Fine ID: " + fineId + "\n"
                + "Borrow ID: " + borrowId + "\n"
                + "User ID: " + userId + "\n"
                + "Fine Amount: ₹" + fineAmount + "\n"
                + "Payment Status: PENDING_APPROVAL\n\n"

                + "Please verify the payment in the "
                + "Archivalia admin dashboard and approve "
                + "or reject it accordingly.\n\n"

                + "Regards,\n"
                + "Archivalia Digital E-Library"
        );

        mailSender.send(message);
    }


    // ---------------------------------------------------------
    // 2. EMAIL TO USER WHEN ADMIN APPROVES PAYMENT
    // ---------------------------------------------------------
    public void sendPaymentApprovedEmail(
            String userEmail,
            String userName,
            Long fineId,
            Double fineAmount) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(userEmail);

        message.setSubject(
                "Archivalia - Fine Payment Approved"
        );

        message.setText(
                "Dear " + userName + ",\n\n"

                + "Your fine payment has been successfully "
                + "verified and approved by the Archivalia administrator.\n\n"

                + "Payment Details:\n"
                + "Fine ID: " + fineId + "\n"
                + "Fine Amount: ₹" + fineAmount + "\n"
                + "Payment Status: APPROVED\n"
                + "Fine Status: PAID\n\n"

                + "Your fine has been marked as PAID. "
                + "If this fine was blocking a book return, "
                + "you can now proceed with the return.\n\n"

                + "Thank you for using Archivalia Digital E-Library.\n\n"

                + "Regards,\n"
                + "Archivalia Digital E-Library"
        );

        mailSender.send(message);
    }
 // ---------------------------------------------------------
 // 3. EMAIL TO USER WHEN ADMIN REJECTS PAYMENT
 // ---------------------------------------------------------
 public void sendPaymentRejectedEmail(
         String userEmail,
         String userName,
         Long fineId,
         Double fineAmount) {

     SimpleMailMessage message = new SimpleMailMessage();

     message.setTo(userEmail);

     message.setSubject(
             "Archivalia - Fine Payment Rejected"
     );

     message.setText(
             "Dear " + userName + ",\n\n"

             + "Your fine payment submitted through "
             + "Archivalia Digital E-Library could not be "
             + "verified by the administrator.\n\n"

             + "Payment Details:\n"
             + "Fine ID: " + fineId + "\n"
             + "Fine Amount: ₹" + fineAmount + "\n"
             + "Payment Status: REJECTED\n"
             + "Fine Status: UNPAID\n\n"

             + "Please verify your payment details and "
             + "submit the payment again if you have made "
             + "the payment successfully.\n\n"

             + "Your fine remains unpaid until the payment "
             + "is verified and approved by the administrator.\n\n"

             + "Thank you for using Archivalia Digital E-Library.\n\n"

             + "Regards,\n"
             + "Archivalia Digital E-Library"
     );

     mailSender.send(message);
 }
}