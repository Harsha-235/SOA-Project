package com.archivalia.borrow.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendBorrowConfirmation(
            String to,
            String userName,
            String bookTitle,
            String issueDate,
            String dueDate) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(
                "Archivalia - Book Borrowing Confirmation");

        message.setText(
                "Hello " + userName + ",\n\n" +
                "Your book has been successfully borrowed from " +
                "Archivalia Digital E-Library.\n\n" +
                "Book: " + bookTitle + "\n" +
                "Issue Date: " + issueDate + "\n" +
                "Due Date: " + dueDate + "\n\n" +
                "Please return the book on or before the due date.\n\n" +
                "Regards,\n" +
                "Archivalia Digital E-Library"
        );

        mailSender.send(message);
    }

    public void sendReturnConfirmation(
            String to,
            String userName,
            String bookTitle,
            String returnDate,
            double fine) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(
                "Archivalia - Book Return Confirmation");

        message.setText(
                "Hello " + userName + ",\n\n" +
                "Your book has been successfully returned.\n\n" +
                "Book: " + bookTitle + "\n" +
                "Return Date: " + returnDate + "\n" +
                "Fine: ₹" + fine + "\n\n" +
                "Thank you for using Archivalia Digital E-Library.\n\n" +
                "Regards,\n" +
                "Archivalia Digital E-Library"
        );

        mailSender.send(message);
    }
    public void sendNewBorrowRequestEmail(
            String to,
            String adminName,
            String userName,
            String userEmail,
            String bookTitle,
            String requestDate) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(
                "Archivalia - New Book Borrow Request");

        message.setText(
                "Hello " + adminName + ",\n\n" +
                "A new book borrowing request has been submitted " +
                "in Archivalia Digital E-Library.\n\n" +
                "Student Name: " + userName + "\n" +
                "Student Email: " + userEmail + "\n" +
                "Book: " + bookTitle + "\n" +
                "Request Date: " + requestDate + "\n\n" +
                "Please log in to the Archivalia Admin Dashboard " +
                "to review and approve or reject this request.\n\n" +
                "Regards,\n" +
                "Archivalia Digital E-Library"
        );

        mailSender.send(message);
    }
}