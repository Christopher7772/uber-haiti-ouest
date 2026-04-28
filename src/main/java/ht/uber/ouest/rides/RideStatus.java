package ht.uber.ouest.rides;

public enum RideStatus {
    REQUESTED,   // Le passager attend un chauffeur
    ACCEPTED,    // Un chauffeur a accepté la course
    IN_PROGRESS, // Le trajet est en cours
    COMPLETED,   // Le trajet est terminé
    CANCELLED    // La course a été annulée
}