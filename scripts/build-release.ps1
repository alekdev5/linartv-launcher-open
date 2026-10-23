# Builds a signed LinarTV release APK into dist\LinarTV.apk.
#
#   powershell -ExecutionPolicy Bypass -File scripts\build-release.ps1
#
# Requires: JDK 17 (JAVA_HOME), Android SDK (ANDROID_HOME), keystore.properties
# created by scripts\create-keystore.ps1, and Python for the public audit.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path 'keystore.properties')) {
    throw 'Brak keystore.properties. Najpierw uruchom scripts\create-keystore.ps1.'
}
if (-not $env:JAVA_HOME) { throw 'Ustaw JAVA_HOME na katalog JDK 17.' }
if (-not $env:ANDROID_HOME) { throw 'Ustaw ANDROID_HOME na katalog Android SDK.' }

Write-Host '1/3 Audyt prywatnych danych...'
python scripts\audit_public.py
if ($LASTEXITCODE -ne 0) { throw 'Audyt nie przeszedł - popraw zgłoszone pliki przed wydaniem.' }

Write-Host '2/3 Budowanie podpisanego APK...'
.\gradlew.bat clean :app:assembleRelease --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Budowanie nie powiodło się.' }

$apk = Get-ChildItem 'app\build\outputs\apk\release\*.apk' | Select-Object -First 1
if (-not $apk) { throw 'Nie znaleziono APK w app\build\outputs\apk\release.' }
if ($apk.Name -like '*unsigned*') { throw 'APK nie jest podpisany - sprawdź keystore.properties.' }

Write-Host '3/3 Kopiowanie do dist...'
New-Item -ItemType Directory -Force -Path 'dist' | Out-Null
$versioned = Join-Path 'dist' $apk.Name
Copy-Item $apk.FullName $versioned -Force
Copy-Item $apk.FullName 'dist\LinarTV.apk' -Force
$hash = (Get-FileHash 'dist\LinarTV.apk' -Algorithm SHA256).Hash

Write-Host ''
Write-Host "Gotowe: dist\LinarTV.apk  (kopia: $versioned)"
Write-Host "SHA-256: $hash"
Write-Host 'Dołącz plik dist\LinarTV.apk do wydania na GitHubie pod tą samą nazwą,'
Write-Host 'aby link .../releases/latest/download/LinarTV.apk i kod Downloadera działały dalej.'
