package ht.uber.ouest.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String home() {
        // Redirige vers /login pour un flux utilisateur propre
        return "redirect:/login"; // Redirection automatique
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login"; // Fichier : src/main/resources/templates/auth/login.html
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register"; // Fichier : src/main/resources/templates/auth/register.html
    }
    
    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard"; // Cherche src/main/resources/templates/dashboard.html
    }

    // --- AJOUT PRO : NOUVELLES ROUTES POUR UBER OUEST ---

    @GetMapping("/dashboard_chauffeur")
    public String dashboardChauffeurPage() {
        return "dashboard_chauffeur"; // Cherche src/main/resources/templates/dashboard_chauffeur.html
    }

    @GetMapping("/ride_active")
    public String rideActivePage() {
        return "ride_active"; // Cherche src/main/resources/templates/ride_active.html
    }

    // --- ROUTE ADMIN ---
    @GetMapping("/admin-dashboard")
    public String adminDashboardPage() {
        return "admin-dashboard"; // Cherche src/main/resources/templates/admin-dashboard.html
    }
}