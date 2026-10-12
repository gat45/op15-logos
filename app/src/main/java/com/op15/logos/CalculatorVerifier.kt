package com.op15.logos

/** Deterministic arithmetic verifier for a deliberately small, safe grammar. */
data class CalculationResult(val value: Double)

object CalculatorVerifier {
    fun evaluate(expression: String): Result<CalculationResult> = runCatching {
        require(expression.length <= 200) { "Expression trop longue." }
        require(expression.matches(Regex("[0-9+*/().\\s-]+"))) { "Seuls nombres, parenthèses et + - * / sont acceptés." }
        val parser = Parser(expression)
        val value = parser.expression()
        parser.finish()
        require(value.isFinite()) { "Résultat non fini." }
        CalculationResult(value)
    }

    private class Parser(private val input: String) {
        private var index = 0
        fun expression(): Double {
            var value = term()
            while (true) {
                when (token()) { '+' -> { index++; value += term() }; '-' -> { index++; value -= term() }; else -> return value }
            }
        }
        private fun term(): Double {
            var value = factor()
            while (true) {
                when (token()) { '*' -> { index++; value *= factor() }; '/' -> { index++; val divisor = factor(); require(divisor != 0.0) { "Division par zéro." }; value /= divisor }; else -> return value }
            }
        }
        private fun factor(): Double = when (token()) {
            '-' -> { index++; -factor() }
            '(' -> { index++; val value = expression(); require(token() == ')') { "Parenthèse fermante attendue." }; index++; value }
            else -> number()
        }
        private fun number(): Double {
            skip(); val start = index
            while (index < input.length && (input[index].isDigit() || input[index] == '.')) index++
            require(start != index) { "Nombre attendu à la position $index." }
            return input.substring(start, index).toDouble()
        }
        private fun token(): Char? { skip(); return input.getOrNull(index) }
        private fun skip() { while (input.getOrNull(index)?.isWhitespace() == true) index++ }
        fun finish() { require(token() == null) { "Caractère inattendu à la position $index." } }
    }
}
