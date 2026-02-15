package com.example.gestionpointage.repository;

import com.example.gestionpointage.entity.RefreshToken;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user = :user")
    void revokeAllByUser(@Param("user") Utilisateur user);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.role = :role")
    void revokeAllByRole(@Param("role") Role role);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id IN :userIds AND t.user.role = :role")
    void revokeAllByUserIdsAndRole(@Param("userIds") List<Long> userIds, @Param("role") Role role);
}