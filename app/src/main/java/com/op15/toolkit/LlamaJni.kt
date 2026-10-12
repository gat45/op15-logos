package com.op15.toolkit

/**
 * Minimal ABI adapter for the audited OP15 llama JNI bridge.
 *
 * The native symbols use this package name. This class deliberately exposes
 * loading and inference as separate operations so a missing or incompatible
 * runtime is reported instead of being mistaken for CPU fallback.
 */
data class NativeRuntimeProbe(val ready: Boolean, val detail: String)

object LlamaJni {
    @Volatile private var librariesLoaded = false

    fun probe(): NativeRuntimeProbe {
        if (librariesLoaded) return NativeRuntimeProbe(true, "Runtime GGUF natif chargé.")
        return runCatching {
            System.loadLibrary("llama")
            System.loadLibrary("ggml-hexagon")
            System.loadLibrary("llama-jni")
            librariesLoaded = true
            NativeRuntimeProbe(true, "Runtime GGUF natif chargé.")
        }.getOrElse { NativeRuntimeProbe(false, "Runtime natif indisponible : ${it.message ?: it.javaClass.simpleName}") }
    }

    fun initialize(nativeLibraryDir: String): NativeRuntimeProbe {
        val state = probe()
        if (!state.ready) return state
        return if (initNative(nativeLibraryDir)) NativeRuntimeProbe(true, "Backend initialisé.")
        else NativeRuntimeProbe(false, "Le backend natif a refusé l'initialisation.")
    }

    fun loadModel(path: String): Int = loadNative(path)
    fun isModelLoaded(): Boolean = isLoadedNative()
    fun begin(prompt: String, predictLength: Int): Int = processUserPromptNative(prompt, predictLength)
    fun nextToken(): String? = generateNextTokenNative()
    fun hadGenerationError(): Boolean = hadGenerationErrorNative()
    fun unload() = unloadNative()

    private external fun initNative(nativeLibDir: String): Boolean
    private external fun loadNative(modelPath: String): Int
    private external fun isLoadedNative(): Boolean
    private external fun processUserPromptNative(prompt: String, predictLength: Int): Int
    private external fun generateNextTokenNative(): String?
    private external fun hadGenerationErrorNative(): Boolean
    private external fun unloadNative()
}
