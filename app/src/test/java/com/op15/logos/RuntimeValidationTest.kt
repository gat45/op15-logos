package com.op15.logos

import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeValidationTest {
    @Test fun `rejects an HTP result that differs from CPU`() {
        val task = object : ReferenceTask {
            override fun run(backend: LogosBackend) = if (backend == LogosBackend.CPU) floatArrayOf(1f, 2f) else floatArrayOf(1f, 9f)
        }
        assertTrue(AcceleratorValidator().validate(LogosBackend.HEXAGON_NPU, listOf(task)) is BackendValidation.Rejected)
    }

    @Test fun `irreversible action always needs confirmation or blocks`() {
        assertTrue(RiskGate().assess(ProposedAction("supprimer", Reversibility.IRREVERSIBLE, 0.9)) is RiskVerdict.RequireConfirmation)
        assertTrue(RiskGate().assess(ProposedAction("supprimer", Reversibility.IRREVERSIBLE, 0.2)) is RiskVerdict.Block)
    }
}
