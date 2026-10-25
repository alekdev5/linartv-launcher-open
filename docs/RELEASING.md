# Jak wydać nową wersję / How to release

## Polski

### Jednorazowo: klucz podpisu
1. Zainstaluj JDK 17 i ustaw `JAVA_HOME`.
2. W katalogu repozytorium uruchom:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\create-keystore.ps1
   ```
3. Podaj hasło (zapisz je w menedżerze haseł) i swój pseudonim.
4. **Zrób kopię** pliku `%USERPROFILE%\.linartv\linartv-release.jks` (np. na pendrive). Bez klucza i hasła nie wydasz aktualizacji — użytkownicy musieliby odinstalować aplikację.

### Każde wydanie
1. W `app/build.gradle` zwiększ `versionCode` o 1 i ustaw `versionName` zgodnie z [semver](https://semver.org/lang/pl/): poprawki → `1.0.1`, nowe funkcje → `1.1.0`, zmiany niezgodne wstecz → `2.0.0`. Pierwsze wydanie: `versionCode 1`, `versionName 1.0.0`.
2. Ustaw `JAVA_HOME` (JDK 17) i `ANDROID_HOME` (Android SDK), potem:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\build-release.ps1
   ```
3. Na GitHubie: **Releases → Draft a new release**, tag `vX.Y.Z` zgodny z `versionName` (np. `v1.0.0`), dołącz plik `dist\LinarTV.apk` **zawsze pod nazwą `LinarTV.apk`**.

### Kod Downloadera (raz)
Stały link do najnowszego APK:
```
https://github.com/domlin92/linartv-launcher-open/releases/latest/download/LinarTV.apk
```
1. Po opublikowaniu pierwszego wydania otwórz ten link w przeglądarce i sprawdź, że pobiera się APK.
2. Wejdź na https://aftv.news, wklej link i utwórz skrót — dostaniesz numeryczny kod.
3. Wpisz kod w README w miejsce informacji o przygotowaniu. Kod pozostaje ważny dla kolejnych wydań, o ile plik nazywa się `LinarTV.apk`.

## English

### Once: signing key
1. Install JDK 17 and set `JAVA_HOME`.
2. From the repository root run:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\create-keystore.ps1
   ```
3. Enter a password (keep it in a password manager) and your name or nickname.
4. **Back up** `%USERPROFILE%\.linartv\linartv-release.jks` (e.g. on a USB stick). Without the key and password you cannot publish updates — users would have to uninstall the app.

### Every release
1. In `app/build.gradle`, increase `versionCode` by 1 and set `versionName` following [semver](https://semver.org/): fixes → `1.0.1`, new features → `1.1.0`, breaking changes → `2.0.0`. First release: `versionCode 1`, `versionName 1.0.0`.
2. Set `JAVA_HOME` (JDK 17) and `ANDROID_HOME` (Android SDK), then:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\build-release.ps1
   ```
3. On GitHub: **Releases → Draft a new release**, tag `vX.Y.Z` matching `versionName` (e.g. `v1.0.0`), attach `dist\LinarTV.apk` **always named `LinarTV.apk`**.

### Downloader code (once)
Permanent link to the latest APK:
```
https://github.com/domlin92/linartv-launcher-open/releases/latest/download/LinarTV.apk
```
1. After the first release, open the link in a browser and check that the APK downloads.
2. Go to https://aftv.news, paste the link and create a short code.
3. Put the code in the README instead of the "being prepared" note. The code stays valid for later releases as long as the file is named `LinarTV.apk`.
