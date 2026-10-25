package com.linart.tvpreview;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the small VEVENT subset used by the two public football subscriptions. */
final class IcsMatchParser {
    private static final Pattern ROUND = Pattern.compile("(?iu)\\bKolejka\\s*[–—:-]\\s*Kolejka\\s+(\\d{1,2})\\b");
    static final class Match {
        final String home;
        final String away;
        final long startsAt;
        final boolean hasTime;
        final int round;
        final String competition, stage;
        final int homeTeamId, awayTeamId;
        Match(String home, String away, long startsAt, boolean hasTime, int round) {
            this(home, away, startsAt, hasTime, round, "", "");
        }
        Match(String home, String away, long startsAt, boolean hasTime, int round,
              String competition, String stage) {
            this(home, away, startsAt, hasTime, round, competition, stage, 0, 0);
        }
        Match(String home, String away, long startsAt, boolean hasTime, int round,
              String competition, String stage, int homeTeamId, int awayTeamId) {
            this.home = home; this.away = away; this.startsAt = startsAt; this.hasTime = hasTime;
            this.round = round;
            this.competition = competition; this.stage = stage;
            this.homeTeamId = homeTeamId; this.awayTeamId = awayTeamId;
        }
    }

    static List<Match> parse(String ics, String requiredDescription, long fromMillis) {
        String normalized = ics.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("\n[ \t]", "");
        List<Match> result = new ArrayList<>();
        HashMap<String, String> event = null;
        for (String line : normalized.split("\n")) {
            if (line.equals("BEGIN:VEVENT")) { event = new HashMap<>(); continue; }
            if (line.equals("END:VEVENT")) {
                if (event != null) addEvent(result, event, requiredDescription, fromMillis);
                event = null; continue;
            }
            if (event == null) continue;
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String key = line.substring(0, colon);
            if (key.startsWith("DTSTART")) event.put("DTSTART_KEY", key);
            event.put(key.split(";", 2)[0], line.substring(colon + 1));
        }
        result.sort(Comparator.comparingLong(m -> m.startsAt));
        return result;
    }

    private static void addEvent(List<Match> out, HashMap<String, String> event,
                                 String requiredDescription, long fromMillis) {
        try {
            if ("CANCELLED".equalsIgnoreCase(event.get("STATUS"))) return;
            if (requiredDescription != null && !event.getOrDefault("DESCRIPTION", "").contains(requiredDescription)) return;
            String summary = unescape(event.getOrDefault("SUMMARY", ""));
            int separator = summary.indexOf(" - ");
            if (separator < 1) return;
            String home = summary.substring(0, separator).trim();
            String away = summary.substring(separator + 3).trim()
                    .replaceFirst("\\s+\\[[^]]+\\](?:\\s+\\(\\d+[-:]\\d+\\))?$", "")
                    .replaceFirst("\\s+\\(\\d+[-:]\\d+\\)$", "");
            String value = event.get("DTSTART");
            if (value == null) return;
            String key = event.getOrDefault("DTSTART_KEY", "");
            boolean hasTime = value.contains("T") && !key.contains("VALUE=DATE");
            SimpleDateFormat format = new SimpleDateFormat(hasTime ? "yyyyMMdd'T'HHmmss" : "yyyyMMdd", Locale.US);
            format.setLenient(false);
            format.setTimeZone(TimeZone.getTimeZone(value.endsWith("Z") ? "UTC" : "Europe/Warsaw"));
            Date date = format.parse(value.endsWith("Z") ? value.substring(0, value.length() - 1) : value);
            if (date != null && date.getTime() >= fromMillis) {
                Matcher round = ROUND.matcher(unescape(event.getOrDefault("DESCRIPTION", "")));
                int roundNumber = round.find() ? Integer.parseInt(round.group(1)) : -1;
                out.add(new Match(home, away, date.getTime(), hasTime, roundNumber));
            }
        } catch (Exception ignored) { }
    }

    private static String unescape(String value) {
        return value.replace("\\,", ",").replace("\\;", ";").replace("\\n", " ").replace("\\\\", "\\");
    }
}
