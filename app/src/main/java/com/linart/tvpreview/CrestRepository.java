package com.linart.tvpreview;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Club identity and cached artwork are independent of the selected league. */
final class CrestRepository {
    interface Listener { void onCrestChanged(String name); }
    private static final long REFRESH_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long RETRY_MS = 5L * 60 * 1000;
    private static final String TEAMS = "https://kalendarzeligowe.pl/api/teams?search=";
    private final Context context;
    private final SharedPreferences prefs;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final Map<String, Entry> bundled = new HashMap<>(), requestedNames = new HashMap<>();
    private final Map<Integer, Entry> byId = new HashMap<>();
    private final CrestNameIndex<Entry> names = new CrestNameIndex<>();
    private volatile boolean closed;
    private static final class Entry {
        final String name, asset;
        final Set<String> watchers = new HashSet<>();
        int id;
        Bitmap bitmap;
        boolean loading;
        long nextCheck;
        Entry(String name, String asset, int id) { this.name = name; this.asset = asset; this.id = id; }
    }
    CrestRepository(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        prefs = this.context.getSharedPreferences("club_crests", Context.MODE_PRIVATE);
        try {
            JSONArray rows = new JSONArray(new String(read(this.context.getAssets().open("crests.json"), 100_000), StandardCharsets.UTF_8));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                Entry e = new Entry(row.getString("name"), row.getString("asset"), row.optInt("teamId", 0));
                bundled.put(key(e.name, row.getString("league")), e);
                names.add(e.name, e);
                if (e.id > 0) byId.put(e.id, e);
                JSONArray aliases = row.optJSONArray("aliases");
                if (aliases != null) for (int n = 0; n < aliases.length(); n++) names.add(aliases.getString(n), e);
            }
        } catch (Exception e) { android.util.Log.w("LinarTV", "Bundled crest index unavailable", e); }
    }
    static String leagueKey(String slug) {
        if ("1-liga".equals(slug)) return "i_liga";
        if ("2-liga".equals(slug)) return "ii_liga";
        if ("3-liga-grupa-4".equals(slug)) return "iii_liga";
        return slug;
    }
    private static String key(String name, String league) { return league + "|" + CrestNameIndex.normalize(name); }
    Bitmap get(String name, String league, int id) { return get(name, league, id, false); }
    private Bitmap get(String name, String league, int id, boolean force) {
        if (closed) return null;
        Entry e = bundled.get(key(name, league));
        if (e == null) {
            Entry candidate = names.find(name);
            if (candidate != null && candidate.asset != null) e = candidate;
        }
        if (e != null && id > 0 && e.id > 0 && e.id != id) e = null;
        if (e == null && id > 0) e = byId.get(id);
        if (e == null && id == 0) e = requestedNames.get(CrestNameIndex.normalize(name));
        if (e == null) {
            int knownId = id > 0 ? id : prefs.getInt("club_id_" + CrestNameIndex.normalize(name), 0);
            e = knownId > 0 ? byId.get(knownId) : null;
            if (e == null) {
                e = new Entry(name, null, knownId);
                requestedNames.put(CrestNameIndex.normalize(name), e);
                if (knownId > 0) byId.put(knownId, e);
            }
        }
        if (e.id == 0 && id > 0) e.id = id;
        if (e.id > 0) byId.put(e.id, e);
        e.watchers.add(name);
        long now = System.currentTimeMillis();
        if (!e.loading && (e.bitmap == null || e.asset == null && (force || now >= e.nextCheck))
                && (force || now >= e.nextCheck)) request(e, force);
        return e.bitmap;
    }
    private File cacheFile(int id) {
        File dir = new File(context.getFilesDir(), "crests-by-team");
        dir.mkdirs();
        return new File(dir, "team-" + id + ".img");
    }
    private void request(Entry e, boolean force) {
        e.loading = true;
        int id = e.id;
        try { workers.execute(() -> load(e, id, force)); }
        catch (RejectedExecutionException stopped) { e.loading = false; }
    }
    private void load(Entry e, int id, boolean force) {
        try {
            if (closed) return;
            if (e.asset != null) {
                Bitmap image = decode(context.getAssets().open(e.asset));
                finish(e, id, image, image == null ? RETRY_MS : Long.MAX_VALUE, "bundled");
                return;
            }
            if (id <= 0) id = findTeamId(e.name);
            if (id <= 0) { finish(e, 0, null, RETRY_MS, "missing"); return; }
            File cached = cacheFile(id);
            Bitmap image = cached.isFile() ? decode(new java.io.FileInputStream(cached)) : null;
            long now = System.currentTimeMillis(), downloaded = prefs.getLong("downloaded_" + id, 0);
            if (image != null) {
                if (!force && now - downloaded < REFRESH_MS) {
                    finish(e, id, image, REFRESH_MS - Math.max(0, now - downloaded), "disk"); return;
                }
                publish(e, id, image, "disk");
            }
            if (!force && now < prefs.getLong("retry_" + id, 0)) {
                finish(e, id, null, RETRY_MS, "cooldown"); return;
            }
            prefs.edit().putLong("retry_" + id, now + RETRY_MS).apply();
            byte[] bytes = fetch("https://kalendarzeligowe.pl/api/teams/" + id + "/logo", 5_000_000);
            image = decode(new ByteArrayInputStream(bytes));
            if (image == null) throw new IllegalStateException("Unsupported crest image");
            File temporary = new File(cached.getParentFile(), cached.getName() + ".tmp");
            try {
                Files.write(temporary.toPath(), bytes);
                Files.move(temporary.toPath(), cached.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally { if (temporary.isFile()) temporary.delete(); }
            prefs.edit().putLong("downloaded_" + id, now)
                    .putInt("club_id_" + CrestNameIndex.normalize(e.name), id).apply();
            finish(e, id, image, REFRESH_MS, "network");
        } catch (Exception unavailable) { finish(e, id, null, RETRY_MS, "unavailable"); }
    }
    private void publish(Entry e, int id, Bitmap image, String source) {
        main.post(() -> {
            if (closed) { if (image != null && image != e.bitmap) image.recycle(); return; }
            if (id > 0) { e.id = id; byId.put(id, e); }
            if (image != null) {
                e.bitmap = image;
                for (String name : e.watchers) listener.onCrestChanged(name);
            }
            if ((context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
                android.util.Log.d("LinarTVCrests", "source=" + source + " teamId=" + id);
        });
    }
    private void finish(Entry e, int id, Bitmap image, long delay, String source) {
        main.post(() -> {
            e.loading = false;
            e.nextCheck = delay == Long.MAX_VALUE ? Long.MAX_VALUE : System.currentTimeMillis() + delay;
        });
        publish(e, id, image, source);
    }
    void refreshIfDue(List<IcsMatchParser.Match> first, String firstLeague,
                      List<IcsMatchParser.Match> second, String secondLeague, boolean forced) {
        prefetch(first, firstLeague, forced); prefetch(second, secondLeague, forced);
    }
    private void prefetch(List<IcsMatchParser.Match> matches, String league, boolean forced) {
        int count = 0;
        long now = System.currentTimeMillis();
        for (IcsMatchParser.Match match : matches) {
            if (match.startsAt + 105L * 60 * 1000 < now) continue;
            get(match.home, leagueKey(league), match.homeTeamId, forced);
            get(match.away, leagueKey(league), match.awayTeamId, forced);
            if (++count == 2) break;
        }
    }
    private int findTeamId(String name) throws Exception {
        JSONArray rows = new JSONArray(new String(fetch(TEAMS + URLEncoder.encode(name, "UTF-8"), 100_000), StandardCharsets.UTF_8));
        int found = 0;
        String normalized = CrestNameIndex.normalize(name);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (!row.optBoolean("hasLogo") || !normalized.equals(CrestNameIndex.normalize(row.optString("name")))) continue;
            int id = row.optInt("id", 0);
            if (found > 0 && found != id) return 0;
            found = id;
        }
        return found;
    }
    void close() {
        closed = true;
        workers.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }

    private static Bitmap decode(InputStream stream) throws Exception {
        try (InputStream input = stream) {
            byte[] data = read(input, 5_000_000);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            if (bounds.outWidth < 16 || bounds.outHeight < 16 || bounds.outWidth > 16000 || bounds.outHeight > 16000)
                return null;
            BitmapFactory.Options scaled = new BitmapFactory.Options();
            scaled.inSampleSize = 1;
            scaled.inScaled = false;
            scaled.inPreferredConfig = Bitmap.Config.ARGB_8888;
            while (Math.max(bounds.outWidth, bounds.outHeight) / scaled.inSampleSize > 512)
                scaled.inSampleSize *= 2;
            return BitmapFactory.decodeByteArray(data, 0, data.length, scaled);
        }
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
}
