package ht.uber.ouest.rides;

import ht.uber.ouest.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.security.core.context.SecurityContextHolder; // AJOUT PRO : Pour la sécurité
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Map;
import java.util.HashMap; 

@Service
@Transactional 
public class RideService {

    @Autowired
    private RideRepository rideRepository;

    @Autowired 
    private SimpMessagingTemplate messagingTemplate; 

    /**
     * --- MÉTHODE PRO : CALCUL GÉOGRAPHIQUE (HAVERSINE) ---
     * Cette formule calcule la distance réelle en kilomètres entre deux points sur la Terre.
     */
    private Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        // PROTECTION PRO : Si une coordonnée est nulle, on retourne une distance par défaut (ex: 5km)
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 5.0; 
        }
        
        double R = 6371; // Rayon de la Terre en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
                   
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Retourne la distance en km
    }

    /**
     * --- MÉTHODE PRO : CALCUL DU PRIX ---
     * On applique un tarif de base + un prix au kilomètre (HTG).
     */
    private Double calculateFarePro(double distance) {
        double baseFare = 200.0; // Prise en charge fixe
        double ratePerKm = 150.0; // Tarif au kilomètre à Port-au-Prince
        
        double total = baseFare + (distance * ratePerKm);
        
        // Arrondi à l'unité supérieure pour un prix propre en HTG
        return Math.ceil(total);
    }
    
    public Double calculateEstimate(String start, String end) {
        double baseFare = 250.0; 
        if (start.contains("Pétion-Ville") && end.contains("Aéroport")) {
            return baseFare + 500.0;
        }
        return baseFare + 150.0; 
    }

    /**
     * --- MÉTHODE PRO : CRÉATION DE COURSE AVEC GPS ---
     */
    public Ride requestRide(User passenger, String from, String to, 
                           Double lat1, Double lon1, Double lat2, Double lon2) {
        Ride ride = new Ride();
        ride.setPassenger(passenger);
        ride.setPickupLocation(from);
        ride.setDestination(to);
        
        // Stockage des coordonnées pour la Map
        ride.setPickupLatitude(lat1);
        ride.setPickupLongitude(lon1);
        ride.setDestinationLatitude(lat2);
        ride.setDestinationLongitude(lon2);

        // Calcul du prix PRO basé sur la distance réelle
        // Utilisation de Double (objet) pour éviter le crash si null
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        ride.setFare(calculateFarePro(distance));
        
        ride.setStatus(RideStatus.REQUESTED);
        ride.setStartTime(java.time.LocalDateTime.now()); // Date de la demande
        
        return rideRepository.save(ride);
    }

    /**
     * --- MÉTHODE PRO : LOGIQUE D'ACCEPTATION ---
     */
    public Ride acceptRide(Long rideId, User chauffeur) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Course introuvable."));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new IllegalStateException("Désolé, cette course a déjà été acceptée par un autre chauffeur.");
        }

        ride.setChauffeur(chauffeur);
        ride.setDriver(chauffeur); 
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setStartTime(java.time.LocalDateTime.now()); // PRO : On enregistre le début réel
        
        Ride savedRide = rideRepository.save(ride);
        
        String destination = "/topic/passenger/" + ride.getPassenger().getUsername();
        Map<String, Object> notification = new HashMap<>(); 
        notification.put("type", "RIDE_ACCEPTED");
        notification.put("driverName", chauffeur.getUsername());
        notification.put("message", "Votre chauffeur " + chauffeur.getUsername() + " est en route !");
        notification.put("rideId", ride.getId());
        
        messagingTemplate.convertAndSend(destination, (Object) notification);
        
        return savedRide;
    }

    public List<Ride> getAvailableRides() {
        return rideRepository.findByStatus(RideStatus.REQUESTED);
    }

    /**
     * --- MÉTHODE PRO : CLÔTURE DE COURSE SÉCURISÉE ---
     */
    public Ride completeRide(Long rideId) {
        // 1. Récupérer l'utilisateur qui fait la requête (le chauffeur connecté)
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Course introuvable."));

        // 2. Vérification de sécurité : Seul le chauffeur assigné peut terminer sa propre course
        if (ride.getChauffeur() == null || !ride.getChauffeur().getUsername().equals(currentUsername)) {
            throw new IllegalStateException("Sécurité : Vous n'êtes pas autorisé à terminer cette course car vous n'en êtes pas le chauffeur.");
        }

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalStateException("Impossible de terminer une course qui n'est pas en état 'Acceptée'.");
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setEndTime(java.time.LocalDateTime.now());
        Ride finishedRide = rideRepository.save(ride);

        String destination = "/topic/passenger/" + ride.getPassenger().getUsername();
        Map<String, Object> endNotification = new HashMap<>();
        endNotification.put("type", "RIDE_COMPLETED");
        endNotification.put("message", "Vous êtes arrivé à destination. Merci d'utiliser Uber Ouest !");
        endNotification.put("fare", ride.getFare().toString() + " HTG");

        messagingTemplate.convertAndSend(destination, (Object) endNotification);

        return finishedRide;
    }
}