# Herby klubowe / Club crests

**PL.** Od wersji 1.0.1 aplikacja nie zawiera żadnych herbów. Wszystkie herby pochodzą z [kalendarzeligowe.pl](https://kalendarzeligowe.pl) (`/api/teams/{id}/logo`, PNG z przezroczystym tłem), z którego LinarTV korzysta za zgodą właściciela serwisu.

### Jak aplikacja dobiera herb
1. Każdy mecz z API ma identyfikator drużyny i wersję herbu (`crestVer`). Herb jest pobierany po identyfikatorze i zapisywany w pamięci telewizora (`crests-by-team`).
2. Herb jest pobierany ponownie tylko wtedy, gdy zmieni się jego wersja. Bez znanej wersji (stare dane) aplikacja sprawdza go co 7 dni.
3. Drużyny bez herbu (dziś większość IV ligi) mają inicjały. Aplikacja sprawdza, czy herb się pojawił, najwyżej co 12 godzin.
4. Gdy mecz nie ma identyfikatora drużyny (bardzo stare dane), drużyna jest szukana po dokładnej nazwie, tylko jeśli wynik jest jednoznaczny.

Herby są znakami poszczególnych klubów i **nie są objęte licencją MIT** tego repozytorium. Repozytorium nie zawiera plików herbów.

**EN.** Since 1.0.1 the app bundles no crests. All crests come from [kalendarzeligowe.pl](https://kalendarzeligowe.pl) (`/api/teams/{id}/logo`, transparent PNG), used with the site owner's permission. Every match from the API carries the team id and a crest version (`crestVer`); a crest is cached on the TV per team and downloaded again only when its version changes (every 7 days when the version is unknown). Teams without a crest (most of IV liga for now) show initials; the app checks for a new crest at most every 12 hours. Club crests are **not covered by the MIT license**, and the repository contains no crest files.
