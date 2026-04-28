package ht.uber.ouest.dto;

public class RideRequest {
    private String pickupLocation;
    private String destination;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double destinationLatitude;
    private Double destinationLongitude;

    // Getters
    public String getPickupLocation() { return pickupLocation; }
    public String getDestination() { return destination; }
    public Double getPickupLatitude() { return pickupLatitude; }
    public Double getPickupLongitude() { return pickupLongitude; }
    public Double getDestinationLatitude() { return destinationLatitude; }
    public Double getDestinationLongitude() { return destinationLongitude; }

    // Setters 
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }
    public void setDestinationLatitude(Double destinationLatitude) { this.destinationLatitude = destinationLatitude; }
    public void setDestinationLongitude(Double destinationLongitude) { this.destinationLongitude = destinationLongitude; }
}