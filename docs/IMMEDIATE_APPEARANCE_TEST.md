# Natychmiastowy podgląd ustawień — 24.10.2026

Przyczyna: `buildScreen()` odkłada odświeżanie pulpitu do zamknięcia całego stosu okien. Dotyczyło to również świadomej zmiany tapety lub trybu wyglądu przez użytkownika.

Poprawka wspólna dla Open i Private:

- Zmiana tapety, trybu wyglądu i harmonogramu wywołuje `applyVisualSettingsNow()`.
- Pulpit jest przebudowywany, otwarty panel ustawień otrzymuje aktualną paletę, a przed rysowaniem nowego pulpitu odtwarzane jest wspólne rozmyte tło.
- Otwarte okna i ich licznik pozostają zachowane. Odświeżenia sieciowe nadal są odkładane, dzięki czemu zwykłe otwieranie podmenu ponownie wykorzystuje rozmycie.

Weryfikacja na Philipsie:

- Kompilacje debug Open i Private: OK; obie aktualizacje wgrane.
- Zmiana na jasny wygląd przy nadal otwartym panelu: OK w obu aplikacjach.
- Open: zmiana tapety sunset → aurora od razu zmienia tło za panelem. Przywrócono sunset.
- Przywrócono automatyczny tryb w obu aplikacjach; brak błędów AndroidRuntime w końcowej kontroli.
- Private: ustawienia i źródła mają teraz półprzezroczystą paletę zgodną z kafelkami pulpitu. Zmiana kolorów dotyczy tylko Private.

Zrzuty testowe znajdują się w ignorowanych folderach `tmp/` obu projektów.

## Rozszerzenie na wszystkie ustawienia

- Zmiana języka, jednostki temperatury, miasta, klubów i czasu wygaszacza oraz usunięcie aktywnej tapety używają tego samego natychmiastowego odświeżenia.
- Wyniki pobierania pogody po zmianie miasta i terminarzy po wyborze klubów lub ręcznym odświeżeniu są stosowane również przy otwartym panelu. Aktualizacje okresowe nadal nie przerywają otwartych okien.
- Pobieranie pogody przechwytuje współrzędne i wersję wyboru miasta przed uruchomieniem wątku. Spóźniony wynik dla poprzedniej lokalizacji jest odrzucany, a aktualne miasto pobierane ponownie.
- Po żądaniu zamknięcia aplikacji panel odczytuje ponownie statystyki pamięci. O faktycznym zamknięciu procesu decyduje Android.
- Czas wygaszacza zapisuje się natychmiast; licznik bezczynności działa po powrocie z otwartego okna, które samo blokuje wygaszacz.

Po rozszerzeniu skompilowano i zainstalowano oba debug APK. Dodatkowo na TV sprawdzono w Private zmianę jednostki na °F i języka na angielski przy otwartym panelu; przywrócono °C i język polski. Nie powtarzano pełnego scenariusza wyboru klubów i odliczania wygaszacza.
