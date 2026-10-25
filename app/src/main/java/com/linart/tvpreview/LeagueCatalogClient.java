package com.linart.tvpreview;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Current leagues and teams from the same public catalogue used by kalendarzeligowe.pl. */
final class LeagueCatalogClient {
    static final int CACHE_FORMAT = 2;
    private static final String ROOT = "https://kalendarzeligowe.pl/api/";
    private static final List<String> SLUGS = CalendarScope.LEAGUES;
    private static final String[] CATEGORIES = {"EKSTRAKLASA", "FIRST_LEAGUE", "SECOND_LEAGUE",
            "THIRD_LEAGUE", "THIRD_LEAGUE", "THIRD_LEAGUE", "THIRD_LEAGUE"};
    private static final String[] GROUPS = {"", "", "", "Grupa I", "Grupa II", "Grupa III",
            "Grupa IV"};

    static final class League {
        final int id;
        final String name, slug, season;
        League(int id, String name, String slug, String season) {
            this.id = id; this.name = name; this.slug = slug; this.season = season;
        }
    }

    static final class Team {
        final int id;
        final String name;
        Team(int id, String name) { this.id = id; this.name = name; }
        String calendarUrl(League league) {
            return "https://kalendarzeligowe.pl/" + league.slug + "/"
                    + slug(name) + "-" + id + "/calendar.ics";
        }
    }

    static List<League> leagues() throws Exception {
        JSONArray seasons = readArray("seasons");
        String season = "";
        for (int i = 0; i < seasons.length(); i++) {
            JSONObject item = seasons.getJSONObject(i);
            if (item.optBoolean("isCurrent")) { season = item.getString("name"); break; }
        }
        if (season.isEmpty()) throw new IllegalStateException("Brak bieżącego sezonu");
        JSONArray competitions = readArray("competitions?season=" + URLEncoder.encode(season, "UTF-8"));
        List<League> result = new ArrayList<>();
        for (int i = 0; i < SLUGS.size(); i++) {
            for (int j = 0; j < competitions.length(); j++) {
                JSONObject item = competitions.getJSONObject(j);
                String group = item.isNull("groupName") ? "" : item.optString("groupName", "");
                if (CATEGORIES[i].equals(item.optString("category")) && GROUPS[i].equals(group)) {
                    result.add(new League(item.getInt("id"), item.getString("name"), SLUGS.get(i), season));
                    break;
                }
            }
        }
        return result;
    }

    static List<Team> teams(League league) throws Exception {
        HashMap<Integer, Team> found = new HashMap<>();
        JSONObject standings = null;
        try { standings = readObject("competitions/" + league.id + "/standings"); }
        catch (Exception ignored) { }
        JSONArray rows = standings == null ? null : standings.optJSONArray("standings");
        if (rows != null) for (int i = 0; i < rows.length(); i++)
            add(found, rows.getJSONObject(i).optJSONObject("team"));
        if (found.isEmpty()) {
            for (int page = 1; page <= 10; page++) {
                JSONObject matches = readObject("matches?competitionId=" + league.id
                        + "&season=" + URLEncoder.encode(league.season, "UTF-8")
                        + "&pageSize=100&page=" + page);
                JSONArray data = matches.optJSONArray("data");
                if (data == null || data.length() == 0) break;
                for (int i = 0; i < data.length(); i++) {
                    JSONObject match = data.getJSONObject(i);
                    add(found, match.optJSONObject("home"));
                    add(found, match.optJSONObject("away"));
                }
                JSONObject meta = matches.optJSONObject("meta");
                if (meta == null || page >= meta.optInt("pageCount", page)) break;
            }
        }
        List<Team> result = new ArrayList<>(found.values());
        result.sort(Comparator.comparing(team -> team.name.toLowerCase(new Locale("pl", "PL"))));
        return result;
    }

    /** The site's ICS feed omits fixtures whose kickoff time is still provisional. */
    static List<IcsMatchParser.Match> teamMatches(String calendarUrl, long fromMillis) throws Exception {
        Matcher id = Pattern.compile("-(\\d+)/calendar\\.ics(?:\\?.*)?$").matcher(calendarUrl);
        if (!CalendarScope.accepts(calendarUrl) || !id.find())
            throw new IllegalArgumentException("Unknown team calendar URL");
        int teamId = Integer.parseInt(id.group(1));
        List<IcsMatchParser.Match> matches = new ArrayList<>();
        for (int page = 1; page <= 10; page++) {
            JSONObject response = readObject("matches?teamId=" + teamId + "&pageSize=100&page=" + page);
            JSONArray data = response.optJSONArray("data");
            if (data == null) throw new IllegalStateException("Missing match data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                if ("CANCELLED".equalsIgnoreCase(item.optString("status"))) continue;
                JSONObject home = item.optJSONObject("home");
                JSONObject away = item.optJSONObject("away");
                if (home == null || away == null
                        || (home.optInt("id") != teamId && away.optInt("id") != teamId)) continue;
                String date = item.optString("matchDate", "");
                if (date.isEmpty()) continue;
                Instant kickoff = Instant.parse(date);
                boolean hasTime = !item.optBoolean("isDateTbd", false);
                long startsAt = hasTime ? kickoff.toEpochMilli()
                        : kickoff.atZone(ZoneId.of("Europe/Warsaw")).toLocalDate()
                                .atStartOfDay(ZoneId.of("Europe/Warsaw")).toInstant().toEpochMilli();
                if (startsAt < fromMillis) continue;
                JSONObject round = item.optJSONObject("round");
                JSONObject competition = item.optJSONObject("competition");
                MatchCompetition metadata = MatchCompetition.from(
                        competition == null ? "" : competition.optString("name", ""),
                        competition == null ? "" : competition.optString("category", ""),
                        round == null ? "" : round.optString("name", ""),
                        round == null ? -1 : round.optInt("roundNumber", -1));
                matches.add(new IcsMatchParser.Match(home.optString("name"), away.optString("name"),
                        startsAt, hasTime, metadata.round, metadata.name, metadata.stage));
            }
            JSONObject meta = response.optJSONObject("meta");
            if (meta == null || page >= meta.optInt("pageCount", page)) break;
        }
        matches.sort(Comparator.comparingLong(match -> match.startsAt));
        return matches;
    }

    private static void add(HashMap<Integer, Team> teams, JSONObject item) {
        if (item == null || item.optInt("id") <= 0) return;
        teams.put(item.optInt("id"), new Team(item.optInt("id"), item.optString("name")));
    }

    static String slug(String name) {
        String polish = name.toLowerCase(Locale.ROOT).replace('ł', 'l');
        String plain = Normalizer.normalize(polish, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return plain.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    private static JSONArray readArray(String path) throws Exception { return new JSONArray(read(path)); }
    private static JSONObject readObject(String path) throws Exception { return new JSONObject(read(path)); }
    private static String read(String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(ROOT + path).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        try {
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException("Katalog HTTP " + connection.getResponseCode());
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                    if (bytes.size() > 2_000_000) throw new IllegalStateException("Katalog jest zbyt duży");
                }
                return bytes.toString("UTF-8");
            }
        } finally { connection.disconnect(); }
    }
}
