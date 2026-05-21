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