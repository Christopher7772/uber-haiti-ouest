package ht.uber.ouest.controllers;

import ht.uber.ouest.rides.Ride;
import ht.uber.ouest.rides.RideRepository;
import ht.uber.ouest.payments.MonCashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private MonCashService monCashService;

    @Autowired
    private RideRepository rideRepository;

    @PostMapping("/initiate/{rideId}")
    public ResponseEntity<?> initiatePayment(@PathVariable Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Course introuvable ID: " + rideId));
        String paymentUrl = monCashService.generateMonCashUrl(ride.getId(), ride.getFare());
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * Webhook POST (Alert URL) – pour la confirmation automatique en production.
     */
    @PostMapping("/return")
    public ResponseEntity<?> processMonCashAlert(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("📥 WEBHOOK RECU - Payload complet : " + payload);
            String orderIdStr = null;
            if (payload.containsKey("orderId")) orderIdStr = (String) payload.get("orderId");
            else if (payload.containsKey("transactionId")) orderIdStr = (String) payload.get("transactionId");
            // ... autres clés possibles
            
            if (orderIdStr != null && orderIdStr.contains("R") && orderIdStr.contains("T")) {
                String rideIdStr = orderIdStr.substring(orderIdStr.indexOf("R") + 1, orderIdStr.indexOf("T"));
                Long rideId = Long.parseLong(rideIdStr);
                Ride ride = rideRepository.findById(rideId).orElse(null);
                if (ride != null && !"PAYÉ".equals(ride.getPaymentStatus())) {
                    ride.setPaymentStatus("PAYÉ");
                    rideRepository.save(ride);
                    System.out.println("✅ Webhook : Course " + rideId + " PAYÉE");
                }
                return ResponseEntity.ok("OK");
            }
            return ResponseEntity.badRequest().body("Format invalide");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Page de succès (GET) – appelée par MonCash après paiement.
     * C'est ici que nous forçons la mise à jour du statut (contournement Sandbox).
     */
    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccessPage(@RequestParam(required = false) String transactionId) {
        System.out.println("🔁 Redirection /success - transactionId=" + transactionId);
        
        // === CONTOURNEMENT : mise à jour du statut ===
        if (transactionId != null && transactionId.contains("R") && transactionId.contains("T")) {
            try {
                String rideIdStr = transactionId.substring(transactionId.indexOf("R") + 1, transactionId.indexOf("T"));
                Long rideId = Long.parseLong(rideIdStr);
                Optional<Ride> optionalRide = rideRepository.findById(rideId);
                if (optionalRide.isPresent()) {
                    Ride ride = optionalRide.get();
                    if (!"PAYÉ".equals(ride.getPaymentStatus())) {
                        ride.setPaymentStatus("PAYÉ");
                        rideRepository.save(ride);
                        System.out.println("✅ (success) Course " + rideId + " marquée PAYÉE");
                    }
                } else {
                    System.err.println("❌ Course " + rideId + " non trouvée");
                }
            } catch (Exception e) {
                System.err.println("Erreur extraction: " + e.getMessage());
            }
        }
        
        String html = "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                      "<h1 style='color:green;'>Paiement Réussi ! ✅</h1>" +
                      "<p>Votre transaction (" + transactionId + ") a été confirmée.</p>" +
                      "<a href='/dashboard'>Retour au tableau de bord</a>" +
                      "</body></html>";
        return ResponseEntity.ok(html);
    }

    /**
     * Redirection GET de secours (si jamais MonCash appelle /return en GET)
     */
    @GetMapping("/return")
    public ResponseEntity<String> handleReturnRedirect(@RequestParam(required = false) String transactionId) {
        // Rediriger vers /success pour centraliser la logique
        return ResponseEntity.status(302).header("Location", "/api/payment/success?transactionId=" + (transactionId != null ? transactionId : "")).build();
    }
}
