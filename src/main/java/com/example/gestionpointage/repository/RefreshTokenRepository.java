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

    // ── للـ rotate: البحث بدون فلترة revoked (لكشف إعادة الاستخدام) ──
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // ── للـ logout: فقط التوكنز النشطة ──
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    // ── حذف كل توكنز الجهاز نفسه عند Login جديد ──
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user = :user AND t.userAgent = :userAgent")
    void deleteAllByUserAndUserAgent(
            @Param("user") Utilisateur user,
            @Param("userAgent") String userAgent
    );

    // ── إلغاء كل توكنز المستخدم ──
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user = :user")
    void revokeAllByUser(@Param("user") Utilisateur user);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.role = :role")
    void revokeAllByRole(@Param("role") Role role);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id IN :userIds AND t.user.role = :role")
    void revokeAllByUserIdsAndRole(
            @Param("userIds") List<Long> userIds,
            @Param("role") Role role
    );

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.revoked = true")
    int deleteAllRevoked();

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}