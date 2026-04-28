package ht.uber.ouest.dto;

import ht.uber.ouest.users.Role;

/**
 * Data Transfer Object pour les requêtes d'inscription.
 * Cette classe transporte les données du client vers le serveur Uber Ouest.
 */
public class RegisterRequest {
    private String username;
    private String password;
    private String confirmPassword;
    private Role role;

    // Constructeur par défaut nécessaire pour la désérialisation JSON (Jackson)
    public RegisterRequest() {}

    /**
     * Méthode de validation métier de haut niveau.
     * Vérifie si le mot de passe et sa confirmation sont identiques.
     * @return true si les mots de passe correspondent, false sinon.
     */
    public boolean passwordsMatch() {
        // --- LOGIQUE PRO : Éviter le NullPointerException avant la comparaison ---
        return this.password != null && this.password.equals(this.confirmPassword);
    }

    // Getters et Setters standard
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}