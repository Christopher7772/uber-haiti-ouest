package ht.uber.ouest.users;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime dateCreation;

    // --- NOUVEAUX CHAMPS POUR L'ADMINISTRATION ---
    
    @Column(nullable = false)
    private boolean isActive = true; // Pour suspendre/activer un compte




@Column(name = "is_active", columnDefinition = "boolean default true")

private boolean isActive = true;

@Column(name = "is_driver_validated", columnDefinition = "boolean default true")

private boolean isDriverValidated = true;
    
    

    @Column(nullable = false)
    private boolean isDriverValidated = false; // Pour valider les documents des chauffeurs

    // Constructeur vide (Obligatoire pour JPA)
    public User() {
        this.dateCreation = LocalDateTime.now();
    }

    // --- MÉTHODES USERDETAILS (SÉCURITÉ) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Retourne le rôle standard (ex: ROLE_ADMIN, ROLE_DRIVER, ROLE_PASSENGER)
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * LIEN SÉCURITÉ : Si isActive est false, Spring Security refusera la connexion.
     */
    @Override
    public boolean isEnabled() { 
        return this.isActive; 
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    // --- GETTERS ET SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @Override
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    // Getters et Setters pour la gestion Admin
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isDriverValidated() { return isDriverValidated; }
    public void setDriverValidated(boolean driverValidated) { isDriverValidated = driverValidated; }
    
}
