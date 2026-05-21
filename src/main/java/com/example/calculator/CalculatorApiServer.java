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
        // Set CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

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

            double result = switch (operation) {
                case "add"      -> calculator.add(a, b);
                case "subtract" -> calculator.subtract(a, b);
                case "multiply" -> calculator.multiply(a, b);
                case "divide"   -> calculator.divide(a, b);
                default -> {
                    sendJson(exchange, 400, """
                        {
                          "error": "Opération inconnue. Utiliser : add, subtract, multiply, divide"
                        }
                        """);
                    yield Double.NaN;
                }
            };

            if (Double.isNaN(result)) return;


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
                params.put(keyValue[0], keyValue[1]);
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