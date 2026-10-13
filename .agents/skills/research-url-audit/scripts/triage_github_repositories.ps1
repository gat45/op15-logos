param(
    [string]$RegistryPath = (Join-Path (Get-Location) "docs/URL_REGISTRY.csv"),
    [string]$OutputPath = (Join-Path (Get-Location) "docs/GITHUB_TRIAGE.csv"),
    [int]$PauseMilliseconds = 350
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $RegistryPath)) { throw "Registre introuvable : $RegistryPath" }

# Preserve a successful previous collection if the anonymous GitHub API is
# rate-limited. The cache is metadata, not a semantic audit result.
$cached = @{}
if (Test-Path -LiteralPath $OutputPath) {
    Import-Csv -LiteralPath $OutputPath | ForEach-Object {
        if ($_.triage_status -in @("metadata_collected", "metadata_cached")) { $cached[$_.url] = $_ }
    }
}
try {
    $repoRoot = (git rev-parse --show-toplevel 2>$null).Trim()
    if ($repoRoot) {
        $relativeOutput = [IO.Path]::GetRelativePath($repoRoot, (Resolve-Path -LiteralPath $OutputPath)) -replace '\\', '/'
        $baseline = git -C $repoRoot show "HEAD:$relativeOutput" 2>$null | ConvertFrom-Csv
        $baseline | ForEach-Object {
            if ($_.triage_status -in @("metadata_collected", "metadata_cached") -and -not $cached.ContainsKey($_.url)) { $cached[$_.url] = $_ }
        }
    }
} catch { }

$repositories = Import-Csv -LiteralPath $RegistryPath | Where-Object {
    $_.domain -eq "github.com" -and $_.url -match '^https://github\.com/[^/]+/[^/#?]+/?$'
} | Sort-Object url -Unique

$headers = @{ "Accept" = "application/vnd.github+json"; "User-Agent" = "OP15-Logos-Research-Audit/0.1" }
$results = foreach ($row in $repositories) {
    $repo = ([uri]$row.url).AbsolutePath.Trim('/')
    try {
        $data = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo" -Headers $headers -Method Get
        [pscustomobject]@{
            url = $row.url
            repository = $data.full_name
            owner = $data.owner.login
            description = $data.description
            license = if ($null -eq $data.license) { "" } else { $data.license.spdx_id }
            default_branch = $data.default_branch
            archived = [string]$data.archived
            disabled = [string]$data.disabled
            pushed_at = $data.pushed_at
            stars = [string]$data.stargazers_count
            open_issues = [string]$data.open_issues_count
            registry_status = $row.status
            triage_status = "metadata_collected"
            error = ""
        }
    } catch {
        $message = $_.Exception.Message.Replace("`r", " ").Replace("`n", " ").Substring(0, [Math]::Min(180, $_.Exception.Message.Length))
        if ($cached.ContainsKey($row.url)) {
            $previous = $cached[$row.url]
            [pscustomobject]@{
                url = $row.url; repository = $previous.repository; owner = $previous.owner; description = $previous.description; license = $previous.license; default_branch = $previous.default_branch;
                archived = $previous.archived; disabled = $previous.disabled; pushed_at = $previous.pushed_at; stars = $previous.stars; open_issues = $previous.open_issues;
                registry_status = $row.status; triage_status = "metadata_cached"; error = "Dernier contrôle indisponible : $message"
            }
        } else {
            [pscustomobject]@{
                url = $row.url; repository = $repo; owner = ""; description = ""; license = ""; default_branch = "";
                archived = ""; disabled = ""; pushed_at = ""; stars = ""; open_issues = "";
                registry_status = $row.status; triage_status = "metadata_unavailable"; error = $message
            }
        }
    }
    Start-Sleep -Milliseconds $PauseMilliseconds
}

$results | Export-Csv -LiteralPath $OutputPath -NoTypeInformation -Encoding utf8
$summary = $results | Group-Object triage_status | ForEach-Object { "$($_.Name)=$($_.Count)" }
Write-Host "Triage GitHub : $($summary -join ', ') -> $OutputPath"
