param(
    [string]$DocsDirectory = (Join-Path (Get-Location) "docs"),
    [string]$OutputPath = (Join-Path (Get-Location) "docs/URL_REGISTRY.csv")
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $DocsDirectory)) {
    throw "Documentation introuvable : $DocsDirectory"
}

$existing = @{}
if (Test-Path -LiteralPath $OutputPath) {
    Import-Csv -LiteralPath $OutputPath | ForEach-Object { $existing[$_.url] = $_ }
}

$pattern = 'https?://[^\s\)\]}`>"'']+'
$urls = [ordered]@{}
Get-ChildItem -LiteralPath $DocsDirectory -Filter *.md -File | ForEach-Object {
    $source = $_.Name
    $lineNumber = 0
    Get-Content -LiteralPath $_.FullName | ForEach-Object {
        $lineNumber++
        foreach ($match in [regex]::Matches($_, $pattern)) {
            $url = $match.Value.TrimEnd('.', ',', ';', ':')
            if (-not $urls.Contains($url)) {
                try { $domain = ([uri]$url).Host } catch { $domain = "invalid" }
                $previous = $existing[$url]
                $urls[$url] = [pscustomobject]@{
                    url = $url
                    domain = $domain
                    source_file = $source
                    source_line = $lineNumber
                    status = if ($previous) { $previous.status } else { "known" }
                    decision = if ($previous) { $previous.decision } else { "" }
                    notes = if ($previous) { $previous.notes } else { "" }
                    reviewed_on = if ($previous) { $previous.reviewed_on } else { "" }
                }
            }
        }
    }
}

$urls.Values | Export-Csv -LiteralPath $OutputPath -NoTypeInformation -Encoding utf8
Write-Host "Registre mis a jour : $($urls.Count) URL(s) -> $OutputPath"

