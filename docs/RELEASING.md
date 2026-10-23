# Jak wydać nową wersję / How to release

## Jednorazowo: klucz podpisu
1. Zainstaluj JDK 17 i ustaw `JAVA_HOME`.
2. W katalogu repozytorium uruchom:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\create-keystore.ps1
   ```
3. Podaj hasło (zapisz je w menedżerze haseł) i swój pseudonim.
4. **Zrób kopię** pliku `%USERPROFILE%\.linartv\linartv-release.jks` (np. pendrive). Bez klucza i hasła nie wydasz aktualizacji — użytkownicy musieliby odinstalować aplikację.

## Każde wydanie
1. W `app/build.gradle` zwiększ `versionCode` o 1 i ustaw `versionName` (np. `0.1.1`).
2. Ustaw `JAVA_HOME` (JDK 17) i `ANDROID_HOME` (Android SDK), potem:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\build-release.ps1
   ```
3. Na GitHubie: **Releases → Draft a new release**, tag `v0.1.0` (zgodny z `versionName`), dołącz plik `dist\LinarTV.apk` **zawsze pod nazwą `LinarTV.apk`**.

## Kod Downloadera (raz)
Stały link do najnowszego APK:
```
https://github.com/domlin92/linartv-launcher-open/releases/latest/download/LinarTV.apk
```
1. Po opublikowaniu pierwszego wydania otwórz ten link w przeglądarce i sprawdź, że pobiera się APK.
2. Wejdź na https://aftv.news, wklej link i utwórz skrót — dostaniesz numeryczny kod.
3. Wpisz kod w README w miejsce `KOD`. Kod pozostaje ważny dla kolejnych wydań, o ile plik nazywa się `LinarTV.apk`.
