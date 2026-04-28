package ht.uber.ouest.rides;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    // Trouver les courses d'un passager spécifique
    List<Ride> findByPassengerUsername(String username);
    
     List<Ride> findByStatus(RideStatus status);
     List<Ride> findByPaymentStatus(String paymentStatus);
}


