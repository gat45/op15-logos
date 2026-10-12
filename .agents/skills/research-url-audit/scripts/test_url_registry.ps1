param(
    [string]$RegistryPath = (Join-Path (Get-Location) "docs/URL_REGISTRY.csv"),
    [int]$TimeoutSeconds = 12,
    [int]$StartIndex = 0,
    [int]$Count = 10
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $RegistryPath)) {
    throw "Registre introuvable : $RegistryPath"
}

$rows = @(Import-Csv -LiteralPath $RegistryPath)
if ($StartIndex -lt 0 -or $StartIndex -ge $rows.Count) { throw "StartIndex hors plage : $StartIndex" }
$endIndex = [Math]::Min($StartIndex + $Count, $rows.Count)
for ($index = $StartIndex; $index -lt $endIndex; $index++) {
    $row = $rows[$index]
    $row | Add-Member -NotePropertyName availability -NotePropertyValue "" -Force
    $row | Add-Member -NotePropertyName http_code -NotePropertyValue "" -Force
    $row | Add-Member -NotePropertyName final_url -NotePropertyValue "" -Force
    $row | Add-Member -NotePropertyName checked_on -NotePropertyValue "" -Force
    $row | Add-Member -NotePropertyName availability_note -NotePropertyValue "" -Force
    try {
        $response = Invoke-WebRequest -Uri $row.url -Method Head -MaximumRedirection 5 -TimeoutSec $TimeoutSeconds -UseBasicParsing -Headers @{ "User-Agent" = "OP15-Logos-Research-Audit/0.1" } -SkipHttpErrorCheck
        if ($response.StatusCode -eq 405) {
            $response = Invoke-WebRequest -Uri $row.url -Method Get -MaximumRedirection 5 -TimeoutSec $TimeoutSeconds -UseBasicParsing -Headers @{ "User-Agent" = "OP15-Logos-Research-Audit/0.1" } -SkipHttpErrorCheck
        }
        $row.http_code = [string]$response.StatusCode
        $row.final_url = $response.BaseResponse.ResponseUri.AbsoluteUri
        $row.availability = if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) { "reachable" } else { "http_error" }
    } catch {
        $row.availability = "unreachable"
        $row.availability_note = $_.Exception.Message.Replace("`r", " ").Replace("`n", " ").Substring(0, [Math]::Min(180, $_.Exception.Message.Length))
    }
    $row.checked_on = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
}

$rows | Export-Csv -LiteralPath $RegistryPath -NoTypeInformation -Encoding utf8
$summary = $rows[$StartIndex..($endIndex - 1)] | Group-Object availability | ForEach-Object { "$($_.Name)=$($_.Count)" }
Write-Host "Lot $StartIndex..$($endIndex - 1) : $($summary -join ', ')"
