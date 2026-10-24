# Local testing

## Without a TV

With JDK 17 on `JAVA_HOME`, run from the repository root:

```powershell
.\scripts\test-local.ps1
.\gradlew.bat :app:assembleDebug
```

The first command tests the actual scheduling rules for automatic light/dark
appearance (including schedules crossing midnight), forced appearance, and the
screensaver timeout. It does not test Android views or remote navigation.
The second command compiles the native Android app; it does not run it.

Local checks also compare the optimized popup blur pixel by pixel with the
original blur. Actual TV preparation timings and limitations are recorded in
`OPTIMIZATION_REPORT.md`.

## Android TV emulator

An Android TV virtual device can exercise the real APK, D-pad focus, Back,
settings persistence, wallpaper gallery and popup navigation on a laptop.
It requires Android Emulator, a TV system image and preferably hardware
virtualization. A browser mockup does not run the Android implementation.

On the development laptop, checked 2026-10-24:

- No emulator or TV system image is installed in the configured Android SDK.
- SDK image discovery failed with a Java certificate validation error (PKIX).
- Windows reported firmware virtualization unavailable. Confirm the actual
  Windows/firmware configuration before installing an accelerated emulator.
- No television was used for the quick settings implementation session.

Do not disable HTTPS certificate validation to download an image. Resolve the
Java trust configuration or install the SDK packages through Android Studio.

Philips HDMI inputs, Ambilight, Cast, real background-process restrictions,
remote wake and performance still need checks on the actual television.

## Quick settings verification on Android

1. The home toolbar contains Watch TV, Inputs and Settings, in that order.
2. Settings opens the side panel. D-pad reaches all six tiles and footer actions;
   Back closes a submenu first, then the overview, restoring home focus.
3. RAM/storage appear only in the overview.
4. Wallpapers opens the existing gallery and phone upload flow.
5. Screensaver supports Off, presets and a custom 1–1440 minute value; restart
   the app and verify persistence. Only user photos are used.
6. Appearance supports Automatic, Always light and Always dark. Custom start
   times persist; identical times are rejected. Defaults remain 08:00 / 20:00.
7. Weather still supports city search and Celsius/Fahrenheit; calendars still
   support club selection and can request an immediate refresh.
8. Language selection remains available. Verify Polish and English.
9. Changing settings or completing a data refresh while a popup is open must
   not replace its underlying dashboard; rebuild happens after the popup stack closes.

Background cleanup asks Android to kill eligible background processes. The
system can refuse or restart them; the UI must not promise recovered RAM.
