package ht.uber.ouest.rides;

import ht.uber.ouest.users.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version; // PRO : Empêche deux chauffeurs d'accepter la même course simultanément

    // Le point de départ (Ex: Delmas 33)
    private String pickupLocation;

    // Coordonnées précises pour la map et MonCash
    private Double pickupLatitude;
    private Double pickupLongitude;

    // La destination (Ex: Pétion-Ville, Place Boyer)
    private String destination;
    
    private Double destinationLatitude;
    private Double destinationLongitude;

    // Prix calculé en Gourdes ou USD
    private Double fare;

    @Enumerated(EnumType.STRING)
    private RideStatus status;

    // --- AJOUT PRO : STATUT DU PAIEMENT ---
    // Par défaut, une course est créée avec le statut "NON_PAYÉ"
    private String paymentStatus = "NON_PAYÉ"; 

    @ManyToOne
    @JoinColumn(name = "passenger_id")
    private User passenger;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver; // Sera null tant qu'aucun chauffeur n'a accepté

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Ride() {}
    
    @ManyToOne
    @JoinColumn(name = "chauffeur_id")
    private User chauffeur; // Le chauffeur qui a accepté la course (doublon utile pour ta logique actuelle)

    // Getter et Setter pour chauffeur
    public User getChauffeur() { return chauffeur; }
    public void setChauffeur(User chauffeur) { this.chauffeur = chauffeur; }

    // --- GETTER ET SETTER POUR PAYMENT_STATUS ---
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    // Getters et Setters (Générés via Alt+Insert dans NetBeans)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getVersion() { return version; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    
    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }
    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    
    public Double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(Double destinationLatitude) { this.destinationLatitude = destinationLatitude; }
    public Double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(Double destinationLongitude) { this.destinationLongitude = destinationLongitude; }

    public Double getFare() { return fare; }
    public void setFare(Double fare) { this.fare = fare; }
    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus status) { this.status = status; }
    public User getPassenger() { return passenger; }
    public void setPassenger(User passenger) { this.passenger = passenger; }
    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}