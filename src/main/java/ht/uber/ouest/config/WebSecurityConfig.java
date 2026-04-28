package ht.uber.ouest.config;

import ht.uber.ouest.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    // 1. Responsabilité : Définir la méthode de hachage (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Responsabilité : Exposer l'AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // 3. Responsabilité : Instancier le filtre JWT
    @Bean
    public JwtAuthenticationFilter authenticationJwtTokenFilter() {
        return new JwtAuthenticationFilter();
    }

    // 4. Responsabilité : Configurer la chaîne de filtres de sécurité
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Configuration CORS (Autorise les requêtes Cross-Origin)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Désactiver le CSRF (Inutile car nous utilisons des Tokens JWT et non des Cookies)
            .csrf(csrf -> csrf.disable())
            
            // Gestion des erreurs d'authentification (Renvoie 401 au lieu d'une page de login HTML)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(
                (request, response, authException) -> response.sendError(401, "Erreur: Non autorisé")
            ))
            
            // Politique Stateless : On ne crée pas de session sur le serveur
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // --- GESTION DES ACCÈS ---
            .authorizeHttpRequests(auth -> auth
                // VUES (HTML/CSS/JS) : Toujours accessibles pour que le frontend puisse charger
                .requestMatchers("/", "/login", "/register", "/dashboard", "/dashboard_chauffeur", "/admin-dashboard", "/ride_active").permitAll()
                
                // RESSOURCES STATIQUES (Images, scripts)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                // API AUTHENTIFICATION (Login/Inscription)
                .requestMatchers("/api/auth/**").permitAll()
                
                // API PAIEMENT MONCASH (Public pour les retours de Digicel)
                .requestMatchers("/api/payment/success", "/api/payment/return").permitAll()

                // CONSOLE H2 (Développement uniquement)
                .requestMatchers("/h2-console/**").permitAll()

                // --- APIS SÉCURISÉES ---
                
                // Admin : Nécessite le rôle ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Chauffeurs : Accès aux courses disponibles
                .requestMatchers("/api/rides/available").hasRole("CHAUFFEUR")
                
                // Passagers & Chauffeurs : Actions générales
                .requestMatchers("/api/rides/**", "/api/payment/initiate/**").authenticated()

                // Sécurité par défaut : Tout le reste demande une connexion
                .anyRequest().authenticated()
            );

        // Autoriser l'affichage de la console H2 dans des frames (IFrame)
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        // Injection du filtre JWT avant le filtre standard de Spring Security
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    /**
     * Configuration CORS PRO : Autorise les headers nécessaires pour le JWT
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // En production, mettre le domaine précis
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(List.of("authorization")); // Permet au client de lire le header Auth
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}