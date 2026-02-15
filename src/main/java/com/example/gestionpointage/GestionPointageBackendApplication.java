package com.example.gestionpointage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionPointageBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionPointageBackendApplication.class, args);
    }
}
