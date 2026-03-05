package com.example.gestionpointage.repository;

import com.example.gestionpointage.entity.RefreshToken;
import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // ── Login: حذف كل توكنز الجهاز السابقة ──
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user = :user AND t.userAgent = :userAgent")
    void deleteAllByUserAndUserAgent(
            @Param("user") Utilisateur user,
            @Param("userAgent") String userAgent
    );

    // ── Logout All: حذف كل توكنز المستخدم ──
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user = :user")
    void deleteAllByUser(@Param("user") Utilisateur user);

    // ── Admin: حذف كل توكنز الموظفين ──
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user.role = :role")
    void deleteAllByRole(@Param("role") Role role);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user.id IN :userIds AND t.user.role = :role")
    void deleteAllByUserIdsAndRole(
            @Param("userIds") List<Long> userIds,
            @Param("role") Role role
    );

    // ── Cleanup: حذف المنتهية فقط ──
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}