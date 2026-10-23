# Plan / Roadmap

## 0.1.0 — pierwsze wydanie
- [x] Wybór jednego lub dwóch klubów z katalogu kalendarzeligowe.pl (za zgodą właściciela serwisu).
- [x] Prognoza i aktualne warunki z Open-Meteo dla dowolnego miasta (bez klucza API).
- [x] Galeria tapet, wysyłanie zdjęć z telefonu (kod z limitem prób), wygaszacz.
- [x] Podpisane APK i instalacja przez Downloader.
- [ ] Test na kilku urządzeniach Android TV / Google TV.

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

- [ ] **Etap 1 — wklejany link (bez katalogu):** w kreatorze klubów opcja „Dodaj kalendarz z fixtur.es”; użytkownik kopiuje na fixtur.es link do kalendarza swojej drużyny i wkleja go w LinarTV (lub skanuje kod QR i wkleja z telefonu). Aplikacja działa jak zwykły klient kalendarza.
- [ ] **Kontakt:** e-mail do De Agendamakers (info@agendamakers.nl) — prośba o zgodę na katalog drużyn i o stabilny format adresów ICS.
- [ ] **Etap 2 — katalog (tylko po zgodzie):** wybór kraj → liga → drużyna lub wyszukiwanie nazwy w kreatorze.
- [ ] **Opisy wydarzeń:** pokazywać opis z kalendarza w szczegółach meczu (fixtur.es umieszcza tam swoje treści promocyjne — nie ukrywać ich).
- [ ] **Parser ICS:** dopasować do formatu fixtur.es (nazwy drużyn w SUMMARY, rozgrywki, kolejka, wyniki na żywo, różne strefy czasowe).
- [ ] **Herby:** dla zagranicznych klubów inicjały (fallback), bez dołączania nowych grafik bez zgody.
- [ ] **Język:** domyślnie angielski interfejs, gdy system TV nie jest po polsku.
- [ ] Atrybucja fixtur.es w szczegółach meczu i w README.

## Później
- Podział `DashboardActivity` na mniejsze klasy.

Sterowanie oświetleniem nie jest częścią tej wersji.
