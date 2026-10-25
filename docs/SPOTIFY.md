# Spotify na pulpicie

Pasek pomiędzy zegarem a przyciskami pokazuje wykonawcę i tytuł w jednej linii oraz grafikę udostępnioną przez Spotify. Tekst przesuwa się płynnie od lewej do prawej, po krótkim początkowym zatrzymaniu. Aktualizacje stanu tego samego utworu nie resetują animacji. Kliknięcie otwiera Spotify. Pasek znika po zakończeniu sesji; pauza zachowuje informację o utworze. Kolory pochodzą z aktualnego wyglądu launchera.

W Ustawieniach przycisk Spotify prowadzi do systemowego dostępu do powiadomień. Android wymaga tego uprawnienia do odczytu cudzych sesji multimedialnych. Uprawnienie obejmuje wszystkie powiadomienia, ale implementacja nie odczytuje ich treści ani ich nie zapisuje. Korzysta tylko z kontrolerów sesji pakietów Spotify. Nie wymaga konta Spotify w launcherze ani dodatkowych zapytań sieciowych.

Metadane i stan odtwarzania aktualizują się przez callbacki Androida. Nasłuchiwanie jest wyłączane, gdy pulpit nie jest widoczny.

Test na Philipsie: aktywna muzyka Spotify była widoczna w Open i Private; kolejne utwory aktualizowały tytuł i wykonawcę. Spotify na tym TV udostępniało logo aplikacji zamiast okładki albumu; launcher pokazuje grafikę otrzymaną z sesji.
