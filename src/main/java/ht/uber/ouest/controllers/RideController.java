package ht.uber.ouest.controllers;

import ht.uber.ouest.dto.RideRequest;
import ht.uber.ouest.dto.RideDTO;
import ht.uber.ouest.rides.Ride;
import ht.uber.ouest.rides.RideRepository; 
import ht.uber.ouest.rides.RideService;
import ht.uber.ouest.users.User;
import ht.uber.ouest.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List; 
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    @Autowired
    private RideService rideService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * --- MÉTHODE PRIVÉE PRO : CONVERTIR ENTITÉ -> DTO ---
     */
    private RideDTO convertToDTO(Ride ride) {
        return new RideDTO(
            ride.getId(),
            ride.getPickupLocation(),
            ride.getDestination(),
            ride.getFare(),
            ride.getStatus(),
            ride.getPaymentStatus(),
            ride.getPassenger().getUsername(),
            (ride.getChauffeur() != null) ? ride.getChauffeur().getUsername() : "En attente...",
            ride.getStartTime()
        );
    }
     
    @PostMapping("/request")
    public ResponseEntity<?> createRide(@RequestBody RideRequest request) {
        try {
            // 1. Récupérer l'utilisateur connecté via le Token
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // 2. Chercher l'utilisateur complet en base
            User passenger = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // 3. Créer la course (Sécurisé contre les valeurs nulles GPS)
            Ride ride = rideService.requestRide(
                    passenger, 
                    request.getPickupLocation(), 
                    request.getDestination(),
                    request.getPickupLatitude(),
                    request.getPickupLongitude(),
                    request.getDestinationLatitude(),
                    request.getDestinationLongitude()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Course demandée avec succès");
            response.put("ride", convertToDTO(ride));
            response.put("estimation", ride.getFare() + " HTG");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log de l'erreur pour débugger dans NetBeans
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erreur lors de la commande : " + e.getMessage());
        }
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<RideDTO>> getMyHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Ride> history = rideRepository.findByPassengerUsername(username);
        List<RideDTO> dtos = history.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<RideDTO>> getAvailableRides() {
        List<RideDTO> dtos = rideService.getAvailableRides().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideDTO> getRideDetails(@PathVariable Long id) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course non trouvée"));
        return ResponseEntity.ok(convertToDTO(ride));
    }
    
    @PostMapping("/{rideId}/accept")
    public ResponseEntity<?> acceptRide(@PathVariable Long rideId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User chauffeur = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Chauffeur non trouvé"));

        try {
            Ride ride = rideService.acceptRide(rideId, chauffeur);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Course acceptée ! En route vers le client.");
            response.put("ride", convertToDTO(ride));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    @PostMapping("/{rideId}/complete")
    public ResponseEntity<?> completeRide(@PathVariable Long rideId) {
        try {
            Ride ride = rideService.completeRide(rideId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Course terminée avec succès !");
            response.put("facture", convertToDTO(ride));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }
}