package ht.uber.ouest.controllers;

import ht.uber.ouest.rides.Ride;
import ht.uber.ouest.rides.RideRepository;
import ht.uber.ouest.payments.MonCashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur de paiement optimisé pour Uber Ouest.
 * Gère l'initialisation du paiement et les notifications de MonCash.
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private MonCashService monCashService;

    @Autowired
    private RideRepository rideRepository;

    /**
     * 1. INITIALISATION : Appelé par le Dashboard quand le client clique sur "Payer".
     */
    @PostMapping("/initiate/{rideId}")
    public ResponseEntity<?> initiatePayment(@PathVariable Long rideId) {
        // On récupère la course ou on lance une erreur si inexistante
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Course introuvable ID: " + rideId));

        // On génère l'URL de redirection via le service
        String paymentUrl = monCashService.generateMonCashUrl(ride.getId(), ride.getFare());

        // On retourne l'URL au format JSON pour le JavaScript (fetch)
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 2. ALERT URL (WEBHOOK) : Configuré sur "/return" dans ton Dashboard MonCash.
     * C'est ici que MonCash confirme SECRÈTEMENT le paiement en base de données.
     */
    @PostMapping("/return")
    public ResponseEntity<?> processMonCashAlert(@RequestBody Map<String, Object> payload) {
        try {
            // Extraction de l'orderId (ex: R7T177733166)
            String orderIdStr = (String) payload.get("orderId");
            
            if (orderIdStr != null && orderIdStr.contains("R") && orderIdStr.contains("T")) {
                // Découpage : on prend ce qui est APRES 'R' et AVANT 'T'
                String rideIdStr = orderIdStr.substring(orderIdStr.indexOf("R") + 1, orderIdStr.indexOf("T"));
                Long rideId = Long.parseLong(rideIdStr);

                // Mise à jour du statut en base de données
                Ride ride = rideRepository.findById(rideId).orElseThrow();
                ride.setPaymentStatus("PAYÉ");
                rideRepository.save(ride);

                System.out.println("✅ Notification MonCash traitée : Course " + rideId + " est PAYÉE.");
                return ResponseEntity.ok("Notification reçue");
            }
            
            return ResponseEntity.badRequest().body("Format orderId invalide");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du Webhook : " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 3. RETURN URL (PAGE DE SUCCÈS) : Configuré sur "/success" dans ton Dashboard.
     * C'est ici que l'utilisateur est redirigé VISUELLEMENT après avoir payé.
     */
    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccessPage() {
        // Une réponse HTML simple pour informer l'utilisateur
        String html = "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                      "<h1 style='color:green;'>Paiement Réussi ! ✅</h1>" +
                      "<p>Merci ! Votre paiement a été confirmé. Vous pouvez retourner sur l'application.</p>" +
                      "</body></html>";
        return ResponseEntity.ok(html);
    }

    /**
     * 4. REDIRECTION APRÈS PAIEMENT (GET) : Appelée par MonCash pour ramener l'utilisateur.
     *    Le paramètre transactionId est ajouté automatiquement par MonCash.
     */
    @GetMapping("/return")
    public ResponseEntity<String> handleReturnRedirect(@RequestParam(required = false) String transactionId) {
        System.out.println("🔁 Redirection utilisateur après paiement, transactionId=" + transactionId);
        // Affiche une page de confirmation (peut aussi rediriger vers /success)
        String html = "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                      "<h1 style='color:green;'>Paiement effectué avec succès ✅</h1>" +
                      "<p>Votre transaction (" + transactionId + ") a été enregistrée.</p>" +
                      "<a href='/dashboard'>Retour au tableau de bord</a>" +
                      "</body></html>";
        return ResponseEntity.ok(html);
    }
}
