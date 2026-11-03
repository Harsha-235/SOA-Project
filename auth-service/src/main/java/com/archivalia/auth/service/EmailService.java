package com.archivalia.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // =========================
    // EMAIL VERIFICATION OTP
    // =========================

    public void sendEmailOtp(String to, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);

        message.setSubject(
                "Archivalia - Email Verification OTP"
        );

        message.setText(
                "Hello,\n\n" +
                "Your Archivalia email verification OTP is:\n\n" +
                otp + "\n\n" +
                "This OTP is valid for 5 minutes.\n\n" +
                "If you did not create an Archivalia account, " +
                "please ignore this email.\n\n" +
                "Regards,\n" +
                "Archivalia Digital E-Library"
        );

        mailSender.send(message);
    }


    // =========================
    // LOGIN NOTIFICATION
    // =========================

    public void sendLoginNotification(String to, String name) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);

        message.setSubject(
                "Archivalia - Login Successful"
        );

        message.setText(
                "Hello " + name + ",\n\n" +
                "You have successfully logged in to your " +
                "Archivalia Digital E-Library account.\n\n" +
                "If you did not perform this login, please contact " +
                "the Archivalia administrator immediately.\n\n" +
                "Regards,\n" +
                "Archivalia Digital E-Library"
        );

        mailSender.send(message);
    }


    // =========================
    // PASSWORD RESET OTP
    // =========================

    public void sendPasswordResetOtp(
            String to,
            String userName,
            String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);

        message.setSubject(
                "Archivalia - Password Reset OTP"
        );

        message.setText(
                "Hello " + userName + ",\n\n" +
                "We received a request to reset your " +
                "Archivalia Digital E-Library password.\n\n" +
                "Your password reset OTP is: " + otp + "\n\n" +
                "This OTP is valid for 5 minutes.\n\n" +
                "If you did not request a password reset, " +
                "please ignore this email.\n\n" +
                "Regards,\n" +
                "Archivalia Digital E-Library"
        );

        mailSender.send(message);
    }


    // =========================
    // ACCOUNT SUSPENSION
    // =========================

    public void sendAccountSuspensionEmail(
            String to,
            String userName) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);

        message.setSubject(
                "Archivalia - Account Suspended"
        );

        message.setText(
                "Hello " + userName + ",\n\n" +

                "Your Archivalia Digital E-Library account " +
                "has been suspended by the administrator.\n\n" +

                "Your account is currently unavailable for login " +
                "and other protected activities.\n\n" +

                "Your borrowing, fine, payment, and account history " +
                "have not been deleted.\n\n" +

                "If you believe this suspension was made in error " +
                "or you need further information, please contact " +
                "the Archivalia administrator.\n\n" +

                "Regards,\n" +
                "Archivalia Digital E-Library"
        );

        mailSender.send(message);
    }
 // =========================
 // ACCOUNT DELETION
 // =========================

 public void sendAccountDeletionEmail(
         String to,
         String userName) {

     SimpleMailMessage message = new SimpleMailMessage();

     message.setTo(to);

     message.setSubject(
             "Archivalia - Account Deleted"
     );

     message.setText(
             "Dear " + userName + ",\n\n" +

             "Your Archivalia Digital E-Library account " +
             "has been successfully deleted.\n\n" +

             "The account deletion has been processed by the " +
             "Archivalia administrator.\n\n" +

             "Thank you for using Archivalia Digital E-Library.\n\n" +

             "Regards,\n" +
             "Archivalia Digital E-Library"
     );

     mailSender.send(message);
 }
}