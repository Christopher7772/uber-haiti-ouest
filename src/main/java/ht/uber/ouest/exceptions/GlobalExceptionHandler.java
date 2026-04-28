package ht.uber.ouest.exceptions; // Assure-toi que le package est correct

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    // PRO : Gère les erreurs de logique (ex: course déjà prise)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "ACTION_NON_AUTORISEE");
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    // PRO : Gère les erreurs de ressources non trouvées
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "ERREUR_SERVEUR");
        body.put("message", ex.getMessage());
        return ResponseEntity.internalServerError().body(body);
    }
}