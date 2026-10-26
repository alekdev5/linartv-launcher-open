package com.linart.tvpreview;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Collator;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Top 5 foreign leagues from openfootball/football.json (CC0, no key), plus the club's
 * European cup matches from kalendarzeligowe.pl, which also lists foreign clubs.
 */
final class OpenFootballClient {
    static final class Country {
        final String code, namePl, nameEn, league, zone, logoPage;
        Country(String code, String namePl, String nameEn, String league, String zone, String logoPage) {
            this.code = code; this.namePl = namePl; this.nameEn = nameEn; this.league = league; this.zone = zone;
            this.logoPage = logoPage;
        }
    }
    /** openfootball file, country (PL / EN), league name, the league's own time zone. */
    static final List<Country> COUNTRIES = Arrays.asList(
            new Country("en.1", "Anglia", "England", "Premier League", "Europe/London", "england"),
            new Country("fr.1", "Francja", "France", "Ligue 1", "Europe/Paris", "france"),
            new Country("es.1", "Hiszpania", "Spain", "La Liga", "Europe/Madrid", "spain"),
            new Country("de.1", "Niemcy", "Germany", "Bundesliga", "Europe/Berlin", "germany"),
            new Country("it.1", "Włochy", "Italy", "Serie A", "Europe/Rome", "italy"));
    private static final String RAW = "https://raw.githubusercontent.com/openfootball/football.json/master/";
    private static final Pattern ROUND = Pattern.compile("(\\d+)");
    private static final Set<String> EUROPEAN = new HashSet<>(Arrays.asList(
            "CHAMPIONS_LEAGUE", "EUROPA_LEAGUE", "CONFERENCE_LEAGUE"));

    static Country country(String code) {
        for (Country c : COUNTRIES) if (c.code.equals(code)) return c;
        return null;
    }

    /** Countries sorted by their name in the interface language. */
    static List<Country> sortedCountries(boolean english) {
        List<Country> list = new ArrayList<>(COUNTRIES);
        Collator collator = Collator.getInstance(english ? Locale.ENGLISH : new Locale("pl", "PL"));
        list.sort((a, b) -> collator.compare(english ? a.nameEn : a.namePl, english ? b.nameEn : b.namePl));
        return list;
    }

    /** Season folder such as "2026-27"; a new season starts in July. */
    static String season(long nowMillis) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);
        int start = now.get(Calendar.MONTH) >= Calendar.JULY ? now.get(Calendar.YEAR) : now.get(Calendar.YEAR) - 1;
        return start + "-" + String.format(Locale.ROOT, "%02d", (start + 1) % 100);
    }

    static List<String> teams(String code) throws Exception {
        JSONArray matches = leagueMatches(code);
        TreeSet<String> names = new TreeSet<>(Collator.getInstance(Locale.ENGLISH));
        for (int i = 0; i < matches.length(); i++) {
            JSONObject m = matches.getJSONObject(i);
            names.add(m.optString("team1")); names.add(m.optString("team2"));
        }
        names.remove("");
        return new ArrayList<>(names);
    }

    static List<IcsMatchParser.Match> teamMatches(String code, String team, long fromMillis) throws Exception {
        Country country = country(code);
        if (country == null) throw new IllegalArgumentException("Unknown league " + code);
        ZoneId zone = ZoneId.of(country.zone);
        List<IcsMatchParser.Match> result = new ArrayList<>();
        JSONArray matches = leagueMatches(code);
        loadLogoPages();
        for (int i = 0; i < matches.length(); i++) {
            JSONObject m = matches.getJSONObject(i);
            String home = m.optString("team1"), away = m.optString("team2");
            if (!team.equals(home) && !team.equals(away)) continue;
            LocalDate date;
            try { date = LocalDate.parse(m.optString("date")); } catch (Exception undated) { continue; }
            String time = m.optString("time", "");
            boolean hasTime = time.matches("\\d{1,2}:\\d{2}");
            // Times are the league's local time (London is an hour behind Poland).
            long startsAt = hasTime
                    ? date.atTime(LocalTime.parse(time.length() == 4 ? "0" + time : time)).atZone(zone).toInstant().toEpochMilli()
                    : date.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(); // date only, see LeagueCatalogClient
            if (startsAt < fromMillis) continue;
            Matcher round = ROUND.matcher(m.optString("round"));
            result.add(new IcsMatchParser.Match(home, away, startsAt, hasTime,
                    round.find() ? Integer.parseInt(round.group(1)) : -1, country.league, ""));
        }
        // European cups from kalendarzeligowe.pl, shown under our club name.
        try {
            int id = europeanTeamId(team);
            if (id > 0) for (IcsMatchParser.Match m : LeagueCatalogClient.apiMatches(id, fromMillis, EUROPEAN)) {
                boolean home = m.homeTeamId == id;
                result.add(new IcsMatchParser.Match(home ? team : m.home, home ? m.away : team, m.startsAt,
                        m.hasTime, m.round, m.competition, m.stage, m.homeTeamId, m.awayTeamId,
                        m.homeCrestVer, m.awayCrestVer));
            }
        } catch (Exception cupsUnavailable) {
            android.util.Log.w("LinarTV", "European cup fixtures unavailable", cupsUnavailable);
        }
        result.sort((a, b) -> Long.compare(a.startsAt, b.startsAt));
        return result;
    }

    /** kalendarzeligowe.pl id of a foreign club playing in European cups this season, or 0. */
    static int europeanTeamId(String team) throws Exception {
        List<int[]> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (JSONObject t : LeagueCatalogClient.europeanTeams()) {
            ids.add(new int[]{t.optInt("id")}); names.add(t.optString("name"));
        }
        int index = bestMatch(team, names);
        return index < 0 ? 0 : ids.get(index)[0];
    }

    /**
     * Index of the name that means the same club: identical words first, otherwise one name's
     * words contained in the other's ("Borussia Dortmund" ~ "BV Borussia 1909 (Dortmund)"),
     * only when exactly one candidate fits.
     */
    static int bestMatch(String team, List<String> candidates) {
        Set<String> wanted = words(team);
        if (wanted.isEmpty()) return -1;
        int subset = -1, subsetCount = 0;
        for (int i = 0; i < candidates.size(); i++) {
            Set<String> other = words(candidates.get(i));
            if (other.isEmpty()) continue;
            if (other.equals(wanted)) return i;
            if (other.containsAll(wanted) || wanted.containsAll(other)) { subset = i; subsetCount++; }
        }
        return subsetCount == 1 ? subset : -1;
    }

    private static final Set<String> FILLER = new HashSet<>(Arrays.asList(
            "fc", "afc", "cf", "sc", "ac", "ssc", "club", "de", "bv", "sv", "the", "cfc", "calcio",
            "as", "us", "ud", "rc", "cd", "sd", "ss", "acf", "ogc", "rcd"));

    // TEST ONLY (2026-10-27): foreign club crests from football-logos.cc (Leo4815162342/football-logos).
    // No license yet - the maintainer will ask the author before any release. Images are only
    // downloaded on the TV, never added to the repository.
    private static final java.util.Map<String, String> LOGOS = new java.util.concurrent.ConcurrentHashMap<>();
    /** Per country page: crest file names and their image URLs. */
    private static final java.util.Map<String, List<String>> LOGO_NAMES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, List<String>> LOGO_URLS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Pattern LOGO = Pattern.compile(
            "https://assets\\.football-logos\\.cc/logos/([a-z-]+)/256x256/([a-z0-9-]+)\\.[0-9a-f]+\\.png");

    /**
     * Crest image URL for a club from any of the five countries (cup opponents included), or
     * null. A club found in more than one country is left without a crest.
     */
    static String logoUrl(String team) {
        String cached = LOGOS.get(team);
        if (cached != null) return cached.isEmpty() ? null : cached;
        if (LOGO_NAMES.size() < COUNTRIES.size()) return null; // pages not read yet
        String found = "";
        for (Country c : COUNTRIES) {
            List<String> names = LOGO_NAMES.get(c.logoPage);
            if (names == null) continue;
            int index = CrestNameMatcher.best(team, names);
            if (index < 0) continue;
            if (!found.isEmpty()) { found = ""; break; }
            found = LOGO_URLS.get(c.logoPage).get(index);
        }
        LOGOS.put(team, found);
        return found.isEmpty() ? null : found;
    }

    /** Reads the five country pages once per app run (network; call off the main thread). */
    static synchronized void loadLogoPages() {
        if (LOGO_NAMES.size() == COUNTRIES.size()) return;
        for (Country country : COUNTRIES) {
            if (LOGO_NAMES.containsKey(country.logoPage)) continue;
            try {
                String page = read("https://football-logos.cc/" + country.logoPage + "/");
                List<String> names = new ArrayList<>(), urls = new ArrayList<>();
                Matcher m = LOGO.matcher(page);
                while (m.find()) {
                    if (!m.group(1).equals(country.logoPage) || names.contains(m.group(2))) continue;
                    names.add(m.group(2)); urls.add(m.group());
                }
                LOGO_URLS.put(country.logoPage, urls);
                LOGO_NAMES.put(country.logoPage, names);
            } catch (Exception unavailable) {
                android.util.Log.w("LinarTV", "Foreign crests unavailable: " + country.logoPage, unavailable);
            }
        }
        LOGOS.clear();
    }

    static Set<String> words(String name) {
        String plain = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('ł', 'l');
        Set<String> result = new HashSet<>();
        for (String w : plain.split("[^a-z0-9]+"))
            if (!w.isEmpty() && !FILLER.contains(w) && !w.matches("\\d+")) result.add(w);
        return result;
    }

    private static JSONArray leagueMatches(String code) throws Exception {
        String season = season(System.currentTimeMillis());
        JSONObject file = new JSONObject(read(RAW + season + "/" + code + ".json"));
        JSONArray matches = file.optJSONArray("matches");
        if (matches == null) throw new IllegalStateException("No matches in " + code);
        return matches;
    }

    private static String read(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);
        try {
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException("openfootball HTTP " + connection.getResponseCode());
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                    if (bytes.size() > 3_000_000) throw new IllegalStateException("openfootball file too large");
                }
                return bytes.toString("UTF-8");
            }
        } finally { connection.disconnect(); }
    }
}
