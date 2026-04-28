package ht.uber.ouest.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Utilisation d'Optional pour une gestion sécurisée (évite les NullPointerException)
    Optional<User> findByUsername(String username);
}