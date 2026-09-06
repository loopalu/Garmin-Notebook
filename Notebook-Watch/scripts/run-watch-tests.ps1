param(
    [switch]$StartSimulator
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$garminRoot = Split-Path -Parent $projectRoot
$sdkRoot = Join-Path $garminRoot "connectiq-sdk-win-9.2.0-2026-06-09-92a1605b2"
$compiler = Join-Path $sdkRoot "bin\monkeyc.bat"
$runner = Join-Path $sdkRoot "bin\monkeydo.bat"
$simulator = Join-Path $sdkRoot "bin\simulator.exe"
$key = Join-Path $garminRoot "garmin_developer_key"
$testProgram = Join-Path $projectRoot "bin\Notebook-tests.prg"

$jungles = (Join-Path $projectRoot "monkey.jungle") + ";" + (Join-Path $projectRoot "tests.jungle")
& $compiler -f $jungles -o $testProgram -y $key -d enduro3 -t -l 3 -w
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if ($StartSimulator) {
    $runningSimulator = Get-Process -Name "simulator" -ErrorAction SilentlyContinue
    if ($null -eq $runningSimulator) {
        Start-Process -FilePath $simulator
        $deadline = (Get-Date).AddSeconds(30)
        do {
            Start-Sleep -Milliseconds 500
            $runningSimulator = Get-Process -Name "simulator" -ErrorAction SilentlyContinue
        } while ($null -eq $runningSimulator -and (Get-Date) -lt $deadline)

        if ($null -eq $runningSimulator) {
            throw "Connect IQ simulator did not start within 30 seconds"
        }
        Start-Sleep -Seconds 2
    }
}

& $runner $testProgram enduro3 /t
exit $LASTEXITCODE
