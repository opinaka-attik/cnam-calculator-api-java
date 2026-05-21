Oui. Voici **la version Java complète demandée**, avec **Dockerfile**, **docker-compose.yml**, **HttpServer** et **JUnit 5**, prête à copier-coller. Une configuration Docker simple pour Java repose généralement sur Maven pour builder le projet puis sur une image JRE/JDK pour exécuter l’application, et Docker documente aussi l’exécution des tests Java dans ce type de workflow. [docs.docker](https://docs.docker.com/guides/java/run-tests/)

## Structure

Cette structure Maven minimale sépare bien l’application, les tests et les fichiers Docker, ce qui reste adapté à un usage pédagogique. `HttpServer` vient du JDK, donc tu n’as pas besoin de framework web supplémentaire. [docs.oracle](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/package-summary.html)

```txt
calculator-api-java/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── .dockerignore
└── src/
    ├── main/
    │   └── java/
    │       └── com/example/calculator/
    │           ├── Calculator.java
    │           └── CalculatorApiServer.java
    └── test/
        └── java/
            └── com/example/calculator/
                └── CalculatorTest.java
```

## Fichiers

Le projet utilise Maven pour compiler et lancer JUnit, puis Docker construit l’image à partir du projet. Pour un exemple simple, un Dockerfile multi-stage avec une étape Maven puis une étape Java runtime est une approche standard et propre. [stackoverflow](https://stackoverflow.com/questions/52120845/docker-compose-build-with-maven-that-re-uses-the-maven-repository)

### `Dockerfile`

```dockerfile
FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target/calculator-api-java-1.0-SNAPSHOT.jar app.jar

EXPOSE 8000

CMD ["java", "-jar", "app.jar"]
```

### `docker-compose.yml`

```yaml
services:
  api:
    build: .
    container_name: java-calculator-api
    ports:
      - "8000:8000"
```

### `.dockerignore`

```txt
target
.idea
.git
```

### `pom.xml`

JUnit 5 s’ajoute en dépendance de test, tandis que `maven-jar-plugin` permet de déclarer la classe principale pour que `java -jar` fonctionne correctement dans le conteneur. La configuration Maven suit le schéma habituel des projets Java testés avec JUnit 5. [mkyong](https://mkyong.com/junit5/junit-5-maven-examples/)

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>calculator-api-java</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.10.2</junit.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.4.1</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.example.calculator.CalculatorApiServer</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### `src/main/java/com/example/calculator/Calculator.java`

```java
package com.example.calculator;

/**
 * Classe métier.
 *
 * Elle contient uniquement les opérations de calcul.
 * Elle ne dépend ni du serveur HTTP ni de Docker.
 */
public class Calculator {

    /**
     * Additionne deux nombres.
     */
    public double add(double a, double b) {
        return a + b;
    }

    /**
     * Soustrait b à a.
     */
    public double subtract(double a, double b) {
        return a - b;
    }

    /**
     * Multiplie deux nombres.
     */
    public double multiply(double a, double b) {
        return a * b;
    }

    /**
     * Divise a par b.
     *
     * On lève une exception si b vaut 0.
     */
    public double divide(double a, double b) {
        if (b == 0) {
            throw new IllegalArgumentException("Division par zéro impossible.");
        }

        return a / b;
    }
}
```

### `src/main/java/com/example/calculator/CalculatorApiServer.java`

`HttpServer` permet de créer un petit serveur HTTP embarqué sans dépendance externe, ce qui en fait une bonne base pédagogique pour montrer le fonctionnement d’une API REST très simple. Oracle documente cette API dans le package `com.sun.net.httpserver`. [docs.oracle](https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/package-summary.html)

```java
package com.example.calculator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CalculatorApiServer {

    public static void main(String[] args) throws IOException {
        int port = 8000;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/calculate", CalculatorApiServer::handleCalculate);

        server.setExecutor(null);
        server.start();

        System.out.println("Serveur démarré sur http://localhost:" + port);
    }

    private static void handleCalculate(HttpExchange exchange) throws IOException {
        Calculator calculator = new Calculator();

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, """
                {
                  "error": "Méthode non autorisée. Utiliser GET."
                }
                """);
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI());

        String operation = params.get("operation");
        String aParam = params.get("a");
        String bParam = params.get("b");

        if (operation == null || aParam == null || bParam == null) {
            sendJson(exchange, 400, """
                {
                  "error": "Paramètres attendus : operation, a, b"
                }
                """);
            return;
        }

        try {
            double a = Double.parseDouble(aParam);
            double b = Double.parseDouble(bParam);

            double result;

            switch (operation) {
                case "add":
                    result = calculator.add(a, b);
                    break;
                case "subtract":
                    result = calculator.subtract(a, b);
                    break;
                case "multiply":
                    result = calculator.multiply(a, b);
                    break;
                case "divide":
                    result = calculator.divide(a, b);
                    break;
                default:
                    sendJson(exchange, 400, """
                        {
                          "error": "Opération inconnue. Utiliser : add, subtract, multiply, divide"
                        }
                        """);
                    return;
            }

            String json = """
                {
                  "operation": "%s",
                  "a": %s,
                  "b": %s,
                  "result": %s
                }
                """.formatted(operation, a, b, result);

            sendJson(exchange, 200, json);

        } catch (NumberFormatException e) {
            sendJson(exchange, 400, """
                {
                  "error": "Les paramètres a et b doivent être des nombres."
                }
                """);
        } catch (IllegalArgumentException e) {
            String json = """
                {
                  "error": "%s"
                }
                """.formatted(e.getMessage());

            sendJson(exchange, 400, json);
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();

        String query = uri.getQuery();

        if (query == null || query.isBlank()) {
            return params;
        }

        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);

            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue [docs.docker](https://docs.docker.com/guides/java/run-tests/));
            }
        }

        return params;
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
```

### `src/test/java/com/example/calculator/CalculatorTest.java`

```java
package com.example.calculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitaires de la classe Calculator.
 */
class CalculatorTest {

    @Test
    void testAdd() {
        Calculator calculator = new Calculator();
        assertEquals(5.0, calculator.add(2, 3));
    }

    @Test
    void testSubtract() {
        Calculator calculator = new Calculator();
        assertEquals(6.0, calculator.subtract(10, 4));
    }

    @Test
    void testMultiply() {
        Calculator calculator = new Calculator();
        assertEquals(42.0, calculator.multiply(6, 7));
    }

    @Test
    void testDivide() {
        Calculator calculator = new Calculator();
        assertEquals(4.0, calculator.divide(20, 5));
    }

    @Test
    void testDivideByZeroThrowsException() {
        Calculator calculator = new Calculator();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> calculator.divide(10, 0)
        );

        assertEquals("Division par zéro impossible.", exception.getMessage());
    }
}
```

## Commandes

Avec cette configuration, tu peux builder, lancer et tester le projet uniquement avec Docker et Docker Compose. Docker montre justement ce type de workflow pour les projets Java packagés avec Maven. [docs.docker](https://docs.docker.com/reference/cli/docker/compose/)

### Construire et démarrer

```bash
docker compose up --build
```

L’API sera accessible ici :

```txt
http://localhost:8000/calculate
```

### Lancer les tests JUnit dans un conteneur temporaire

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9.8-eclipse-temurin-17 mvn test
```

Cette commande est simple pour un atelier, car elle évite d’ajouter un second service Compose uniquement pour les tests, tout en gardant Maven et JUnit dans un conteneur dédié. Docker recommande de toute façon un workflow où les tests Java sont exécutés via Maven pendant ou avant le packaging. [maven.apache](https://maven.apache.org/surefire/maven-surefire-plugin/docker.html)

## Tests curl

Quand tu passes des paramètres GET avec `&`, il faut **mettre l’URL entre guillemets**, sinon Bash interprète `&` comme un séparateur de commandes en arrière-plan. C’est un comportement normal du shell, pas de Java ni de Docker. [stackoverflow](https://stackoverflow.com/questions/13339469/how-to-include-an-character-in-a-bash-curl-statement)

### Requêtes correctes

```bash
curl "http://localhost:8000/calculate?operation=add&a=4&b=2"
```

```bash
curl "http://localhost:8000/calculate?operation=subtract&a=10&b=3"
```

```bash
curl "http://localhost:8000/calculate?operation=multiply&a=6&b=7"
```

```bash
curl "http://localhost:8000/calculate?operation=divide&a=20&b=5"
```

### Cas d’erreur

```bash
curl "http://localhost:8000/calculate?operation=divide&a=10&b=0"
```

```bash
curl "http://localhost:8000/calculate"
```

## Résultats attendus

- Addition :

```json
{
  "operation": "add",
  "a": 4.0,
  "b": 2.0,
  "result": 6.0
}
```

- Division par zéro :

```json
{
  "error": "Division par zéro impossible."
}
```

- Paramètres manquants :

```json
{
  "error": "Paramètres attendus : operation, a, b"
}
```

## Option compose tests

Si tu veux **vraiment tout via `docker compose`**, tu peux ajouter un second service `test` qui lance `mvn test`. Compose permet de définir plusieurs services pour build, run et test dans un même fichier. [geeksforgeeks](https://www.geeksforgeeks.org/devops/docker-compose-for-java-applications-simplifying-deployment/)

Ajoute cette variante dans `docker-compose.yml` :

```yaml
services:
  api:
    build: .
    container_name: java-calculator-api
    ports:
      - "8000:8000"

  test:
    image: maven:3.9.8-eclipse-temurin-17
    working_dir: /app
    volumes:
      - ./:/app
    command: mvn test
```

Puis lance :

```bash
docker compose run --rm test
```

Si tu veux, je te donne maintenant une **version avec README.md complet**, prêt à distribuer aux étudiants, sans rien oublier cette fois.