package com.example.gestionpointage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private void send(String to, String subject, String body) {
        try {
            log.info("📧 Sending email to: {}", to);
            log.info("📧 Subject: {}", subject);
            log.info("📧 From: {}", fromEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);  // ← مهم جدًا!
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("✅ Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send email to: {} - Error: {}", to, e.getMessage(), e);
        }
    }

    @Async("taskExecutor")
    public void sendActivationLinkEmail(
            String to, String prenom, String nom, String link) {
        String body = "Bonjour " + prenom + " " + nom + ",\n\n"
                + "Veuillez activer votre compte en cliquant sur le lien suivant :\n"
                + link + "\n\n"
                + "Ce lien expire dans 24 heures.";
        send(to, "Activation de votre compte", body);
    }

    @Async("taskExecutor")
    public void sendResetLinkEmail(
            String to, String prenom, String nom, String link) {
        String body = "Bonjour " + prenom + " " + nom + ",\n\n"
                + "Cliquez sur le lien suivant pour définir un nouveau mot de passe :\n"
                + link + "\n\n"
                + "Ce lien expire dans 5 minutes.";
        send(to, "Réinitialisation du mot de passe", body);
    }

    @Async("taskExecutor")
    public void sendSuspiciousLoginAlert(
            String email, String ip, String userAgent) {
        String subject = "⚠ Suspicious login detected";
        String resetLink = "https://gestion-pointage.up.railway.app/reset-password";
        String body = """
            A new login was detected on your account.

            IP Address: %s
            Device: %s

            If this was not you,
            you can reset your password here:

            %s
            """.formatted(ip, userAgent, resetLink);
        send(email, subject, body);
    }
}