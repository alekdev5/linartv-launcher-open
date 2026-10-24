# Plan / Roadmap

## 0.1.0 — pierwsze wydanie
- [x] Wybór jednego lub dwóch klubów z katalogu kalendarzeligowe.pl (za zgodą właściciela serwisu).
- [x] Prognoza i aktualne warunki z Open-Meteo dla dowolnego miasta (bez klucza API).
- [x] Galeria tapet, wysyłanie zdjęć z telefonu (kod z limitem prób), wygaszacz.
- [x] Podpisane APK i instalacja przez Downloader.
- [ ] Test na kilku urządzeniach Android TV / Google TV.

## Jakość terminarzy w ligach świętokrzyskich
- [ ] Zweryfikować znaczenie `isDateTbd` w API kalendarzeligowe.pl. Dla Alit Ożarów – Neptun Końskie (24.10.2026) oraz Orlicz Suchedniów – Alit Ożarów (31.10.2026) API i Kielecka Piłka podają te same godziny, choć flaga w API ma wartość `true`; obecnie LinarTV ukrywa godzinę.
- [ ] Porównać więcej najbliższych spotkań IV ligi, okręgówki i klas A/B z Kielecką Piłką oraz sprawdzić dostępność ustrukturyzowanego eksportu. Przed włączeniem tego serwisu jako źródła danych uzgodnić sposób pobierania z redakcją.

## 0.2.0 — własne aplikacje na pulpicie
Użytkownik sam decyduje, które aplikacje są widoczne na pierwszym ekranie i w jakiej kolejności. Dziś pasek ma na stałe YouTube, Netflix, CANAL+ i Spotify.

- [ ] **Dodawanie do pulpitu:** w pełnej liście aplikacji przytrzymanie OK (długie naciśnięcie) otwiera menu „Dodaj do pulpitu / Usuń z pulpitu”.
- [ ] **Przesuwanie pilotem:** na pasku pulpitu długie naciśnięcie OK włącza tryb przesuwania; strzałki ← → przenoszą kafelek, OK zatwierdza, Wstecz anuluje.
- [ ] **Usuwanie z pulpitu:** z tego samego menu na kafelku pulpitu.
- [ ] **Zapis wyboru** lokalnie na TV (lista nazw pakietów w kolejności); przy pierwszym uruchomieniu domyślny zestaw z zainstalowanych aplikacji.
- [ ] **Limit miejsca:** maksymalna liczba kafelków dopasowana do szerokości paska; przy pełnym pasku komunikat zamiast cichego pominięcia.
- [ ] **Odinstalowane aplikacje** znikają z pulpitu automatycznie.
- [ ] Logo: dla aplikacji bez własnej grafiki pulpit pokazuje baner lub ikonę z systemu.
- [ ] Opcja „Przywróć domyślny układ” w ustawieniach.

## 0.3.0 — mecze z całego świata (fixtur.es)
Drugie źródło terminarzy obok kalendarzeligowe.pl: [fixtur.es](https://fixtur.es/) (De Agendamakers B.V.) — darmowe kalendarze ICS dla drużyn z lig na całym świecie. Otwiera LinarTV na rynki poza Polską.

Uzgodniona reguła wyboru źródła: **Polska → API kalendarzeligowe.pl; Świat → kalendarze ICS fixtur.es**. Kreator zaczyna się od wyboru „Polska / Świat”; dla Polski pokazuje ligę i klub. W części „Świat” działa testowe dodanie linku ICS skopiowanego przez użytkownika (z przykładem Arsenalu). Automatyczny katalog krajów, lig i klubów pozostaje do uzgodnienia z fixtur.es. Można dodać jeden lub dwa kluby.

Stan rozpoznania (24.10.2026): w polskiej części serwisu są Ekstraklasa i I liga; niższych lig nie znaleziono. Kalendarz klubowy obejmuje też puchary i sparingi. Część odległych terminów ma godzinę 01:00/02:00 — wygląda na datę bez potwierdzonej godziny, więc aplikacja nie powinna pokazywać jej jako pewnej godziny meczu. Właściciel projektu wysłał już pytanie do operatora o wykorzystanie źródła.

Próba techniczna: strona klubu wskazuje adres `https://ics.fixtur.es/v2/<klub>.ics`; przykładowy kalendarz Arsenalu zawiera także historyczne wydarzenia i ma około 200 kB. Parser musi filtrować daty, rozpoznawać strefy czasowe i oddzielać dopiski z wynikiem/rozgrywkami od nazwy rywala. Nie wyprowadzać adresów ICS wyłącznie z nazwy klubu — korzystać z linku podanego przez dostawcę.

- [x] **Etap 1 — wklejany link (bez katalogu):** w kreatorze klubów użytkownik wpisuje nazwę drużyny i link ICS z fixtur.es; domyślny przykład to Arsenal. Dodawanie linku z telefonu przez QR pozostaje na później.
- [x] **Kontakt:** wysłano pytanie do operatora o wykorzystanie źródła; przed integracją katalogu potwierdzić warunki, stabilny format adresów ICS i dopuszczalną częstotliwość odświeżania.
- [ ] **Etap 2 — katalog (tylko po zgodzie):** wybór kraj → liga → drużyna lub wyszukiwanie nazwy w kreatorze.
- [ ] **Kierowanie źródła:** w kreatorze nie pokazywać użytkownikowi technicznego wyboru dostawcy; Polska korzysta z obecnego katalogu, inne kraje z zatwierdzonego katalogu fixtur.es. Zapisać identyfikator dostawcy przy każdym klubie.
- [ ] **Opisy wydarzeń:** pokazywać opis z kalendarza w szczegółach meczu (fixtur.es umieszcza tam swoje treści promocyjne — nie ukrywać ich).
- [ ] **Parser ICS:** dopasować do formatu fixtur.es (nazwy drużyn w SUMMARY, rozgrywki, kolejka, wyniki na żywo, różne strefy czasowe).
- [ ] **Godziny meczów:** rozpoznawać terminy orientacyjne/bez ustalonej godziny; potwierdzić znaczenie tych wpisów z operatorem źródła.
- [ ] **Herby:** dla zagranicznych klubów inicjały (fallback), bez dołączania nowych grafik bez zgody.
- [ ] **Język:** domyślnie angielski interfejs, gdy system TV nie jest po polsku.
- [ ] Atrybucja fixtur.es w README (interfejs pozostaje minimalistyczny, bez logotypów).

## Pełne pokrycie polskich lig (do ustalenia)
Cel: wszystkie poziomy rozgrywek w Polsce, aż po klasy A i B.

- [ ] Sprawdzić, jak daleko w dół sięga katalog kalendarzeligowe.pl, i zapytać właściciela o brakujące klasy rozgrywkowe.
- [ ] laczynaspilka.pl (PZPN) ma komplet rozgrywek, ale jego API (`competition-api-pro2.laczynaspilka.pl`) wymaga tokenu wydawanego po weryfikacji reCAPTCHA; bez niego zwraca 401. Nie obchodzimy tego zabezpieczenia — jedyna droga to oficjalna zgoda PZPN lub właściwego Wojewódzkiego ZPN.
- [ ] Nie pobierać danych z serwisów, które zabraniają tego w `robots.txt` (np. kieleckapilka.pl).

## Później
- Podział `DashboardActivity` na mniejsze klasy.

Sterowanie oświetleniem nie jest częścią tej wersji.
