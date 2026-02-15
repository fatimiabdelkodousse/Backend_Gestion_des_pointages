package com.example.gestionpointage.listener;

import com.example.gestionpointage.event.AccountActivationEvent;
import com.example.gestionpointage.service.EmailService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class AccountActivationListener {

    private final EmailService emailService;
    
    public AccountActivationListener(
            EmailService emailService
    ) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleActivation(AccountActivationEvent event) {

        emailService.sendActivationLinkEmail(
                event.email(),
                event.prenom(),
                event.nom(),
                event.activationLink()
        );
    }
}
