package com.linart.tvpreview;

/** Scheduling rules shared by the launcher and local JVM checks. All times are local. */
final class LauncherTiming {
    static boolean isNight(String mode, int minute, int dayStart, int nightStart) {
        if ("dark".equals(mode)) return true;
        if ("light".equals(mode)) return false;
        boolean day = dayStart < nightStart ? minute >= dayStart && minute < nightStart
                : minute >= dayStart || minute < nightStart;
        return !day;
    }

    static long idleMillis(int minutes) {
        return Math.max(0, Math.min(1440, minutes)) * 60_000L;
    }
}
