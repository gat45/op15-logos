param(
    [string]$RegistryPath = (Join-Path (Get-Location) "docs/URL_REGISTRY.csv"),
    [string]$OutputPath = (Join-Path (Get-Location) "docs/GITHUB_TRIAGE.csv"),
    [int]$PauseMilliseconds = 350
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $RegistryPath)) { throw "Registre introuvable : $RegistryPath" }

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
        [pscustomobject]@{
            url = $row.url; repository = $repo; owner = ""; description = ""; license = ""; default_branch = "";
            archived = ""; disabled = ""; pushed_at = ""; stars = ""; open_issues = "";
            registry_status = $row.status; triage_status = "metadata_unavailable"; error = $_.Exception.Message.Replace("`r", " ").Replace("`n", " ").Substring(0, [Math]::Min(180, $_.Exception.Message.Length))
        }
    }
    Start-Sleep -Milliseconds $PauseMilliseconds
}

$results | Export-Csv -LiteralPath $OutputPath -NoTypeInformation -Encoding utf8
$summary = $results | Group-Object triage_status | ForEach-Object { "$($_.Name)=$($_.Count)" }
Write-Host "Triage GitHub : $($summary -join ', ') -> $OutputPath"
