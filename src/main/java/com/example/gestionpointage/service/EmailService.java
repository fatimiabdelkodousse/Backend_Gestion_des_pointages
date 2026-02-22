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
            Email from = new Email(fromEmail);
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
            throw new RuntimeException("Failed to send email to: " + to, e);
        }
    }

    @Async("taskExecutor")
    public void sendActivationLinkEmail(
            String to, String prenom, String nom, String link) {

        String html = buildEmailTemplate(
                prenom + " " + nom,
                "Votre compte a été créé. Veuillez l'activer en cliquant sur le bouton ci-dessous.",
                link,
                "Activer mon compte",
                "Ce lien expire dans 24 heures."
        );

        send(to, "Activation de votre compte", html);
    }

    @Async("taskExecutor")
    public void sendResetLinkEmail(
            String to, String prenom, String nom, String link) {

        String html = buildEmailTemplate(
                prenom + " " + nom,
                "Vous avez demandé la réinitialisation de votre mot de passe. Cliquez sur le bouton ci-dessous pour continuer.",
                link,
                "Réinitialiser le mot de passe",
                "Ce lien expire dans 5 minutes."
        );

        send(to, "Réinitialisation du mot de passe", html);
    }

    @Async("taskExecutor")
    public void sendSuspiciousLoginAlert(
            String email, String ip, String userAgent) {

        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#F5F6FA;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 20px;">
                <tr><td align="center">
                  <table width="420" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:18px;box-shadow:0 2px 12px rgba(0,0,0,0.04);padding:36px 32px;">
                    <tr><td align="center" style="padding-bottom:24px;">
                      <div style="width:56px;height:56px;background:rgba(255,152,0,0.1);border-radius:50%%;display:inline-flex;align-items:center;justify-content:center;">
                        <span style="font-size:28px;">⚠️</span>
                      </div>
                    </td></tr>
                    <tr><td align="center" style="font-size:20px;font-weight:700;color:#1a1a2e;padding-bottom:16px;">
                      Connexion suspecte détectée
                    </td></tr>
                    <tr><td style="font-size:14px;color:#666;line-height:1.7;padding-bottom:20px;">
                      Une nouvelle connexion a été détectée sur votre compte :
                    </td></tr>
                    <tr><td style="background:#F5F6FA;border-radius:12px;padding:16px 20px;margin-bottom:20px;">
                      <table width="100%%">
                        <tr>
                          <td style="font-size:13px;color:#999;padding:4px 0;">Adresse IP</td>
                          <td style="font-size:13px;font-weight:600;color:#1a1a2e;text-align:right;">%s</td>
                        </tr>
                        <tr>
                          <td style="font-size:13px;color:#999;padding:4px 0;">Appareil</td>
                          <td style="font-size:13px;font-weight:600;color:#1a1a2e;text-align:right;">%s</td>
                        </tr>
                      </table>
                    </td></tr>
                    <tr><td style="font-size:13px;color:#999;padding-top:20px;line-height:1.6;">
                      Si ce n'était pas vous, veuillez changer votre mot de passe immédiatement depuis l'application.
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(ip, userAgent);

        send(email, "⚠ Connexion suspecte détectée", html);
    }

    private String buildEmailTemplate(
            String name,
            String message,
            String link,
            String buttonText,
            String footer
    ) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#F5F6FA;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 20px;">
                <tr><td align="center">
                  <table width="420" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:18px;box-shadow:0 2px 12px rgba(0,0,0,0.04);padding:36px 32px;">

                    <!-- Logo -->
                    <tr><td align="center" style="padding-bottom:28px;">
                      <div style="width:56px;height:56px;background:linear-gradient(135deg,#6C63FF,#9C55F7);border-radius:14px;display:inline-block;text-align:center;line-height:56px;">
                        <span style="font-size:28px;color:white;">🔒</span>
                      </div>
                    </td></tr>

                    <!-- Greeting -->
                    <tr><td style="font-size:15px;color:#1a1a2e;padding-bottom:8px;">
                      Bonjour <strong>%s</strong>,
                    </td></tr>

                    <!-- Message -->
                    <tr><td style="font-size:14px;color:#666;line-height:1.7;padding-bottom:28px;">
                      %s
                    </td></tr>

                    <!-- Button -->
                    <tr><td align="center" style="padding-bottom:28px;">
                      <a href="%s"
                         style="display:inline-block;
                                padding:14px 36px;
                                background:linear-gradient(135deg,#6C63FF,#9C55F7);
                                color:#ffffff;
                                text-decoration:none;
                                border-radius:14px;
                                font-size:15px;
                                font-weight:700;
                                box-shadow:0 4px 16px rgba(108,99,255,0.3);">
                        %s
                      </a>
                    </td></tr>

                    <!-- Footer -->
                    <tr><td style="border-top:1px solid #eee;padding-top:20px;font-size:12px;color:#999;line-height:1.6;">
                      %s<br>
                      Si vous n'avez pas fait cette demande, ignorez cet e-mail.
                    </td></tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, message, link, buttonText, footer);
    }
}