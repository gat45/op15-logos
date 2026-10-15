package com.op15.logos

/**
 * Ported with the user's authorization from their local PhiloHarness project:
 * `risk/RiskGate.kt`. It controls proposed actions; it never executes them.
 */
enum class Reversibility { REVERSIBLE, PARTIALLY_REVERSIBLE, IRREVERSIBLE }
data class ProposedAction(val description: String, val reversibility: Reversibility, val modelConfidence: Double)
sealed interface RiskVerdict {
    data object Allow : RiskVerdict
    data class RequireConfirmation(val reason: String) : RiskVerdict
    data class Block(val reason: String) : RiskVerdict
}

class RiskGate(
    private val irreversibleConfidenceThreshold: Double = 0.85,
    private val partiallyReversibleConfidenceThreshold: Double = 0.60,
) {
    fun assess(action: ProposedAction): RiskVerdict = when (action.reversibility) {
        Reversibility.REVERSIBLE -> RiskVerdict.Allow
        Reversibility.PARTIALLY_REVERSIBLE -> if (action.modelConfidence >= partiallyReversibleConfidenceThreshold) RiskVerdict.Allow
        else RiskVerdict.RequireConfirmation("Action partiellement réversible : confiance insuffisante.")
        Reversibility.IRREVERSIBLE -> if (action.modelConfidence >= irreversibleConfidenceThreshold) {
            RiskVerdict.RequireConfirmation("Action irréversible : confirmation humaine obligatoire.")
        } else RiskVerdict.Block("Action irréversible bloquée : confiance insuffisante.")
    }
}
