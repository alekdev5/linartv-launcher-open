package com.linart.tvpreview;

/** Runs with a plain JDK; no TV, emulator, network, or Gradle dependencies. */
public final class LauncherTimingCheck {
    public static void main(String[] args) {
        for (int minute = 0; minute < 1440; minute++) {
            check(LauncherTiming.isNight("auto", minute, 480, 1200)
                    == (minute < 480 || minute >= 1200), "Default schedule at " + minute);
            check(LauncherTiming.isNight("auto", minute, 1200, 480)
                    == (minute >= 480 && minute < 1200), "Overnight day interval at " + minute);
            check(LauncherTiming.isNight("auto", minute, 495, 1175)
                    == (minute < 495 || minute >= 1175), "Custom minute precision at " + minute);
            check(!LauncherTiming.isNight("light", minute, 480, 1200), "Forced light");
            check(LauncherTiming.isNight("dark", minute, 480, 1200), "Forced dark");
        }
        check(LauncherTiming.idleMillis(0) == 0, "Disabled saver");
        check(LauncherTiming.idleMillis(5) == 300_000L, "Default timeout");
        check(LauncherTiming.idleMillis(37) == 2_220_000L, "Custom timeout");
        check(LauncherTiming.idleMillis(1440) == 86_400_000L, "24 hour timeout");
        check(LauncherTiming.idleMillis(-1) == 0, "Negative value");
        check(LauncherTiming.idleMillis(Integer.MAX_VALUE) == 86_400_000L, "Overflow protection");
        System.out.println("OK: 7206 scheduling and screensaver checks");
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
