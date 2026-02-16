package com.example.gestionpointage.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gestionpointage.entity.UserDevice;
import com.example.gestionpointage.model.Utilisateur;

@Repository
public interface UserDeviceRepository 
        extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserAndDeviceHash(
            Utilisateur user, 
            String deviceHash
    );
}
