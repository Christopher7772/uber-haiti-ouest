package ht.uber.ouest.admin;

import ht.uber.ouest.rides.Ride;
import ht.uber.ouest.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API d'Administration PRO sécurisée.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Protection stricte
public class AdminController {

    @Autowired
    private AdminService adminService;

    // --- ENDPOINTS DASHBOARD ---

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getGlobalStats());
    }

    @GetMapping("/rides")
    public ResponseEntity<List<Ride>> getAllSystemRides() {
        return ResponseEntity.ok(adminService.getAllRides());
    }

    // --- NOUVEAUX ENDPOINTS : GESTION DES UTILISATEURS ---

    /**
     * Obtenir tous les utilisateurs.
     * URL: GET /api/admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    /**
     * Suspendre ou Activer un compte.
     * URL: PUT /api/admin/users/{id}/toggle-status
     */
    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        try {
            User updatedUser = adminService.toggleUserStatus(id);
            String statusMsg = updatedUser.isActive() ? "activé" : "suspendu";
            return ResponseEntity.ok(Map.of("message", "Le compte a été " + statusMsg + " avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Valider officiellement un chauffeur.
     * URL: PUT /api/admin/users/{id}/validate
     */
    @PutMapping("/users/{id}/validate")
    public ResponseEntity<?> validateDriver(@PathVariable Long id) {
        try {
            User validatedDriver = adminService.validateDriver(id);
            return ResponseEntity.ok(Map.of("message", "Chauffeur " + validatedDriver.getUsername() + " validé avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}