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
            // 🔍 LOG DU PAYLOAD COMPLET (indispensable pour voir ce que MonCash envoie)
            System.out.println("📥 WEBHOOK RECU - Payload complet : " + payload);
            
            // Extraction de l'identifiant (orderId) depuis différentes clés possibles
            String orderIdStr = null;
            if (payload.containsKey("orderId")) {
                orderIdStr = (String) payload.get("orderId");
            } else if (payload.containsKey("order_id")) {
                orderIdStr = (String) payload.get("order_id");
            } else if (payload.containsKey("reference")) {
                orderIdStr = (String) payload.get("reference");
            } else if (payload.containsKey("transactionId")) {
                orderIdStr = (String) payload.get("transactionId");
            } else if (payload.containsKey("id")) {
                orderIdStr = (String) payload.get("id");
            }
            
            System.out.println("🔑 Identifiant extrait : " + orderIdStr);
            
            if (orderIdStr != null && orderIdStr.contains("R") && orderIdStr.contains("T")) {
                // Découpage : on prend ce qui est APRES 'R' et AVANT 'T'
                String rideIdStr = orderIdStr.substring(orderIdStr.indexOf("R") + 1, orderIdStr.indexOf("T"));
                Long rideId = Long.parseLong(rideIdStr);
                System.out.println("🚗 ID course extrait : " + rideId);

                // Mise à jour du statut en base de données
                Ride ride = rideRepository.findById(rideId).orElse(null);
                if (ride != null && !"PAYÉ".equals(ride.getPaymentStatus())) {
                    ride.setPaymentStatus("PAYÉ");
                    rideRepository.save(ride);
                    System.out.println("✅ Webhook : Course " + rideId + " PAYÉE");
                    return ResponseEntity.ok("Notification reçue");
                } else {
                    System.err.println("❌ Course non trouvée ou déjà payée pour l'ID : " + rideId);
                    return ResponseEntity.badRequest().body("Course inexistante ou déjà payée");
                }
            }
            
            System.err.println("⚠️ Aucun orderId valide trouvé dans le payload. Clés reçues : " + payload.keySet());
            return ResponseEntity.badRequest().body("Format orderId invalide");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du Webhook : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 3. RETURN URL (PAGE DE SUCCÈS) : Configuré sur "/success" dans ton Dashboard.
     * C'est ici que l'utilisateur est redirigé VISUELLEMENT après avoir payé.
     * Cette méthode gère également le contournement pour le sandbox en mettant à jour
     * le statut du paiement à partir du paramètre de redirection.
     */
    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccessPage(@RequestParam Map<String, String> allParams) {
        System.out.println("🔁 Redirection /success - Paramètres reçus : " + allParams);
        
        // Extraire un identifiant parmi plusieurs noms possibles (transactionId, orderId, reference, id)
        String candidate = null;
        if (allParams.containsKey("transactionId")) candidate = allParams.get("transactionId");
        else if (allParams.containsKey("orderId")) candidate = allParams.get("orderId");
        else if (allParams.containsKey("order_id")) candidate = allParams.get("order_id");
        else if (allParams.containsKey("reference")) candidate = allParams.get("reference");
        else if (allParams.containsKey("id")) candidate = allParams.get("id");
        
        // === CONTOURNEMENT : mise à jour du statut si l'identifiant contient le format R...T...
        if (candidate != null && candidate.contains("R") && candidate.contains("T")) {
            try {
                String rideIdStr = candidate.substring(candidate.indexOf("R") + 1, candidate.indexOf("T"));
                Long rideId = Long.parseLong(rideIdStr);
                Optional<Ride> optionalRide = rideRepository.findById(rideId);
                if (optionalRide.isPresent()) {
                    Ride ride = optionalRide.get();
                    if (!"PAYÉ".equals(ride.getPaymentStatus())) {
                        ride.setPaymentStatus("PAYÉ");
                        rideRepository.save(ride);
                        System.out.println("✅ (success) Course " + rideId + " marquée PAYÉE depuis paramètre " + candidate);
                    } else {
                        System.out.println("ℹ️ Course " + rideId + " déjà payée");
                    }
                } else {
                    System.err.println("❌ Course " + rideId + " non trouvée");
                }
            } catch (Exception e) {
                System.err.println("Erreur extraction: " + e.getMessage());
            }
        } else if (candidate != null) {
            System.out.println("⚠️ Identifiant reçu ne correspond pas au format attendu (R...T...) : " + candidate);
        } else {
            System.out.println("⚠️ Aucun identifiant trouvé dans les paramètres");
        }
        
        String html = "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                      "<h1 style='color:green;'>Paiement Réussi ! ✅</h1>" +
                      "<p>Votre transaction a été confirmée.</p>" +
                      "<a href='/dashboard'>Retour au tableau de bord</a>" +
                      "</body></html>";
        return ResponseEntity.ok(html);
    }

    /**
     * 4. REDIRECTION APRÈS PAIEMENT (GET) – Endpoint de secours.
     * Si MonCash appelle /return en GET (au lieu de /success), on redirige vers /success
     * tout en conservant les paramètres.
     */
    @GetMapping("/return")
    public ResponseEntity<Void> handleReturnRedirect(@RequestParam Map<String, String> allParams) {
        System.out.println("🔄 GET /return appelé avec paramètres : " + allParams);
        // Redirige vers /success pour centraliser le traitement
        StringBuilder redirectUrl = new StringBuilder("/api/payment/success");
        if (!allParams.isEmpty()) {
            redirectUrl.append("?");
            allParams.forEach((k, v) -> redirectUrl.append(k).append("=").append(v).append("&"));
            redirectUrl.setLength(redirectUrl.length() - 1); // supprime le dernier '&'
        }
        return ResponseEntity.status(302).header("Location", redirectUrl.toString()).build();
    }

    /**
     * 5. ENDPOINT DE TEST (TEMPORAIRE) : Pour vérifier si MonCash appelle bien la Return URL.
     * À utiliser lors du diagnostic : configurer cette URL comme Return URL dans le dashboard MonCash,
     * puis observer les logs.
     */
    @GetMapping("/test-redirect")
    public ResponseEntity<String> testRedirect(@RequestParam Map<String, String> allParams) {
        System.out.println("🎯 TEST REDIRECT - Paramètres reçus : " + allParams);
        String html = "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                      "<h1>Test de redirection réussi ✅</h1>" +
                      "<p>Paramètres reçus : " + allParams + "</p>" +
                      "</body></html>";
        return ResponseEntity.ok(html);
    }
}
