$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$garminRoot = Split-Path -Parent $projectRoot
$sdkRoot = Join-Path $garminRoot "connectiq-sdk-win-9.2.0-2026-06-09-92a1605b2"
$compiler = Join-Path $sdkRoot "bin\monkeyc.bat"
$developerKey = Join-Path $garminRoot "garmin_developer_key"
$jungleFile = Join-Path $projectRoot "monkey.jungle"
$outputDirectory = Join-Path $projectRoot "bin"
$applicationPackage = Join-Path $outputDirectory "Notebook.iq"

if (!(Test-Path -LiteralPath $compiler -PathType Leaf)) {
    throw "Connect IQ compiler not found at $compiler"
}
if (!(Test-Path -LiteralPath $developerKey -PathType Leaf)) {
    throw "Garmin developer key not found at $developerKey"
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

& $compiler -e -r -f $jungleFile -o $applicationPackage -y $developerKey -l 3 -w
exit $LASTEXITCODE
