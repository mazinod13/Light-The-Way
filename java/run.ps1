# Light the Way (JavaFX) — compile & run.
# Finds a JavaFX-bundled JDK (a "*-fx-*" build) or falls back to JAVA_HOME.
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

$jdk = $null
$cands = Get-ChildItem 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue |
         Where-Object { $_.Name -match 'fx' -and (Test-Path "$($_.FullName)\bin\javac.exe") }
if ($cands) { $jdk = $cands[0].FullName }
elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) { $jdk = $env:JAVA_HOME }

if (-not $jdk) {
    Write-Error "No JavaFX-enabled JDK found. Install one (e.g. Azul Zulu FX) or set JAVA_HOME, then re-run."
    exit 1
}
Write-Host "Using JDK: $jdk"

$javac = "$jdk\bin\javac.exe"
$java  = "$jdk\bin\java.exe"
$out   = Join-Path $here 'out'
$res   = Join-Path $here 'resources'
New-Item -ItemType Directory -Force -Path $out | Out-Null

$srcs = Get-ChildItem (Join-Path $here 'src') -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $javac --add-modules javafx.controls,javafx.media -d $out $srcs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& $java --add-modules javafx.controls,javafx.media -cp "$out;$res" lighttheway.App
