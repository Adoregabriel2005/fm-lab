param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$Java = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$BuildTools = '36.0.0',
    [string]$Platform = 'android-34',
    [string]$KeyStore = ''
)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = $Java
$env:Path = "$Java\bin;$env:Path"
$buildRoot = Join-Path $PSScriptRoot '.build'
$build = Join-Path $buildRoot ('build-' + [guid]::NewGuid().ToString('N'))
$keyPath = if ($KeyStore) { [System.IO.Path]::GetFullPath($KeyStore) } else { Join-Path $buildRoot 'test-key.p12' }
$tools = Join-Path $Sdk "build-tools\$BuildTools"
$android = Join-Path $Sdk "platforms\$Platform\android.jar"
$dist = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Force -Path $dist | Out-Null
$out = Join-Path $dist 'FM-Lab-0.8-teste.apk'
New-Item -ItemType Directory -Force -Path "$build\classes","$build\dex" | Out-Null
$env:TEMP = $build
$env:TMP = $build
if (-not (Test-Path -LiteralPath $android)) { throw "SDK nÃƒÂ£o encontrado em $Sdk. Informe -Sdk com o caminho do Android SDK." }
function Assert-Step { if ($LASTEXITCODE -ne 0) { throw "Ferramenta falhou: cÃƒÂ³digo $LASTEXITCODE" } }
$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
& "$Java\bin\javac.exe" -encoding UTF-8 --release 8 -classpath $android -d "$build\classes" @sources
Assert-Step
& "$Java\bin\jar.exe" cf "$build\classes.jar" -C "$build\classes" .
Assert-Step
& "$tools\d8.bat" --min-api 21 --lib $android --output "$build\dex" "$build\classes.jar"
Assert-Step
& "$tools\aapt.exe" package -f -M "$PSScriptRoot\AndroidManifest.xml" -S "$PSScriptRoot\res" -I $android -F "$build\unsigned.apk"
Assert-Step
Push-Location "$build\dex"
try { & "$tools\aapt.exe" add "$build\unsigned.apk" classes.dex; Assert-Step } finally { Pop-Location }
& "$tools\zipalign.exe" -f 4 "$build\unsigned.apk" "$build\aligned.apk"
Assert-Step
# Local test key. Keep this file for compatible future updates; never use for production.
if (-not (Test-Path -LiteralPath $keyPath)) {
    & "$Java\bin\keytool.exe" -genkeypair -keystore $keyPath -storetype PKCS12 -storepass fmlab-local-test -keypass fmlab-local-test -alias fmlab -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=FM Lab Local Test'
    Assert-Step
}
& "$tools\apksigner.bat" sign --ks $keyPath --ks-key-alias fmlab --ks-pass pass:fmlab-local-test --key-pass pass:fmlab-local-test --v1-signing-enabled true --v2-signing-enabled true --out $out "$build\aligned.apk"
Assert-Step
& "$tools\apksigner.bat" verify --verbose $out
Assert-Step
& "$tools\aapt.exe" dump badging $out
Assert-Step
Write-Output "APK gerado: $out"

