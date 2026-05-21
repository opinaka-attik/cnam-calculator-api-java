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