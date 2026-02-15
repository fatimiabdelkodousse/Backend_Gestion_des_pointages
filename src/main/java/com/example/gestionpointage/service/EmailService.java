package com.example.gestionpointage.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void sendActivationLinkEmail(
            String to,
            String prenom,
            String nom,
            String link
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Activation de votre compte");
        message.setText(
                "Bonjour " + prenom + " " + nom + ",\n\n" +
                "Veuillez activer votre compte en cliquant sur le lien suivant :\n" +
                link + "\n\n" +
                "Ce lien expire dans 24 heures."
        );
        mailSender.send(message);
    }
    
    public void sendResetLinkEmail(
            String to,
            String prenom,
            String nom,
            String link
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Réinitialisation du mot de passe");
        message.setText(
                "Bonjour " + prenom + " " + nom + ",\n\n" +
                "Cliquez sur le lien suivant pour définir un nouveau mot de passe :\n" +
                link + "\n\n" +
                "Ce lien expire dans 5 minutes."
        );
        mailSender.send(message);
    }
}
