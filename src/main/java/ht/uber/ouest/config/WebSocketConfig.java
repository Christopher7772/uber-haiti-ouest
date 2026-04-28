package ht.uber.ouest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Canal pour envoyer des messages du serveur vers le client
        config.enableSimpleBroker("/topic");
        // Préfixe pour les messages envoyés du client vers le serveur
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Point d'entrée de la connexion (utilisé par le JS)
        registry.addEndpoint("/ws_uber").withSockJS();
    }
}