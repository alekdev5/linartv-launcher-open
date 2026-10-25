# Bezpieczeństwo i dane prywatne / Security and private data

## Polski
Repozytorium nie może zawierać haseł, kluczy API, kluczy podpisu, adresów domowej sieci ani zdjęć użytkowników. Klucz podpisu (`*.jks`) leży poza repozytorium w `%USERPROFILE%\.linartv`, a `keystore.properties` jest ignorowany przez Git.

Przed każdym `git push` uruchom `python scripts/audit_public.py` i przejrzyj `git diff --cached`. Skrypt to dodatkowa kontrola, nie gwarancja.

Wysyłanie zdjęć z telefonu działa tylko w sieci lokalnej i tylko wtedy, gdy okno z kodem jest otwarte na TV. Po 5 błędnych kodach serwer odrzuca kolejne próby, dopóki okno nie zostanie otwarte ponownie.

Problem z bezpieczeństwem zgłoś przez issue bez wrażliwych szczegółów i poproś o prywatny kontakt.

## English
Never commit passwords, API keys, signing keys, home network addresses or personal photos. The signing key (`*.jks`) lives outside the repository in `%USERPROFILE%\.linartv`; `keystore.properties` is ignored by Git.

Before every `git push`, run `python scripts/audit_public.py` and review `git diff --cached`. The script is an extra check, not a guarantee.

Phone photo upload works only on the local network and only while the code window is open on the TV. After 5 wrong codes the server rejects further attempts until the window is opened again.

To report a security problem, open an issue without sensitive details and ask for a private contact.
