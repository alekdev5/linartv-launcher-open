# Spotify na pulpicie / Spotify on the home screen

## Polski
Pasek między zegarem a przyciskami pokazuje wykonawcę i tytuł w jednej linii oraz grafikę udostępnioną przez Spotify. Tekst przesuwa się płynnie od lewej do prawej po krótkim zatrzymaniu na początku. Aktualizacje stanu tego samego utworu nie resetują animacji. Kliknięcie otwiera Spotify. Pasek znika po zakończeniu sesji, a pauza zachowuje informację o utworze. Kolory pochodzą z aktualnego wyglądu launchera.

W Ustawieniach przycisk Spotify prowadzi do systemowego dostępu do powiadomień. Android wymaga tego uprawnienia do odczytu sesji multimedialnych innych aplikacji. Uprawnienie obejmuje wszystkie powiadomienia, ale LinarTV nie odczytuje ich treści ani ich nie zapisuje — korzysta tylko z kontrolerów sesji pakietów Spotify. Nie wymaga konta Spotify w launcherze ani dodatkowych zapytań sieciowych.

Metadane i stan odtwarzania aktualizują się przez callbacki Androida. Nasłuchiwanie jest wyłączane, gdy pulpit nie jest widoczny.

Test na telewizorze Philips: odtwarzana muzyka była widoczna, a kolejne utwory aktualizowały tytuł i wykonawcę. Spotify na tym TV udostępniało logo aplikacji zamiast okładki albumu; launcher pokazuje grafikę otrzymaną z sesji.

## English
The strip between the clock and the toolbar shows artist and title on one line, plus the artwork provided by Spotify. The text scrolls smoothly after a short pause at the start; state updates of the same track do not restart the animation. Clicking it opens Spotify. The strip disappears when the session ends; pausing keeps the track information. Colours follow the current launcher appearance.

In Settings, the Spotify button opens the system *notification access* screen. Android requires this permission to read other apps' media sessions. The permission covers all notifications, but LinarTV neither reads nor stores their content — it only uses the session controllers of Spotify packages. No Spotify account and no extra network requests are needed.

Metadata and playback state are updated through Android callbacks. Listening stops while the home screen is not visible.

Tested on a Philips TV: the playing track was shown and each next track updated title and artist. Spotify on that TV provided the app logo instead of album art; the launcher shows whatever artwork the session provides.
