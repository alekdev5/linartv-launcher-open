package com.linart.tvpreview;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bundled fixture crests plus a quiet, version-aware refresh of the source images. */
final class CrestRepository {
    interface Listener { void onCrestChanged(String name); }
    private static final long CHECK_INTERVAL = 7L * 24 * 60 * 60 * 1000;
    private static final String TEAMS = "https://kalendarzeligowe.pl/api/teams?search=";
    private static final String OFFICIAL = "https://ekstraklasa.org/kluby/";
    private final Context context;
    private final SharedPreferences prefs;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Entry> entries = new HashMap<>();
    private boolean running;

    private static final class Entry {
        final String name, league, asset;
        String source, url;
        int teamId;
        Bitmap bitmap;
        boolean loading;
        Entry(String name, String league, String asset, String source, int teamId, String url) {
            this.name = name; this.league = league; this.asset = asset;
            this.source = source; this.teamId = teamId; this.url = url;
        }
    }

    CrestRepository(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        prefs = this.context.getSharedPreferences("club_crests", Context.MODE_PRIVATE);
        try {
            String json = new String(read(this.context.getAssets().open("crests.json"), 100_000), StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                Entry entry = new Entry(item.getString("name"), item.getString("league"), item.getString("asset"),
                        item.getString("source"), item.optInt("teamId"), item.getString("url"));
                entry.url = prefs.getString(versionKey(entry), entry.url);
                entries.put(key(entry.name, entry.league), entry);
            }
        } catch (Exception ignored) { }
    }

    private static String key(String name, String league) { return league + "|" + name; }
    private static String versionKey(Entry entry) { return "url_" + key(entry.name, entry.league); }
    private File cacheFile(Entry entry) {
        File folder = new File(context.getFilesDir(), "crests");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, Integer.toHexString(key(entry.name, entry.league).hashCode()) + ".img");
    }

    Bitmap get(String name, String league) {
        Entry entry = entries.get(key(name, league));
        if (entry == null) return null;
        if (entry.bitmap != null && !entry.bitmap.isRecycled()) return entry.bitmap;
        if (!entry.loading) {
            entry.loading = true;
            String requestedUrl = entry.url;
            new Thread(() -> {
                Bitmap loaded = load(entry);
                main.post(() -> {
                    entry.loading = false;
                    if (entries.get(key(entry.name, entry.league)) != entry
                            || !requestedUrl.equals(entry.url)) {
                        if (loaded != null) loaded.recycle();
                        return;
                    }
                    entry.bitmap = loaded;
                    if (loaded != null) listener.onCrestChanged(entry.name);
                });
            }, "crest-decode").start();
        }
        return null;
    }

    private Bitmap load(Entry entry) {
        File cached = cacheFile(entry);
        try {
            boolean bundledPreferred = entry.asset != null && entry.asset.startsWith("crests/curated/")
                    && ("curated".equals(entry.source) || !entry.url.contains("width=960"));
            Bitmap bitmap = null;
            if (!bundledPreferred && cached.isFile()) bitmap = decode(new java.io.FileInputStream(cached));
            if (bitmap == null && entry.asset != null)
                bitmap = decode(context.getAssets().open(entry.asset));
            if (bitmap == null && bundledPreferred && cached.isFile())
                bitmap = decode(new java.io.FileInputStream(cached));
            return bitmap;
        } catch (Exception ignored) { }
        return null;
    }

    private static Bitmap decode(InputStream stream) throws Exception {
        try (InputStream input = stream) {
            byte[] data = read(input, 5_000_000);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            if (bounds.outWidth < 16 || bounds.outHeight < 16 || bounds.outWidth > 6000 || bounds.outHeight > 6000)
                return null;
            BitmapFactory.Options scaled = new BitmapFactory.Options();
            scaled.inSampleSize = 1;
            scaled.inScaled = false;
            scaled.inPreferredConfig = Bitmap.Config.ARGB_8888;
            while (Math.max(bounds.outWidth, bounds.outHeight) / scaled.inSampleSize > 2048)
                scaled.inSampleSize *= 2;
            return BitmapFactory.decodeByteArray(data, 0, data.length, scaled);
        }
    }

    void refreshIfDue(List<IcsMatchParser.Match> firstClub, List<IcsMatchParser.Match> secondClub, boolean forced) {
        long now = System.currentTimeMillis();
        if (running || !forced && now - prefs.getLong("last_check", 0) < CHECK_INTERVAL) return;
        Map<String, String> clubs = new HashMap<>();
        for (IcsMatchParser.Match match : firstClub) {
            clubs.put(match.home, "ekstraklasa"); clubs.put(match.away, "ekstraklasa");
        }
        for (IcsMatchParser.Match match : secondClub) {
            clubs.put(match.home, "iii_liga"); clubs.put(match.away, "iii_liga");
        }
        if (clubs.isEmpty()) return;
        running = true;
        prefs.edit().putLong("last_check", now).apply();
        new Thread(() -> refresh(clubs), "club-crest-refresh").start();
    }

    private void refresh(Map<String, String> clubs) {
        Map<String, String> official = officialCrests();
        for (Map.Entry<String, String> club : clubs.entrySet()) {
            String name = club.getKey(), league = club.getValue();
            Entry old = entries.get(key(name, league));
            String source = old == null ? ("ekstraklasa".equals(league) && official.containsKey(alias(name))
                    ? "ekstraklasa" : "kalendarzeligowe") : old.source;
            if ("curated".equals(source)) continue;
            String url = "ekstraklasa".equals(source) ? official.get(alias(name)) : null;
            int teamId = old == null ? 0 : old.teamId;
            try {
                if (url == null && !"ekstraklasa".equals(source)) {
                    JSONObject team = findTeam(name);
                    if (team == null) continue;
                    teamId = team.getInt("id");
                    url = "https://kalendarzeligowe.pl/api/teams/" + teamId + "/logo";
                    String version = team.optString("crestVer", "");
                    if (!version.isEmpty() && !"null".equals(version)) url += "?v=" + version;
                }
                if (url == null) continue;
                Entry target = old == null ? new Entry(name, league, null, source, teamId, url) : old;
                if (old != null && old.asset != null && old.asset.startsWith("crests/curated/")
                        && sameSourceImage(url, old.url)) continue;
                if (old != null && url.equals(old.url) && (old.asset != null || cacheFile(old).isFile())) continue;
                byte[] image = fetch(url, 5_000_000);
                if (!isImage(image) || BitmapFactory.decodeByteArray(image, 0, image.length) == null) continue;
                File destination = cacheFile(target);
                File temp = new File(destination.getParentFile(), destination.getName() + ".tmp");
                try (FileOutputStream output = new FileOutputStream(temp)) { output.write(image); }
                if (destination.exists() && !destination.delete()) continue;
                if (!temp.renameTo(destination)) continue;
                final String verifiedUrl = url;
                final int verifiedId = teamId;
                main.post(() -> {
                    target.url = verifiedUrl;
                    target.teamId = verifiedId;
                    target.bitmap = null;
                    entries.put(key(name, league), target);
                    prefs.edit().putString(versionKey(target), verifiedUrl).apply();
                    listener.onCrestChanged(name);
                });
            } catch (Exception ignored) { }
        }
        main.post(() -> running = false);
    }

    private JSONObject findTeam(String name) throws Exception {
        JSONArray array = new JSONArray(new String(fetch(TEAMS + URLEncoder.encode(name, "UTF-8"), 100_000), StandardCharsets.UTF_8));
        for (int i = 0; i < array.length(); i++) {
            JSONObject team = array.getJSONObject(i);
            if (name.equalsIgnoreCase(team.optString("name")) && team.optBoolean("hasLogo")) return team;
        }
        return null;
    }

    private Map<String, String> officialCrests() {
        Map<String, String> result = new HashMap<>();
        try {
            String html = new String(fetch(OFFICIAL, 500_000), StandardCharsets.UTF_8);
            Matcher match = Pattern.compile("<img[^>]+src=\"(https://media\\.cms\\.ekstraklasa\\.org/[^\"]+)\"[^>]+alt=\"([^\"]+)\"").matcher(html);
            while (match.find()) result.put(match.group(2), match.group(1).split("\\?")[0] + "?width=960&format=webp");
        } catch (Exception ignored) { }
        return result;
    }

    private static String alias(String name) {
        return "Zagłębie Lubin".equals(name) ? "KGHM Zagłębie Lubin" : name;
    }

    private static boolean sameSourceImage(String a, String b) {
        return a != null && b != null && !b.isEmpty()
                && a.split("\\?")[0].equals(b.split("\\?")[0]);
    }

    private static byte[] fetch(String url, int limit) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV) AppleWebKit/537.36 Chrome/128.0.0.0 Safari/537.36");
        connection.setRequestProperty("Referer", "https://kalendarzeligowe.pl/");
        try {
            if (connection.getResponseCode() != 200) throw new IllegalStateException("HTTP " + connection.getResponseCode());
            return read(connection.getInputStream(), limit);
        } finally { connection.disconnect(); }
    }

    private static byte[] read(InputStream stream, int limit) throws Exception {
        try (InputStream input = stream; ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
                if (bytes.size() > limit) throw new IllegalStateException("Crest response too large");
            }
            return bytes.toByteArray();
        }
    }

    private static boolean isImage(byte[] image) {
        return image.length >= 12 && (image[0] == (byte) 0x89 && image[1] == 'P' && image[2] == 'N' && image[3] == 'G'
                || image[0] == 'G' && image[1] == 'I' && image[2] == 'F'
                || image[0] == (byte) 0xff && image[1] == (byte) 0xd8
                || image[0] == 'R' && image[1] == 'I' && image[8] == 'W' && image[9] == 'E');
    }
}
