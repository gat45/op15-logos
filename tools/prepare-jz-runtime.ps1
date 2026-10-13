param(
    [Parameter(Mandatory = $true)][string]$JzRuntimeDir,
    [Parameter(Mandatory = $true)][string]$JniDir
)

$ErrorActionPreference = "Stop"
$requiredJz = @(
    "libllama.so", "libggml.so", "libggml-base.so", "libggml-cpu.so",
    "libggml-hexagon.so", "libggml-htp-v81.so"
)
$missing = $requiredJz | Where-Object { -not (Test-Path (Join-Path $JzRuntimeDir $_)) }
if ($missing) { throw "Runtime JZ incomplet : $($missing -join ', ')" }
if (-not (Test-Path (Join-Path $JniDir "libllama-jni.so"))) { throw "Pont JNI absent : libllama-jni.so" }

Write-Host "Runtime JZ prêt à être soumis au build :"
$requiredJz | ForEach-Object {
    $file = Get-Item (Join-Path $JzRuntimeDir $_)
    Write-Host ("{0} {1} bytes SHA256={2}" -f $file.Name, $file.Length, (Get-FileHash $file -Algorithm SHA256).Hash)
}
$jni = Get-Item (Join-Path $JniDir "libllama-jni.so")
Write-Host ("{0} {1} bytes SHA256={2}" -f $jni.Name, $jni.Length, (Get-FileHash $jni -Algorithm SHA256).Hash)
Write-Host "Commande de build (ne prouve pas une session HTP) :"
Write-Host ("gradlew :app:assembleDebug -PlogosNativeDirs=`"{0};{1}`"" -f $JzRuntimeDir, $JniDir)

