param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$Java = 'C:\Program Files\Android\Android Studio\jbr',
    [switch]$SkipBuild
)
$ErrorActionPreference='Stop'
if (-not $SkipBuild) { & "$PSScriptRoot\build.ps1" -Sdk $Sdk -Java $Java }
$latest = Get-ChildItem -LiteralPath "$PSScriptRoot\.build" -Directory -Filter 'build-*' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $latest) { throw 'Run build.ps1 before running tests.' }
$android = Join-Path $Sdk 'platforms\android-34\android.jar'
$classes = Join-Path $latest.FullName 'classes'
$testOut = Join-Path $PSScriptRoot '.build\test-classes'
New-Item -ItemType Directory -Force -Path $testOut | Out-Null
$files = @(Get-ChildItem -LiteralPath "$PSScriptRoot\tests" -Filter '*.java' | ForEach-Object {$_.FullName})
& "$Java\bin\javac.exe" --release 8 -classpath "$classes;$android" -d $testOut @files
if ($LASTEXITCODE -ne 0) {throw 'Test compilation failed.'}
foreach ($name in @('SignalQualityTest','LabFeaturesTest','BandScannerTest','BandPlanTest','BackendRegistryTest','ReceptionTrialTest','AudioMeterTest','StabilityTest')) {
    & "$Java\bin\java.exe" -classpath "$testOut;$classes;$android" "br.local.fmlab.$name"
    if ($LASTEXITCODE -ne 0) {throw "Test failed: $name"}
}
