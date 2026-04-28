package ht.uber.ouest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UberHaitiOuestApplication {

    static {
        // Force l'utilisation de TLS 1.2 et 1.3 pour éviter l'erreur "Unexpected end of file"
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
        
        // Désactive la vérification stricte du nom d'hôte pour la Sandbox Digicel
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
    }

    public static void main(String[] args) {
        SpringApplication.run(UberHaitiOuestApplication.class, args);
    }

}