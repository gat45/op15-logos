package com.op15.logos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorVerifierTest {
    @Test fun respects_precedence_and_parentheses() {
        assertEquals(7.0, CalculatorVerifier.evaluate("7 * (3 + 2) / 5").getOrThrow().value, 0.0)
    }

    @Test fun refuses_unsafe_or_invalid_expressions() {
        assertTrue(CalculatorVerifier.evaluate("1 / 0").isFailure)
        assertTrue(CalculatorVerifier.evaluate("Runtime.getRuntime()").isFailure)
    }
}
