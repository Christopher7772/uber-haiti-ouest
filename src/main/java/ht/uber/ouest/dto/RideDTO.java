package ht.uber.ouest.dto;

import ht.uber.ouest.rides.RideStatus;
import java.time.LocalDateTime;

/**
 * --- CLASSE PRO : DATA TRANSFER OBJECT ---
 * Ce record (ou classe) sert à filtrer les données envoyées au client mobile.
 * Mise à jour : Intégration du statut de paiement pour le suivi MonCash.
 */
public class RideDTO {
    private Long id;
    private String pickupLocation;
    private String destination;
    private Double fare;
    private RideStatus status;
    private String paymentStatus; // NOUVEAU : "NON_PAYÉ", "EN_ATTENTE", "PAYÉ"
    private String passengerName; 
    private String driverName;    
    private LocalDateTime startTime;

    public RideDTO(Long id, String pickupLocation, String destination, Double fare, 
                   RideStatus status, String paymentStatus, String passengerName, 
                   String driverName, LocalDateTime startTime) {
        this.id = id;
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.fare = fare;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.passengerName = passengerName;
        this.driverName = driverName;
        this.startTime = startTime;
    }

    // Getters
    public Long getId() { return id; }
    public String getPickupLocation() { return pickupLocation; }
    public String getDestination() { return destination; }
    public Double getFare() { return fare; }
    public RideStatus getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; } // Getter pour le paiement
    public String getPassengerName() { return passengerName; }
    public String getDriverName() { return driverName; }
    public LocalDateTime getStartTime() { return startTime; }
}