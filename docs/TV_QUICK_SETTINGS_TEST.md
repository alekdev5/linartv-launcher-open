# Quick settings check on Philips TPM171E — 2026-10-24

Installed the current debug APK as an update to
`io.github.domlin92.linartv.open`, preserving its existing configuration.
The private installation remains the default HOME application.

Verified on the actual television:

- The right settings panel touches the top, bottom and right screen edges.
- Six tiles fit the panel; icons, text and focus aura render correctly.
- D-pad Right, Down, OK opens the appearance submenu from the initial tile.
- Appearance and screensaver option lists open and Back returns to the overview.
- Inputs shows Live TV and four actual HDMI inputs, including HDMI 1 / MHL.
  No simulated HDMI entries were enabled on this device.
- Weather details displays the selected city, seven-day forecast and a timestamp
  formatted as `dd.MM.yyyy, HH:mm`, including with a long city name.
- HOME returns to `io.github.domlin92.linartv.private`.
- No AndroidRuntime crash was reported during this check.

Fixed a focus issue found during the check: settings tiles now accept the initial
focus even after pointer input, so the first D-pad press follows the intended grid.
The fix was rebuilt, installed and checked again on the television.

Not exercised in this visual/navigation check: switching an actual HDMI signal,
waiting for the full screensaver timeout, uploading/deleting photos, or changing
saved city, clubs, language and appearance schedules.

Screenshots are local inspection outputs in the ignored `tmp` directory:
`tv-open-settings-focus-fixed.png`, `tv-open-appearance-current.png`,
`tv-open-saver-menu-current.png`, `tv-open-sources-current.png`,
`tv-open-weather-current.png`.

Private migration is a separate next step after visual acceptance. Preserve its
lighting integration, configuration and photos when porting shared functionality.
