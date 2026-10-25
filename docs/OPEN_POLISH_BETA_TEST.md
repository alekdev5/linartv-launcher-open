# Polska wersja testowa — 25.10.2026

Środowisko: BlueStacks „LinarTV Test Env”, Android 11, 1920×1080. Testy nie wymagały instalowania tej wersji na fizycznym telewizorze.

## Herby

- 54 lokalne pliki: po 18 dla Ekstraklasy, I ligi i III ligi grupy IV.
- `scripts/import-club-crests.ps1` korzysta z katalogów `club-crests/i-liga` i `club-crests/iii-liga-gr-iv`, obsługuje uporządkowane nazwy plików I ligi oraz UTF-8.
- Ponowny import po zmianie nazw źródłowych: 36 herbów sprawdzonych przez SHA-256, bez zmiany grafiki.
- I liga ma własny klucz zestawu `i_liga`; inne ligi nie korzystają już omyłkowo z mapowania III ligi.
- Duże źródłowe PNG Ruchu i Puszczy (ponad 8000 px) są wczytywane z próbkowaniem do maksymalnie 512 px. Oryginały pozostają nietknięte.
- Na pulpicie emulatora wybrano Arkę Gdynia i KSZO. Widoczne, poprawne herby: Arka, Stal Rzeszów, Wisłoka i KSZO.

## Pogoda

- Prognoza godzinowa, siedmiodniowa, bieżąca analiza/model oraz ikony pochodzą z IMGW dla współrzędnych wybranego miasta.
- W Open warunki teraz pochodzą z najbliższego kroku modelu IMGW, bez przypisywania każdemu miastu pomiaru stacji Kraków-Balice. Ciśnienie Pa jest przeliczane na hPa; opad ma opis okresu 10 minut.
- Wyszukiwanie współrzędnych miast pozostało w geokodowaniu Open-Meteo/GeoNames, z filtrem Polski. Prognoza Open-Meteo została usunięta z aplikacji.
- W emulatorze potwierdzono ikony IMGW, temperaturę i komplet metryk, siedem różnych dni (25–31.10), datę danych, wschód/zachód i podpis IMGW-PIB.
- Stary cache prognozy Open-Meteo jest odrzucany. Współrzędne zapisane poza obsługiwanym obszarem są zastępowane Krakowem.

## Terminarze i kontrola

- Kreator pokazał dokładnie siedem lig: Ekstraklasa, I, II i cztery grupy III ligi.
- W aplikacji brak kodu lub adresów pobierania fixtur.es. Zapisane wcześniej nieobsługiwane kalendarze nie są odświeżane.
- Kompilacja debug, 7206 testów czasu/wygaszacza, 36 porównań rozmycia i 21 testów zakresu kalendarzy: OK. Audyt publiczny: OK.
- Końcowa kontrola AndroidRuntime podczas testu herbów: brak błędów.

Zrzuty do oceny: `tmp/emulator-crests-imgw.png` i `tmp/emulator-imgw-details.png` (ignorowane przez Git). Commit i push czekają na potwierdzenie użytkownika.

## Kandydat na źródło II ligi

Na stronie https://www.drugaliga.org/ odnaleziono bezpośrednie odnośniki `/documents/883582/...` do herbów, m.in. Znicza Pruszków, Sandecji, Rekordu, Resovii i Stali Stalowa Wola. Przykładowy plik zwraca `image/svg+xml`. Nie pobierano jeszcze całego zestawu do aplikacji; należy uzgodnić mapowanie z bieżącymi 18 klubami katalogu oraz sposób renderowania SVG.

### Paczka użytkownika II ligi

Właściciel projektu dostarczył kompletny katalog `club-crests/ii-liga`: 17 PNG i 1 WebP. Import przypisuje wszystkie 18 plików do nazw z bieżącego katalogu kalendarzeligowe.pl, w tym Śląsk II Wrocław do `slask-wroclaw.webp`. Dodano osobny klucz zestawu `ii_liga`. Open ma łącznie 72 pliki: 70 PNG i 2 WebP. Oryginalne grafiki są kopiowane bez zmian, z kontrolą SHA-256.

Kompilacja i audyt publiczny: OK. Zaktualizowane APK zainstalowano wyłącznie w „LinarTV Test Env”.

Na pulpicie emulatora potwierdzono herb Śląska II Wrocław (mecz z Polonią Środa). Następnie właściciel zastąpił herby Śląska w paczkach Ekstraklasy i II ligi plikami PNG. Import odzwierciedla tę zmianę; w zasobach Open pozostały 72 PNG, a nieużywane kopie WebP Śląska usunięto. Import preferuje PNG, jeśli obok niego znajduje się starszy wariant WebP. Zrzut do oceny: `tmp/emulator-ii-liga-crests.png`.

## Nazwy rozgrywek i rund

Dla Arka Gdynia–Stal Rzeszów (29.10.2026, id 36809) API podaje `competition.name=Puchar Polski`, `category=POLISH_CUP`, `round.name=1/16 finału` i `roundNumber=3`. Kod wcześniej pomijał nazwę rozgrywek i wyświetlał „I liga - Kolejka 3”. Teraz etykieta pochodzi z danych konkretnego meczu: „Puchar Polski - 1/16 finału”. Numer kolejki jest używany tylko dla kategorii ligowej; gdy runda pucharu nie ma nazwy, pozostaje sama nazwa rozgrywek. Nieznane rozgrywki mają neutralny nagłówek.

Poprawka obejmuje Open i Private. Format cache terminarzy podniesiono do 2; stare wpisy bez metadanych są pomijane i pobierane ponownie. Dodano 10 testów regresji dla lig, pucharów i brakujących informacji.

Weryfikacja: oba APK kompilują się poprawnie; testy lokalne obu projektów przechodzą. Open zainstalowano w „LinarTV Test Env”; po odświeżeniu cache kafelek Arki pokazał „Puchar Polski - 1/16 finału”, a kafelek KSZO zachował „III liga — grupa IV - Kolejka 15”. Brak błędów AndroidRuntime. Zrzut: `tmp/emulator-cup-fixed.png`. Private z tą poprawką nie instalowano na TV.
