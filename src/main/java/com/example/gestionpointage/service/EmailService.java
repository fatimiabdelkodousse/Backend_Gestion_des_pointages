package com.example.gestionpointage.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    // ══════════════════════════════════════
    // 📧 الإرسال عبر SendGrid API (ليس SMTP)
    // ══════════════════════════════════════
    private void send(String to, String subject, String body) {
        try {
            log.info("📧 Sending email to: {}", to);
            log.info("📧 Subject: {}", subject);
            log.info("📧 From: {}", fromEmail);

            Email from = new Email(fromEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200
                    && response.getStatusCode() < 300) {

                log.info("✅ Email sent successfully to: {} (status: {})",
                        to, response.getStatusCode());
            } else {

                log.error("❌ SendGrid error: status={}, body={}",
                        response.getStatusCode(),
                        response.getBody());
            }

        } catch (IOException e) {
            log.error("❌ Failed to send email to: {} - Error: {}",
                    to, e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════
    // 🔗 Activation Link
    // ══════════════════════════════════════
    @Async("taskExecutor")
    public void sendActivationLinkEmail(
            String to, String prenom, String nom, String link) {

        String body = "Bonjour " + prenom + " " + nom + ",\n\n"
                + "Veuillez activer votre compte en cliquant"
                + " sur le lien suivant :\n"
                + link + "\n\n"
                + "Ce lien expire dans 24 heures.";

        send(to, "Activation de votre compte", body);
    }

    // ══════════════════════════════════════
    // 🔑 Reset Password Link
    // ══════════════════════════════════════
    @Async("taskExecutor")
    public void sendResetLinkEmail(
            String to, String prenom, String nom, String link) {

        String body = "Bonjour " + prenom + " " + nom + ",\n\n"
                + "Cliquez sur le lien suivant pour définir"
                + " un nouveau mot de passe :\n"
                + link + "\n\n"
                + "Ce lien expire dans 5 minutes.";

        send(to, "Réinitialisation du mot de passe", body);
    }

    // ══════════════════════════════════════
    // ⚠ Suspicious Login Alert
    // ══════════════════════════════════════
    @Async("taskExecutor")
    public void sendSuspiciousLoginAlert(
            String email, String ip, String userAgent) {

        String subject = "⚠ Suspicious login detected";
        String resetLink =
                "https://gestion-pointage.up.railway.app/reset-password";

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