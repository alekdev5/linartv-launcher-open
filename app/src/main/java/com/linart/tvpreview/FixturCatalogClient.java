package com.linart.tvpreview;

import android.text.Html;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Public league and team links displayed by fixtur.es. Network calls belong on a worker thread. */
final class FixturCatalogClient {
    static final class League {
        final String name, slug;
        League(String name, String slug) { this.name = name; this.slug = slug; }
    }

    static final class Team {
        final String name, slug, logoUrl;
        Team(String name, String slug, String logoUrl) {
            this.name = name; this.slug = slug; this.logoUrl = logoUrl;
        }
        String calendarUrl() { return "https://ics.fixtur.es/v2/" + slug + ".ics"; }
    }

    private static final Pattern LEAGUE = Pattern.compile(
            "<a\\s+href=\"https://fixtur\\.es/en/([a-z0-9-]+)\"[^>]*>(.*?)</a>", Pattern.DOTALL);
    private static final Pattern TEAM = Pattern.compile(
            "<a\\s+href=['\"]https://fixtur\\.es/en/team/([a-z0-9-]+)['\"]\\s+class=['\"]teamPick-tile['\"][^>]*>"
            + ".*?background-image:\\s*url\\((/images/teams/football/[a-z0-9-]+\\.png)\\)"
            + ".*?class=['\"]teamPick-tile-meta['\"][^>]*>(.*?)</div>", Pattern.DOTALL);

    static List<League> leagues() throws Exception {
        String html = fetch("https://fixtur.es/en/premier-league");
        int start = html.indexOf("Football calendar season");
        if (start < 0) throw new IllegalStateException("League catalog missing");
        int end = html.indexOf("</p>", start);
        if (end < 0) throw new IllegalStateException("League catalog incomplete");
        Matcher matcher = LEAGUE.matcher(html.substring(start, end));
        LinkedHashMap<String, League> result = new LinkedHashMap<>();
        while (matcher.find()) {
            String slug = matcher.group(1);
            result.put(slug, new League(clean(matcher.group(2)), slug));
        }
        if (result.isEmpty()) throw new IllegalStateException("No leagues found");
        return new ArrayList<>(result.values());
    }

    static List<Team> teams(League league) throws Exception {
        if (!league.slug.matches("[a-z0-9-]+")) throw new IllegalArgumentException("Invalid league");
        String html = fetch("https://fixtur.es/en/" + league.slug);
        Matcher matcher = TEAM.matcher(html);
        LinkedHashMap<String, Team> result = new LinkedHashMap<>();
        while (matcher.find()) {
            String slug = matcher.group(1);
            result.put(slug, new Team(clean(matcher.group(3)), slug,
                    "https://fixtur.es" + matcher.group(2)));
        }
        return new ArrayList<>(result.values());
    }

    private static String clean(String html) {
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim();
    }

    static String fetch(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV)");
        try {
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException("Catalog HTTP " + connection.getResponseCode());
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    if (output.size() > 2_000_000) throw new IllegalStateException("Catalog too large");
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally { connection.disconnect(); }
    }
}
