package ht.uber.ouest.auth;

import ht.uber.ouest.users.User;
import ht.uber.ouest.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ht.uber.ouest.dto.RegisterRequest;
import ht.uber.ouest.users.Role;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // AMÉLIORATION PRO : Utilisation de l'AuthenticationManager de Spring
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Authentifie un utilisateur et génère un Token JWT contenant ses rôles.
     */
    public String authenticateUser(String username, String password) {
        // --- LOG DE DEBUG (A supprimer en production) ---
        System.out.println("DEBUG : Tentative de connexion via Spring Security : [" + username + "]");

        // AMÉLIORATION PRO : On laisse Spring Security gérer la vérification (Manager -> Provider -> BCrypt)
        // Note : Si isActive est false, cette méthode lancera une DisabledException (401)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // On injecte l'authentification dans le contexte global
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // On génère le token en passant l'objet authentication complet (pour extraire les rôles)
        System.out.println("DEBUG : Authentification réussie. Génération du token PRO...");
        return jwtUtils.generateToken(authentication);
    }
    
    /**
     * Enregistre un nouvel utilisateur et retourne son premier Token.
     */
    public String registerUser(RegisterRequest request) {
        // 1. Vérifier si l'utilisateur existe déjà
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Erreur : Ce nom d'utilisateur est déjà pris.");
        }

        // 2. Créer le nouvel utilisateur
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        
        // Hachage du mot de passe pour la sécurité (BCrypt)
        newUser.setPassword(passwordEncoder.encode(request.getPassword())); 
        
        // Assigner un rôle (Passager par défaut si non spécifié)
        newUser.setRole(request.getRole() != null ? request.getRole() : Role.PASSAGER);

        // --- CORRECTION : S'assurer que le compte est actif dès la création ---
        newUser.setActive(true); 
        
        // Si c'est un chauffeur, on peut décider s'il est validé par défaut ou non
        if (newUser.getRole() == Role.CHAUFFEUR) {
            newUser.setDriverValidated(false); // Doit être validé par un admin
        }

        // 3. Sauvegarder en base de données
        userRepository.save(newUser);
        
        // AMÉLIORATION PRO : Pour générer un token après inscription, 
        // on simule une authentification pour obtenir l'objet Authentication requis.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                newUser.getUsername(), 
                null, 
                newUser.getAuthorities() // Utilise les autorités ROLE_XXX de l'entité
        );

        return jwtUtils.generateToken(authentication);
    }
}