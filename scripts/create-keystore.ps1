# Creates the private signing key for LinarTV releases. Run it ONCE.
#
#   powershell -ExecutionPolicy Bypass -File scripts\create-keystore.ps1
#
# The key is stored outside the repository (in %USERPROFILE%\.linartv) and
# keystore.properties is ignored by Git. Back up BOTH the .jks file and the
# password (e.g. in a password manager). Without them you can never publish
# an update that installs over an existing LinarTV installation.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$keyDir = Join-Path $env:USERPROFILE '.linartv'
$keystore = Join-Path $keyDir 'linartv-release.jks'
$properties = Join-Path $root 'keystore.properties'
$alias = 'linartv'

if (Test-Path $keystore) {
    throw "Klucz już istnieje: $keystore. Nie nadpisuję go - zrób kopię i używaj istniejącego."
}

$keytool = $null
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
    if (Test-Path $candidate) { $keytool = $candidate }
}
if (-not $keytool) {
    $found = Get-Command keytool -ErrorAction SilentlyContinue
    if ($found) { $keytool = $found.Source }
}
if (-not $keytool) { throw 'Nie znaleziono keytool. Ustaw JAVA_HOME na katalog JDK 17.' }

Write-Host 'Hasło do klucza: co najmniej 8 znaków. Zapisz je w menedżerze haseł.'
$first = Read-Host 'Hasło' -AsSecureString
$second = Read-Host 'Powtórz hasło' -AsSecureString
$plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($first))
$check = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($second))
if ($plain -ne $check) { throw 'Hasła się różnią.' }
if ($plain.Length -lt 8) { throw 'Hasło jest za krótkie.' }
$name = Read-Host 'Imię i nazwisko lub pseudonim autora (np. domlin92)'

New-Item -ItemType Directory -Force -Path $keyDir | Out-Null
& $keytool -genkeypair -v -storetype PKCS12 -keystore $keystore -alias $alias `
    -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $plain -keypass $plain -dname "CN=$name, O=LinarTV, C=PL"
if ($LASTEXITCODE -ne 0) { throw 'keytool zakończył się błędem.' }

$storePath = $keystore.Replace('\', '/')
@"
storeFile=$storePath
storePassword=$plain
keyAlias=$alias
keyPassword=$plain
"@ | Set-Content -Encoding ASCII $properties

Write-Host ''
Write-Host "Gotowe. Klucz: $keystore"
Write-Host "Ustawienia podpisu: $properties (ignorowane przez Git)"
Write-Host 'ZRÓB TERAZ KOPIĘ pliku .jks i hasła - np. na pendrive i w menedżerze haseł.'
