package ht.uber.ouest.controllers;

import ht.uber.ouest.auth.AuthService;
import ht.uber.ouest.dto.LoginRequest;
import ht.uber.ouest.dto.RegisterRequest;
import ht.uber.ouest.users.User; // Import de l'entité User
import ht.uber.ouest.users.UserRepository; // Import du Repository
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion de l'authentification.
 * Gère les flux d'inscription et de connexion pour l'application Uber Ouest.
 */


//AuthController : Gère uniquement les jetons, la connexion et l'inscription.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository; // Injection pour récupérer le rôle

    /**
     * Endpoint pour l'inscription d'un nouvel utilisateur.
     * @param registerRequest DTO contenant les informations d'inscription.
     * @return ResponseEntity avec le jeton JWT ou un message d'erreur.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        
        // --- AJOUT PRO : VALIDATION DE LA CORRESPONDANCE DES MOTS DE PASSE ---
        // Cette étape garantit l'intégrité des données avant de solliciter la base de données.
        if (!registerRequest.passwordsMatch()) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "Les mots de passe ne correspondent pas."));
        }
        // ---------------------------------------------------------------------

        try {
            // Appel au service pour la logique métier et la persistence
            String token = authService.registerUser(registerRequest);
            return buildTokenResponse(token, "Inscription réussie", registerRequest.getRole().toString());
            
        } catch (RuntimeException e) {
            // Gestion des erreurs métiers (ex: utilisateur déjà existant)
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Endpoint pour l'authentification (Login).
     * @param loginRequest DTO contenant les identifiants.
     * @return ResponseEntity avec le jeton JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
            
            // --- AJOUT PRO : RÉCUPÉRATION DU RÔLE ---
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            return buildTokenResponse(token, "Connexion réussie", user.getRole().toString());
        } catch (RuntimeException e) {
            // Erreur 401 Unauthorized pour les identifiants incorrects
            return ResponseEntity.status(401).body(Map.of("erreur", "Identifiants invalides"));
        }
    }

    /**
     * Méthode utilitaire pour formater la réponse JSON de manière uniforme (Standard International).
     * @param token Le jeton JWT généré.
     * @param message Message de succès.
     * @param role Le rôle de l'utilisateur (ex: CHAUFFEUR, PASSAGER).
     * @return Une Map structurée pour le client.
     */
    private ResponseEntity<Map<String, String>> buildTokenResponse(String token, String message, String role) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put("token", token);
        response.put("role", role); // Indispensable pour la redirection frontend
        response.put("type", "Bearer"); // Standard de porteur pour JWT
        return ResponseEntity.ok(response);
    }
}