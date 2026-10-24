# Popup optimization — 2026-10-24

Measured on the Philips TPM171E at 1920×1080, using the same three cycles before
and after the change: Settings → Appearance → Back → Back → Weather → Back.

| Measured preparation | Before, median | After, median | Reduction |
| --- | ---: | ---: | ---: |
| Top-level popup, 6 samples | 384.5 ms | 183.3 ms | 52.3% |
| Appearance submenu, 3 samples | 368.2 ms | 10.4 ms | 97.2% |

These are `showDialogWithBlur` preparation times measured on the UI thread,
including background capture/blur and `Dialog.show()`. They exclude construction
of the popup contents before this method, subsequent layout/drawing and display
presentation. They do **not** establish an equivalent improvement in launcher
startup, overall frame rate, network requests or complete button-to-screen latency.
Sample counts are small; background system load can affect individual results.

## Changes

- Replace the repeated radius-wide summation in the separable box blur with a
  moving sum. Edge clamping, integer rounding and pass count stay the same.
- Keep one blurred dashboard background for the active stack of popup windows.
  Nested windows reuse it instead of capturing and blurring the already blurred
  background. Release its view and bitmap when the last popup closes.
- Keep the existing rule that dashboard rebuilds wait until the popup stack closes.
- Add debug-only preparation timings and a repeatable measurement script:
  `scripts/measure-popups.ps1 -Adb <adb.exe> -Serial <device> -Port <adb-port>`.
  Its tap coordinates assume 1920×1080 and the three-card home layout.

At this resolution each captured background is 480×270×4 bytes (518,400 bytes).
Sharing it avoids that additional bitmap per nested window, as well as its
temporary blur arrays and repeated UI-thread work. This is an allocation estimate,
not a measured reduction in the process's total RAM.

## Validation

- 36 blur cases compare every output pixel with the original algorithm, including
  one-pixel dimensions, radii 0/2/4, one/two passes and a 480×270 image: all equal.
- Existing local schedule/screensaver checks pass.
- Debug APK builds; public-data audit passes; `git diff --check` passes.
- Installed and checked on the actual television. Nested windows, Back, focus
  restoration, return to the unblurred dashboard and the overview were inspected.
- No AndroidRuntime crash was reported during this check.
- No commit or push was made; Private has not been migrated.

## Raw preparation times (ms)

Before, top level: 526.134292, 455.459125, 365.318208, 377.086333, 375.548584, 391.986501.

Before, nested: 406.804042, 367.517708, 368.212667.

After, top level: 225.985, 208.709584, 189.690751, 176.900833, 171.7695, 167.54525.

After, nested: 10.396625, 9.867958, 11.156.
