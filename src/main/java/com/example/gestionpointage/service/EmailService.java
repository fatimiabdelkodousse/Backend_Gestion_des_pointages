package com.example.gestionpointage.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    private void send(String to, String subject, String htmlBody) {
        try {
            Email from = new Email(fromEmail, "Gestion Pointage");
            Email toEmail = new Email(to);
            Content content = new Content("text/html", htmlBody);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            sg.api(request);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("taskExecutor")
    public void sendResetLinkEmail(
            String to, String prenom, String nom, String link) {

        String html = buildActionEmail(
                prenom + " " + nom,
                "Vous avez demandé la réinitialisation de votre mot de passe. Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe.",
                link,
                "Réinitialiser le mot de passe",
                "#6C63FF",
                "Ce lien expire dans 5 minutes."
        );

        send(to, "Réinitialisation du mot de passe", html);
    }

    @Async("taskExecutor")
    public void sendSuspiciousLoginAlert(
            String email, String ip, String userAgent) {

        String html = buildAlertEmail(ip, userAgent);
        send(email, "Connexion suspecte détectée", html);
    }

    private String buildActionEmail(
            String name,
            String message,
            String link,
            String buttonText,
            String buttonColor,
            String footer
    ) {
        return "<!DOCTYPE html>"
                + "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>"
                + "<body style=\"margin:0;padding:0;background-color:#f4f5f9;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding:40px 16px;\">"
                + "<tr><td align=\"center\">"

                // Container
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:460px;background-color:#ffffff;border-radius:16px;overflow:hidden;\">"

                // Header bar
                + "<tr><td style=\"background-color:" + buttonColor + ";padding:24px 32px;text-align:center;\">"
                + "<span style=\"font-size:32px;\">&#128274;</span>"
                + "</td></tr>"

                // Body
                + "<tr><td style=\"padding:32px 28px 12px;\">"
                + "<p style=\"margin:0 0 6px;font-size:15px;color:#333333;\">Bonjour <strong>" + escapeHtml(name) + "</strong>,</p>"
                + "<p style=\"margin:0;font-size:14px;color:#666666;line-height:1.6;\">" + escapeHtml(message) + "</p>"
                + "</td></tr>"

                // Button
                + "<tr><td align=\"center\" style=\"padding:20px 28px 28px;\">"
                + "<a href=\"" + link + "\" style=\"display:inline-block;padding:14px 40px;"
                + "background-color:" + buttonColor + ";color:#ffffff;text-decoration:none;"
                + "border-radius:8px;font-size:15px;font-weight:bold;\">"
                + escapeHtml(buttonText)
                + "</a>"
                + "</td></tr>"

                // Footer
                + "<tr><td style=\"padding:0 28px 24px;\">"
                + "<hr style=\"border:none;border-top:1px solid #eeeeee;margin:0 0 16px;\">"
                + "<p style=\"margin:0;font-size:12px;color:#999999;line-height:1.5;\">"
                + escapeHtml(footer) + "<br>"
                + "Si vous n'avez pas fait cette demande, ignorez cet e-mail."
                + "</p>"
                + "</td></tr>"

                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private String buildAlertEmail(String ip, String userAgent) {

        return "<!DOCTYPE html>"
                + "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>"
                + "<body style=\"margin:0;padding:0;background-color:#f4f5f9;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding:40px 16px;\">"
                + "<tr><td align=\"center\">"

                // Container
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:460px;background-color:#ffffff;border-radius:16px;overflow:hidden;\">"

                // Header bar (orange)
                + "<tr><td style=\"background-color:#FF9800;padding:24px 32px;text-align:center;\">"
                + "<span style=\"font-size:32px;\">&#9888;&#65039;</span>"
                + "</td></tr>"

                // Title
                + "<tr><td style=\"padding:28px 28px 8px;text-align:center;\">"
                + "<p style=\"margin:0;font-size:18px;font-weight:bold;color:#1a1a2e;\">Connexion suspecte d&eacute;tect&eacute;e</p>"
                + "</td></tr>"

                // Message
                + "<tr><td style=\"padding:8px 28px 20px;\">"
                + "<p style=\"margin:0;font-size:14px;color:#666666;line-height:1.6;\">"
                + "Une nouvelle connexion a &eacute;t&eacute; d&eacute;tect&eacute;e sur votre compte :"
                + "</p>"
                + "</td></tr>"

                // Info table
                + "<tr><td style=\"padding:0 28px 24px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f8f8fb;border-radius:10px;\">"

                // IP row
                + "<tr>"
                + "<td style=\"padding:12px 16px;font-size:13px;color:#999999;border-bottom:1px solid #eeeeee;\">Adresse IP</td>"
                + "<td style=\"padding:12px 16px;font-size:13px;font-weight:bold;color:#1a1a2e;text-align:right;border-bottom:1px solid #eeeeee;\">"
                + escapeHtml(ip)
                + "</td>"
                + "</tr>"

                // Device row
                + "<tr>"
                + "<td style=\"padding:12px 16px;font-size:13px;color:#999999;\">Appareil</td>"
                + "<td style=\"padding:12px 16px;font-size:13px;font-weight:bold;color:#1a1a2e;text-align:right;\">"
                + escapeHtml(userAgent)
                + "</td>"
                + "</tr>"

                + "</table>"
                + "</td></tr>"

                // Warning footer
                + "<tr><td style=\"padding:0 28px 28px;\">"
                + "<hr style=\"border:none;border-top:1px solid #eeeeee;margin:0 0 16px;\">"
                + "<p style=\"margin:0;font-size:12px;color:#999999;line-height:1.5;\">"
                + "Si ce n'&eacute;tait pas vous, veuillez changer votre mot de passe imm&eacute;diatement depuis l'application."
                + "</p>"
                + "</td></tr>"

                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}