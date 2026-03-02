package com.example.gestionpointage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionPointageBackendApplication {

    public static void main(String[] args) {
    	System.setProperty("java.awt.headless", "true");
        SpringApplication.run(GestionPointageBackendApplication.class, args);
    }
}
