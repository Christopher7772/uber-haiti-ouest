package ht.uber.ouest.admin;

import ht.uber.ouest.rides.Ride;
import ht.uber.ouest.rides.RideRepository;
import ht.uber.ouest.users.User;
import ht.uber.ouest.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service PRO pour l'administration.
 * Gère les statistiques, les courses et la modération des utilisateurs.
 */
@Service
public class AdminService {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    // --- PARTIE STATISTIQUES ET COURSES ---

    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalUsers = userRepository.count();
        long totalRides = rideRepository.count();
        
        List<Ride> paidRides = rideRepository.findByPaymentStatus("PAYÉ");
        double totalRevenue = paidRides.stream().mapToDouble(Ride::getFare).sum();

        stats.put("totalUsers", totalUsers);
        stats.put("totalRides", totalRides);
        stats.put("totalRevenueHTG", totalRevenue);
        stats.put("successfulRides", paidRides.size());

        return stats;
    }

    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }

    // --- NOUVELLE PARTIE : GESTION DES UTILISATEURS ---

    /**
     * Récupère la liste de tous les utilisateurs (Passagers et Chauffeurs).
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Active ou suspend un utilisateur.
     * Si l'utilisateur est actif, il devient suspendu, et inversement.
     * @param userId L'ID de l'utilisateur à modifier.
     * @return L'utilisateur mis à jour.
     */
    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID: " + userId));
        
        // Inverse le statut actuel (true devient false, false devient true)
        user.setActive(!user.isActive()); 
        
        return userRepository.save(user);
    }

    /**
     * Valide les documents/le profil d'un chauffeur pour l'autoriser à travailler.
     * @param driverId L'ID du chauffeur.
     * @return L'utilisateur mis à jour.
     */
    public User validateDriver(Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Chauffeur introuvable avec l'ID: " + driverId));
        
        // Sécurité : On vérifie que c'est bien un profil chauffeur (selon ta logique de rôle)
        // Remplace : if ("PASSENGER".equalsIgnoreCase(driver.getRole()))
       if (driver.getRole() != null && "PASSENGER".equalsIgnoreCase(driver.getRole().name())) {
    throw new RuntimeException("Impossible de valider un passager comme chauffeur.");
}

        driver.setDriverValidated(true);
        return userRepository.save(driver);
    }
}