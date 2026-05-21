FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /app

# Copie du pom.xml seul pour mettre en cache les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copie du code source et compilation
COPY src ./src
RUN mvn clean package -q

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target/calculator-api-java-1.0-SNAPSHOT.jar app.jar

EXPOSE 8000

CMD ["java", "-jar", "app.jar"]