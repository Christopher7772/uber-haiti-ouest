# Étape 1 : Construction (Build) avec Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copier le fichier de configuration Maven et les dépendances (pour mettre en cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copier le code source et compiler le projet
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Exécution (Runtime) avec Java 17
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copier le fichier .jar généré à l'étape précédente
COPY --from=build /app/target/*.jar app.jar

# Exposer le port par défaut de Spring Boot (8080)
EXPOSE 8080

# Commande de lancement optimisée pour le Cloud
# On ajoute les paramètres TLS que tu as mis dans ton code pour MonCash
ENTRYPOINT ["java", "-Dhttps.protocols=TLSv1.2,TLSv1.3", "-jar", "app.jar"]