# Bezpieczeństwo i dane prywatne / Security

**PL.** Repozytorium nie może zawierać haseł, kluczy API, kluczy podpisu, adresów domowej sieci ani zdjęć użytkowników. Klucz podpisu (`*.jks`) leży poza repozytorium w `%USERPROFILE%\.linartv`, a `keystore.properties` jest ignorowany przez Git.

Przed każdym `git push` uruchom `python scripts/audit_public.py` i przejrzyj `git diff --cached`. Skrypt to dodatkowa kontrola, nie gwarancja.

Wysyłanie zdjęć z telefonu działa tylko w sieci lokalnej i tylko gdy okno z kodem jest otwarte na TV. Po 5 błędnych kodach serwer odrzuca kolejne próby do ponownego otwarcia okna.

**EN.** Never commit passwords, API keys, signing keys, home network addresses or personal photos. To report a security problem, open an issue without sensitive details and ask for a private contact.
