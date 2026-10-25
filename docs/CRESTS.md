# Herby klubowe / Club crests

**PL.** Pakiet zawiera 77 herbów PNG w `app/src/main/assets/crests/curated/`: po 18 dla Ekstraklasy, I ligi, II ligi i III ligi grupy IV, 4 dla III ligi grupy I (Warta Sieradz oraz rezerwy: ŁKS II Łódź, Jagiellonia II Białystok, Wisła II Płock) i 1 dla III ligi grupy II (Polonia Środa Wielkopolska). Powiązania nazw klubów, identyfikatorów drużyn z kalendarzeligowe.pl, plików i adresów źródłowych są w `app/src/main/assets/crests.json`.

Herby pochodzą z serwisu Ekstraklasy oraz stron i materiałów do pobrania udostępnianych przez kluby; zestawy niższych lig — z paczek dostarczonych przez właściciela projektu (folder `club-crests`). Grafika nie jest przerabiana: pliki większe niż 512 px są jedynie zmniejszane do 512 px (dłuższy bok, z zachowaniem przezroczystości), bo aplikacja i tak nie wyświetla herbu w większym rozmiarze; jeśli zmniejszony plik nie byłby mniejszy, zostaje oryginał. Zapis `source: curated` oznacza lokalnie dostarczony plik.

Herby są znakami poszczególnych klubów i **nie są objęte licencją MIT** tego repozytorium. Zostały dołączone wyłącznie po to, by aplikacja pokazywała herby drużyn w terminarzu. Jeśli reprezentujesz klub i chcesz, by herb został usunięty lub zastąpiony, załóż zgłoszenie (issue) w repozytorium — zostanie usunięty w najbliższym wydaniu.

### Jak aplikacja dobiera herb
1. Wbudowany herb z `crests.json` — według ligi i nazwy; jeśli wpis ma identyfikator drużyny z kalendarzeligowe.pl, identyfikator ma pierwszeństwo. Drużyny rezerw mają osobne wpisy (np. „ŁKS II Łódź”), więc nie przejmują herbu pierwszego zespołu przypadkiem.
2. Dopasowanie po nazwie między ligami (np. w Pucharze Polski) — tylko gdy jest jednoznaczne.
3. Brakujące herby są pobierane w tle z kalendarzeligowe.pl i zapisywane lokalnie; po 7 dniach aplikacja sprawdza aktualizację.
4. Jeśli źródło nie udostępnia obsługiwanego obrazu, pozostają inicjały.

### Aktualizacja zestawu
```powershell
powershell -ExecutionPolicy Bypass -File scripts\import-club-crests.ps1   # kopiuje herby z ..\club-crests i sprawdza SHA-256
python scripts\shrink-crests.py                                          # dopasowuje kopie do 512 px
```

**EN.** The 77 bundled crests cover Ekstraklasa, I liga, II liga and III liga group IV (18 each), III liga group I (Warta Sieradz and the reserve teams ŁKS II Łódź, Jagiellonia II Białystok, Wisła II Płock) and III liga group II (Polonia Środa Wielkopolska). Artwork is not altered; files larger than 512 px are only scaled down to 512 px (longest side, transparency kept), the largest size the app ever displays; if the scaled file would not be smaller, the original is kept. Club crests are **not covered by the MIT license**. If you represent a club and want a crest removed or replaced, please open an issue and it will be removed in the next release. Missing crests are fetched from kalendarzeligowe.pl and cached; clubs without an image show initials.
