package ht.uber.ouest.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class MonCashService {

    @Value("${moncash.client.id}")
    private String clientId;

    @Value("${moncash.client.secret}")
    private String secret;

    @Value("${moncash.api.token}")
    private String tokenUrl;

    @Value("${moncash.api.payment}")
    private String paymentUrl;

    @Value("${moncash.url.redirect}")
    private String redirectUrlBase;

    // ⚠️ AJOUT : URL de retour (celle que MonCash appellera après paiement)
    // Pour les tests locaux, vous devez exposer votre serveur publiquement (ngrok).
    // Si vous utilisez localhost, MonCash ne pourra pas joindre votre serveur.
    @Value("${moncash.return.url}")
private String returnUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    static {
        // Force Java à utiliser les protocoles sécurisés modernes (Essentiel pour Java 24)
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
        // Ignore les erreurs de nom de domaine sur les certificats (fréquent en Sandbox)
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
    }

    private String getAccessToken() {
        try {
            String auth = clientId + ":" + secret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("Accept", "application/json");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("scope", "read,write");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
            throw new RuntimeException("Erreur Token : " + response.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Connexion MonCash impossible (Vérifiez Client ID/Secret) : " + e.getMessage());
        }
    }

   
    @SuppressWarnings("unchecked")
    public String generateMonCashUrl(Long rideId, Double amount) {
        try {
            // 1. Récupération du jeton d'accès
            String token = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/json");

            // 2. Préparation des données de paiement (Le coeur du problème 404)
            Map<String, Object> paymentData = new HashMap<>();
            
            // FIX : MonCash Sandbox rejette souvent les nombres à virgule. On force un entier.
            paymentData.put("amount", amount.intValue());
            
            // FIX : ID de commande court et unique (Max 25 caractères pour être sûr)
            String cleanOrderId = "R" + rideId + "T" + (System.currentTimeMillis() / 10000);
            paymentData.put("orderId", cleanOrderId);
            
            // FIX : Description obligatoire et simple sans caractères spéciaux
            paymentData.put("item", "Course");

            // ⭐ AJOUT OBLIGATOIRE : URL de retour (returnUrl)
            paymentData.put("returnUrl", returnUrl);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(paymentData, headers);
            
            // 3. Appel à l'API CreatePayment
            ResponseEntity<Map> response = restTemplate.postForEntity(paymentUrl, request, Map.class);
            
            // LOG pour débogage
            System.out.println("REPONSE BRUTE MONCASH : " + response.getBody());

            // ✅ Accepter les statuts 2xx (200, 201, 202...)
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String transactionToken = null;

                // Extraction du token (adaptée à votre réponse)
                // Votre réponse contient payment_token -> objet avec un champ "token"
                if (body.containsKey("payment_token")) {
                    Object ptObj = body.get("payment_token");
                    if (ptObj instanceof Map) {
                        Map<String, Object> ptMap = (Map<String, Object>) ptObj;
                        if (ptMap.containsKey("token")) {
                            transactionToken = (String) ptMap.get("token");
                        }
                    } else if (ptObj instanceof String) {
                        transactionToken = (String) ptObj;
                    }
                } 
                // Fallback : si la réponse contient directement "token"
                else if (body.containsKey("token")) {
                    transactionToken = (String) body.get("token");
                }
                
                if (transactionToken != null && !transactionToken.isEmpty()) {
                    // ===== CORRECTION : CONSTRUCTION ROBUSTE DE L'URL FINALE =====
                    // Nettoyer la base de redirection
                    String base = redirectUrlBase.trim();
                    
                    // Si la base ne contient pas "?token=", l'ajouter proprement
                    if (!base.contains("?token=")) {
                        // Supprimer un éventuel slash final ou un point d'interrogation existant
                        base = base.replaceAll("/$", "");
                        base = base.replaceAll("\\?$", "");
                        base += "?token=";
                    } else {
                        // Si elle contient déjà "?token=", s'assurer qu'il n'y a pas de double slash avant le token
                        base = base.replaceAll("(?<=token=)/+", "");
                    }
                    
                    // Concaténer le token
                    String finalUrl = base + transactionToken.trim();
                    
                    // Éviter les doubles slashes dans l'URL (sauf après le protocole https://)
                    finalUrl = finalUrl.replaceFirst("(?<=https?:)/{2,}", "/");
                    
                    System.out.println("✅ URL GÉNÉRÉE : " + finalUrl);
                    return finalUrl;
                } else {
                    System.err.println("❌ Aucun token trouvé dans la réponse MonCash. Body : " + body);
                    throw new RuntimeException("Token manquant dans la réponse MonCash");
                }
            } else {
                System.err.println("❌ Réponse HTTP non 2xx : " + response.getStatusCode());
                throw new RuntimeException("Échec de l'appel CreatePayment : " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("ERREUR CRÉATION PAIEMENT : " + e.getMessage());
            throw new RuntimeException("Erreur MonCash : " + e.getMessage());
        }
    }
}
