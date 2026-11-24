$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
if (Test-Path SHA256SUMS) {
    foreach ($line in Get-Content SHA256SUMS) {
        if ($line -match '^([a-f0-9]{64})  (.+)$') {
            $expected = $Matches[1]
            $filename = $Matches[2]
            $stream = [IO.File]::OpenRead((Join-Path (Get-Location).Path $filename))
            $algorithm = [Security.Cryptography.SHA256]::Create()
            try { $actual = [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-','').ToLowerInvariant() }
            finally { $stream.Dispose(); $algorithm.Dispose() }
            if ($actual -ne $expected) { throw "Checksum mismatch: $filename" }
        }
    }
}
if (-not (Test-Path .env)) {
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $values = @('HTTP_PORT=8080','BIND_ADDRESS=127.0.0.1')
    try {
        foreach ($key in @('MYSQL_ROOT_PASSWORD','RUNTIME_DB_PASSWORD','DEMO_PASSWORD','APP_AUTH_SECRET')) {
            $bytes = New-Object byte[] 24
            $rng.GetBytes($bytes)
            $values += $key + '=' + [BitConverter]::ToString($bytes).Replace('-','').ToLowerInvariant()
        }
    } finally { $rng.Dispose() }
    [IO.File]::WriteAllLines((Join-Path (Get-Location) '.env'), $values, (New-Object Text.UTF8Encoding($false)))
}
if (Test-Path images.tar) {
    docker load -i images.tar
    if ($LASTEXITCODE -ne 0) { throw 'Image import failed' }
}
docker compose -f compose.release.yaml up -d --wait --wait-timeout 300
if ($LASTEXITCODE -ne 0) { throw 'Startup failed; inspect docker compose logs' }
docker compose -f compose.release.yaml ps
Write-Host 'Services are ready. Obtain the login password from .env locally.'
