package com.klu.lms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public NotificationService(JavaMailSender mailSender, @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendNotification(String to, String subject, String message) {
        try {
            sendMail(to, subject, message);
        } catch (Exception ex) {
            LOGGER.warn("Mail not sent to {}. Reason: {}", to, ex.getMessage());
        }
    }

    public void sendRequiredNotification(String to, String subject, String message) {
        try {
            sendMail(to, subject, message);
        } catch (Exception ex) {
            LOGGER.error("Required mail not sent to {}. Reason: {}", to, ex.getMessage());
            throw new IllegalStateException("Unable to send OTP email. Please check mail configuration and try again.");
        }
    }

    private void sendMail(String to, String subject, String message) {
        if (fromAddress == null || fromAddress.isBlank() || "your_email@gmail.com".equalsIgnoreCase(fromAddress.trim())) {
            throw new IllegalStateException("Mail sender is not configured");
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setText(message);
        mailSender.send(mail);
    }
}
