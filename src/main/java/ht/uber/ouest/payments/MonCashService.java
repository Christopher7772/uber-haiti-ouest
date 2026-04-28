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

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(paymentData, headers);
            
            // 3. Appel à l'API CreatePayment
            ResponseEntity<Map> response = restTemplate.postForEntity(paymentUrl, request, Map.class);
            
            // LOG pour débogage : Si ça 404, regarde ce qui est écrit ici dans ta console
            System.out.println("REPONSE BRUTE MONCASH : " + response.getBody());

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String transactionToken = null;

                // Extraction du token de transaction selon le format MonCash
                if (body.containsKey("payment_token")) {
                    Map<String, Object> pToken = (Map<String, Object>) body.get("payment_token");
                    transactionToken = (String) pToken.get("token");
                } 
                
                if (transactionToken != null) {
                    // Construction de l'URL finale pour redirection client
                    String finalUrl = redirectUrlBase.trim() + transactionToken.trim();
                    System.out.println("URL GÉNÉRÉE : " + finalUrl);
                    return finalUrl;
                }
            }
            throw new RuntimeException("Réponse MonCash vide ou invalide.");

        } catch (Exception e) {
            System.err.println("ERREUR CRÉATION PAIEMENT : " + e.getMessage());
            throw new RuntimeException("Erreur MonCash : " + e.getMessage());
        }
    }
}