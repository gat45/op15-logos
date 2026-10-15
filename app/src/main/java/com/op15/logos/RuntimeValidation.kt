package com.op15.logos

/**
 * Ported with the user's authorization from their local PhiloHarness project:
 * `resource/DeviceProfile.kt` and `resource/AcceleratorValidator.kt`.
 * A backend is not considered usable merely because it initialized.
 */
enum class LogosBackend { CPU, HEXAGON_NPU }

sealed interface BackendValidation {
    data class Validated(val agreementRatio: Double, val speedupVsCpu: Double) : BackendValidation
    data class Rejected(val reason: String, val agreementRatio: Double?) : BackendValidation
    data object NotYetTested : BackendValidation
}

interface ReferenceTask {
    fun run(backend: LogosBackend): FloatArray
}

class AcceleratorValidator(private val minAgreementRatio: Double = 0.90) {
    fun validate(backend: LogosBackend, tasks: List<ReferenceTask>): BackendValidation {
        if (backend == LogosBackend.CPU) return BackendValidation.Validated(1.0, 1.0)
        if (tasks.isEmpty()) return BackendValidation.Rejected("Aucune tâche CPU de référence.", null)
        val agreed = tasks.count { task -> outputsAgree(task.run(LogosBackend.CPU), task.run(backend)) }
        val ratio = agreed.toDouble() / tasks.size
        return if (ratio >= minAgreementRatio) BackendValidation.Validated(ratio, 1.0)
        else BackendValidation.Rejected("Accord CPU insuffisant : ${"%.1f".format(ratio * 100)}%.", ratio)
    }

    private fun outputsAgree(reference: FloatArray, candidate: FloatArray, tolerance: Float = 1e-2f): Boolean =
        reference.size == candidate.size && reference.indices.all { index -> kotlin.math.abs(reference[index] - candidate[index]) <= tolerance }
}
