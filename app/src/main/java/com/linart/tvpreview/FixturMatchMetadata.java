package com.linart.tvpreview;

import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adds only metadata explicitly published by fixtur.es; never infers a round from fixture order. */
final class FixturMatchMetadata {
    private static final Pattern JSON = Pattern.compile(
            "<script[^>]+type=['\"]application/ld\\+json['\"][^>]*>(.*?)</script>", Pattern.DOTALL);
    private static final Pattern CANONICAL_ROUND = Pattern.compile(
            "<link[^>]+rel=['\"]canonical['\"][^>]+href=['\"]https://fixtur\\.es/en/matches/[a-z0-9-]+/round/(\\d+)['\"]");

    private static final class Event {
        final String key, competition;
        Event(String key, String competition) { this.key = key; this.competition = competition; }
    }

    static List<IcsMatchParser.Match> enrich(List<IcsMatchParser.Match> matches, String slug) {
        if (matches.isEmpty()) return matches;
        Map<String, String> competitions = new HashMap<>();
        Map<String, Integer> rounds = new HashMap<>();
        try {
            for (Event event : events(FixturCatalogClient.fetch("https://fixtur.es/en/team/" + slug)))
                competitions.put(event.key, event.competition);
            Set<String> needed = new LinkedHashSet<>();
            for (int i = 0; i < Math.min(2, matches.size()); i++) {
                String competition = competitions.get(key(matches.get(i)));
                if (competition != null) needed.add(competition);
            }
            if (!needed.isEmpty()) {
                for (FixturCatalogClient.League league : FixturCatalogClient.leagues()) {
                    if (!needed.contains(league.name)) continue;
                    try {
                        String address = "https://fixtur.es/en/matches/" + league.slug;
                        String html = FixturCatalogClient.fetch(address);
                        int round = recordRound(html, rounds);
                        // The dashboard exposes the nearest two fixtures. Check the adjacent round too.
                        if (round > 0 && !covered(matches, competitions, rounds, league.name))
                            recordRound(FixturCatalogClient.fetch(address + "/round/" + (round + 1)), rounds);
                    } catch (Exception e) {
                        android.util.Log.w("LinarTV", "fixtur.es round metadata unavailable for " + league.slug);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.w("LinarTV", "fixtur.es competition metadata unavailable", e);
        }
        List<IcsMatchParser.Match> result = new ArrayList<>();
        for (IcsMatchParser.Match match : matches) {
            String key = key(match);
            result.add(new IcsMatchParser.Match(match.home, match.away, match.startsAt, match.hasTime,
                    rounds.containsKey(key) ? rounds.get(key) : match.round,
                    competitions.containsKey(key) ? competitions.get(key) : match.competition, match.stage));
        }
        return result;
    }

    private static boolean covered(List<IcsMatchParser.Match> matches, Map<String, String> competitions,
                                   Map<String, Integer> rounds, String competition) {
        for (int i = 0; i < Math.min(2, matches.size()); i++) {
            String key = key(matches.get(i));
            if (competition.equals(competitions.get(key)) && !rounds.containsKey(key)) return false;
        }
        return true;
    }

    private static int recordRound(String html, Map<String, Integer> rounds) {
        Matcher matcher = CANONICAL_ROUND.matcher(html);
        if (!matcher.find()) return -1;
        int number = Integer.parseInt(matcher.group(1));
        for (Event event : events(html)) rounds.put(event.key, number);
        return number;
    }

    private static List<Event> events(String html) {
        List<Event> result = new ArrayList<>();
        Matcher matcher = JSON.matcher(html);
        while (matcher.find()) try {
            JSONObject json = new JSONObject(matcher.group(1));
            if (!"SportsEvent".equals(json.optString("@type"))) continue;
            String home = json.getJSONObject("homeTeam").getString("name");
            String away = json.getJSONObject("awayTeam").getString("name");
            long time = OffsetDateTime.parse(json.getString("startDate")).toInstant().toEpochMilli();
            String competition = json.optString("description", "")
                    .replaceFirst("\\s+20\\d{2}(?:[-–]\\d{2,4})?$", "").trim();
            if (!competition.isEmpty()) result.add(new Event(key(home, away, time), competition));
        } catch (Exception ignored) { }
        return result;
    }

    private static String key(IcsMatchParser.Match match) { return key(match.home, match.away, match.startsAt); }
    private static String key(String home, String away, long time) { return home + "|" + away + "|" + time; }
}
