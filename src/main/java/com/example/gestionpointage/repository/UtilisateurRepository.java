package com.example.gestionpointage.repository;

import org.springframework.data.domain.Page;



import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.gestionpointage.model.Utilisateur;
import com.example.gestionpointage.dto.UtilisateurBadgeDTO;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

	Optional<Utilisateur> findByEmail(String email);
	
	boolean existsByEmail(String email);
	
	List<Utilisateur> findBySiteId(Long siteId);
	
	Optional<Utilisateur> findByEmailAndNomAndPrenom(
	        String email,
	        String nom,
	        String prenom
	);
	@Query("""
			SELECT u FROM Utilisateur u
			LEFT JOIN u.badge b
			WHERE u.email = :email
			AND u.nom = :nom
			AND u.prenom = :prenom
			AND (:badgeUid IS NULL OR b.badgeUid = :badgeUid)
			""")
			Optional<Utilisateur> findForPasswordReset(
			    String email,
			    String nom,
			    String prenom,
			    String badgeUid
			);

    // 🔍 SEARCH
    Page<Utilisateur> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String nom,
            String prenom,
            String email,
            Pageable pageable
    );
    
    @Query("""
    	    SELECT u FROM Utilisateur u
    	    JOIN u.badge b
    	    WHERE LOWER(u.email) = LOWER(:email)
    	    AND LOWER(u.nom) = LOWER(:nom)
    	    AND LOWER(u.prenom) = LOWER(:prenom)
    	    AND b.badgeUid = :badgeUid
    	    AND u.active = true
    	""")
    	Optional<Utilisateur> findActiveUserForPasswordReset(
    	        String email,
    	        String nom,
    	        String prenom,
    	        String badgeUid
    	);

    @Query("""
    	    SELECT u FROM Utilisateur u
    	    JOIN u.badge b
    	    WHERE u.site.id = :siteId
    	    AND u.role = com.example.gestionpointage.model.Role.EMPLOYE
    	    AND u.deleted = false
    	    AND b.active = true
    	""")
    	List<Utilisateur> findActiveEmployeesBySite(Long siteId);
    
    @Query("""
    	    SELECT new com.example.gestionpointage.dto.UtilisateurBadgeDTO(
    	        u.id,
    	        u.nom,
    	        u.prenom,
    	        u.email,
    	        u.role,
    	        b.badgeUid,
    	        b.active,
    	        u.site.id
    	    )
    	    FROM Utilisateur u
    	    LEFT JOIN u.badge b
    	    WHERE u.role = com.example.gestionpointage.model.Role.EMPLOYE
    	    AND u.deleted = false
    	""")
    	List<UtilisateurBadgeDTO> findEmployeesWithBadges();
}

