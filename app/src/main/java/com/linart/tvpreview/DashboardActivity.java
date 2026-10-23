package com.linart.tvpreview;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.HashMap;
import java.util.HashSet;
import java.util.WeakHashMap;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/** A safe, native TV preview. It intentionally does not claim the Android HOME intent. */
public final class DashboardActivity extends Activity {
    private static final int W = 1672;
    private static final int H = 941;
    private static final int DASHBOARD_CARD_Y = 328;
    private static final int SINGLE_COLUMN_X = 1114;
    private static final int SINGLE_WEATHER_Y = 145;
    private static final int SINGLE_MATCH_Y = 445;
    private static final Locale PL = new Locale("pl", "PL");
    private static final Locale EN = Locale.ENGLISH;
    private static final int TILE_BG = 0x80152146;
    private static final int TILE_FOCUS_BG = 0xB0335D91;
    private static final int FOCUS_RING = 0xFF9FD5FF;
    private static final String[] DOCK_BRANDS = {"YouTube", "Netflix", "CANAL+", "Spotify"};
    private static final String[] DOCK_PACKAGES = {"com.google.android.youtube.tv", "com.netflix.ninja",
            "com.canal.android.canal", "com.spotify.tv.android"};
    private static final String[] CLEANUP_PACKAGES = {"com.google.android.youtube.tv", "com.netflix.ninja",
            "com.canal.android.canal", "com.spotify.tv.android", "com.tvp.vodtv.tv",
            "com.google.android.youtube.tvkids", "pl.cda.tv"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private float scale;
    private FrameLayout root;
    private TextView clock;
    private TextView date;
    private boolean night;
    private boolean dashboardVisible;
    private int ink;
    private int muted;
    private int panel;
    private int outline;
    private int detailText;
    private int detailMuted;
    private int detailCard;
    private int detailAstronomy;
    private final HashMap<String, Bitmap> logoCache = new HashMap<>();
    private final List<View> dockCards = new ArrayList<>();
    private final List<View> dockGlows = new ArrayList<>();
    private final List<List<View>> extraAppRows = new ArrayList<>();
    private int drawerFocusRow;
    private int drawerFocusColumn;
    private final List<View> suspendedFocusables = new ArrayList<>();
    private FrameLayout drawerBackdrop;
    private ImageView drawerBlurImage;
    private Bitmap drawerBlurBitmap;
    private TextView drawerTitle;
    private ScrollView appDrawer;
    private LinearLayout drawerRows;
    private boolean drawerRowsLoaded;
    private static final int APP_ROW_HEIGHT = 142;
    private ImageView dockExpand;
    private View lastDockFocus;
    private View firstExtraApp;
    private boolean appDrawerOpen;
    private CrestRepository crests;
    private final HashMap<String, List<CrestSlot>> crestSlots = new HashMap<>();
    private final WeakHashMap<Bitmap, Bitmap> scaledCrests = new WeakHashMap<>();
    private static final class CrestSlot {
        final ImageView image;
        final View fallback;
        final String name, league;
        CrestSlot(ImageView image, View fallback, String name, String league) {
            this.image = image; this.fallback = fallback; this.name = name; this.league = league;
        }
    }
    private Typeface weatherTypeface;
    private JSONObject forecast;
    private long forecastUpdatedAt;
    private long forecastAttemptedAt;
    private boolean forecastLoading;
    private static final long WEATHER_REFRESH_MS = 30L * 60 * 1000;
    private String cityName;
    private double cityLatitude;
    private double cityLongitude;
    private static final long CALENDAR_REFRESH_MS = 24L * 60 * 60 * 1000;
    private static final long CALENDAR_PARSE_LOOKBACK_MS = 26L * 60 * 60 * 1000;
    private static final long MATCH_VISIBLE_AFTER_KICKOFF_MS = 105L * 60 * 1000;
    private static final class CalendarFeed {
        final String key, url, club, league, leagueSlug, requiredDescription;
        List<IcsMatchParser.Match> matches = new ArrayList<>();
        long updatedAt;
        long attemptedAt;
        boolean loading;
        int selected;
        long renderedFirstMatchStart;
        boolean renderedLive;
        View tileButton;
        final List<View> cardViews = new ArrayList<>();
        CalendarFeed(String key, String url, String club, String league, String leagueSlug) {
            this.key = key; this.url = url; this.club = club; this.league = league;
            this.leagueSlug = leagueSlug; this.requiredDescription = null;
        }
        boolean configured() { return !url.isEmpty() && !club.isEmpty(); }
    }
    private CalendarFeed clubOneCalendar;
    private CalendarFeed clubTwoCalendar;
    private boolean setupShown;
    private PhotoUploadServer photoUploadServer;
    private static final long SCREENSAVER_IDLE_MS = 5 * 60_000L;
    private static final long SCREENSAVER_SLIDE_MS = 30_000L;
    private boolean popupOpen;
    private int popupDepth;
    private boolean english;
    private boolean fahrenheit;
    private Locale displayLocale() { return english ? EN : PL; }
    private String tr(String polish, String englishText) { return english ? englishText : polish; }
    private boolean screensaverActive;
    private boolean suppressSaverKeyUp;
    private boolean suppressSaverTouch;
    private FrameLayout screensaver;
    private ImageView screensaverImage;
    private LinearLayout screensaverInfo;
    private TextView screensaverClock;
    private TextView screensaverDate;
    private Bitmap screensaverBitmap;
    private final List<String> screensaverChoices = new ArrayList<>();
    private int screensaverIndex;
    private final Runnable startScreensaver = () -> {
        if (!popupOpen && !screensaverActive) showScreensaver();
    };
    private final Runnable advanceScreensaver = new Runnable() {
        @Override public void run() {
            if (!screensaverActive) return;
            screensaverIndex = (screensaverIndex + 1) % screensaverChoices.size();
            showScreensaverSlide();
            if (screensaverActive) handler.postDelayed(this, SCREENSAVER_SLIDE_MS);
        }
    };
    private final Runnable updateTime = new Runnable() {
        @Override public void run() {
            Date now = new Date();
            clock.setText(new SimpleDateFormat("HH:mm", PL).format(now));
            String formatted = new SimpleDateFormat("EEEE, d MMMM", displayLocale()).format(now);
            date.setText(Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1));
            if (night != isNight()) { buildScreen(); return; }
            if (!popupOpen && (matchCardNeedsUpdate(clubOneCalendar, now.getTime())
                    || matchCardNeedsUpdate(clubTwoCalendar, now.getTime()))) {
                buildScreen();
                return;
            }
            refreshWeather();
            refreshCalendar(clubOneCalendar);
            refreshCalendar(clubTwoCalendar);
            crests.refreshIfDue(clubOneCalendar.matches, clubTwoCalendar.matches, false);
            handler.postDelayed(this, 60_000);
        }
    };

    private final Runnable updateMatchCards = new Runnable() {
        @Override public void run() {
            if (!dashboardVisible) return;
            if (popupOpen) {
                handler.postDelayed(this, 1000);
                return;
            }
            long now = System.currentTimeMillis();
            if (matchCardNeedsUpdate(clubOneCalendar, now)
                    || matchCardNeedsUpdate(clubTwoCalendar, now)) buildScreen();
            else scheduleNextMatchBoundary();
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        scale = Math.min(metrics.widthPixels / (float) W, metrics.heightPixels / (float) H);
        SharedPreferences saved = getPreferences(MODE_PRIVATE);
        english = "en".equals(saved.getString("language", "pl"));
        fahrenheit = "f".equals(saved.getString("temperature_unit", "c"));
        clubOneCalendar = configuredCalendar(saved, 1);
        clubTwoCalendar = configuredCalendar(saved, 2);
        forecastUpdatedAt = saved.getLong("weather_updated", 0);
        try { forecast = new JSONObject(saved.getString("weather_json", "")); }
        catch (Exception ignored) { forecast = null; }
        if (forecast != null && !"Open-Meteo".equals(forecast.optString("provider"))) {
            forecast = null;
            forecastUpdatedAt = 0;
        }
        if (forecast != null && (forecast.optJSONArray("hourly") == null
                || forecast.optInt("icon_format") != 2)) forecastUpdatedAt = 0;
        cityName = saved.getString("city_name", "Kraków");
        cityLatitude = Double.longBitsToDouble(saved.getLong("city_lat", Double.doubleToLongBits(50.0614)));
        cityLongitude = Double.longBitsToDouble(saved.getLong("city_lon", Double.doubleToLongBits(19.9366)));
        if (forecast != null && (!cityName.equals(forecast.optString("city"))
                || forecast.optJSONObject("current") == null)) forecastUpdatedAt = 0;
        crests = new CrestRepository(this, this::updateCrestViews);
        restoreCalendar(clubOneCalendar, saved);
        restoreCalendar(clubTwoCalendar, saved);
        buildScreen();
        if (!clubOneCalendar.configured()) handler.post(() -> {
            if (!isFinishing() && !setupShown) showTeamSetup();
        });
    }

    @Override protected void onResume() {
        super.onResume();
        dashboardVisible = true;
        handler.removeCallbacks(updateTime);
        updateTime.run();
        scheduleNextMatchBoundary();
        resetScreensaverTimer();
    }

    @Override protected void onPause() {
        dashboardVisible = false;
        handler.removeCallbacks(updateTime);
        handler.removeCallbacks(updateMatchCards);
        handler.removeCallbacks(startScreensaver);
        hideScreensaver();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (photoUploadServer != null) {
            photoUploadServer.close();
            photoUploadServer = null;
        }
        super.onDestroy();
    }

    private boolean isNight() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= 20 || hour < 8;
    }

    private void refreshWeather() {
        long now = System.currentTimeMillis();
        if (forecastLoading || isForecastCurrent() && now - forecastUpdatedAt < WEATHER_REFRESH_MS
                || now - forecastAttemptedAt < 5 * 60_000L) return;
        forecastLoading = true;
        forecastAttemptedAt = now;
        new Thread(() -> {
            JSONObject result = null;
            String requestedCity = cityName;
            try { result = PublicForecastClient.fetch(cityLatitude, cityLongitude).put("city", requestedCity); }
            catch (Exception e) {
                android.util.Log.w("LinarTV", "Weather refresh failed", e);
                result = null;
            }
            final JSONObject fetched = result;
            handler.post(() -> {
                forecastLoading = false;
                if (fetched != null && requestedCity.equals(cityName)) {
                    forecast = fetched;
                    forecastUpdatedAt = System.currentTimeMillis();
                    getPreferences(MODE_PRIVATE).edit().putString("weather_json", fetched.toString())
                            .putLong("weather_updated", forecastUpdatedAt).apply();
                    buildScreen();
                }
            });
        }, "weather-fetch").start();
    }

    private JSONObject currentWeather() {
        return forecast == null ? null : forecast.optJSONObject("current");
    }

    private String currentWeatherTime() {
        JSONObject current = currentWeather();
        if (current == null || current.optLong("time") <= 0) return tr("brak danych", "no data");
        SimpleDateFormat format = new SimpleDateFormat(english ? "d MMM HH:mm" : "d.MM HH:mm", displayLocale());
        format.setTimeZone(forecastZone());
        return format.format(new Date(current.optLong("time")));
    }

    private TimeZone forecastZone() {
        return TimeZone.getTimeZone(forecast == null ? TimeZone.getDefault().getID()
                : forecast.optString("timezone", TimeZone.getDefault().getID()));
    }

    private String observed(String field, String unit) {
        JSONObject current = currentWeather();
        if (current == null || !current.has(field) || current.isNull(field)) return "—";
        double value = current.optDouble(field, Double.NaN);
        if (Double.isNaN(value)) return "—";
        String text = Math.abs(value - Math.rint(value)) < 0.05 ? String.valueOf(Math.round(value))
                : String.format(PL, "%.1f", value);
        return text + unit;
    }

    private void restoreCalendar(CalendarFeed feed, SharedPreferences saved) {
        if (!feed.configured()) return;
        feed.updatedAt = saved.getLong(feed.key + "_calendar_updated", 0);
        String ics = saved.getString(feed.key + "_calendar_ics", "");
        if (ics.startsWith("BEGIN:VCALENDAR"))
            feed.matches = IcsMatchParser.parse(ics, feed.requiredDescription,
                    System.currentTimeMillis() - CALENDAR_PARSE_LOOKBACK_MS);
    }

    private CalendarFeed configuredCalendar(SharedPreferences saved, int slot) {
        String key = "club" + slot;
        return new CalendarFeed(key, saved.getString(key + "_url", ""),
                saved.getString(key + "_name", ""), saved.getString(key + "_league", ""),
                saved.getString(key + "_league_slug", ""));
    }

    private void refreshCalendar(CalendarFeed feed) {
        if (!feed.configured()) return;
        long now = System.currentTimeMillis();
        if (feed.loading || now - feed.updatedAt < CALENDAR_REFRESH_MS
                || now - feed.attemptedAt < 30 * 60 * 1000) return;
        feed.loading = true;
        feed.attemptedAt = now;
        new Thread(() -> {
            String ics = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(feed.url).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                if (connection.getResponseCode() != 200) throw new IllegalStateException("Calendar HTTP " + connection.getResponseCode());
                try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = stream.read(buffer)) != -1) {
                        bytes.write(buffer, 0, count);
                        if (bytes.size() > 500_000) throw new IllegalStateException("Calendar too large");
                    }
                    ics = bytes.toString("UTF-8");
                }
                if (!ics.startsWith("BEGIN:VCALENDAR") || !ics.contains("END:VCALENDAR"))
                    throw new IllegalStateException("Invalid calendar");
            } catch (Exception ignored) { ics = null; }
            finally { if (connection != null) connection.disconnect(); }
            final String fetched = ics;
            handler.post(() -> {
                if (feed != clubOneCalendar && feed != clubTwoCalendar) return;
                feed.loading = false;
                if (fetched != null) {
                    feed.matches = IcsMatchParser.parse(fetched, feed.requiredDescription,
                            System.currentTimeMillis() - CALENDAR_PARSE_LOOKBACK_MS);
                    feed.updatedAt = System.currentTimeMillis();
                    feed.selected = 0;
                    getPreferences(MODE_PRIVATE).edit().putString(feed.key + "_calendar_ics", fetched)
                            .putLong(feed.key + "_calendar_updated", feed.updatedAt).apply();
                    buildScreen();
                    crests.refreshIfDue(clubOneCalendar.matches, clubTwoCalendar.matches, true);
                }
            });
        }, "calendar-" + feed.key).start();
    }

    private boolean isForecastCurrent() {
        if (forecast == null) return false;
        try {
            String firstDay = forecast.getJSONObject("daily").getJSONArray("time").getString(0);
            return firstDay.equals(new SimpleDateFormat("yyyy-MM-dd", PL).format(new Date()));
        } catch (Exception ignored) { return false; }
    }

    private int px(float value) { return Math.round(value * scale); }

    private void place(View view, int x, int y, int w, int h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(w), px(h));
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        root.addView(view, lp);
    }

    private GradientDrawable shape(int fill, int border, int radius) {
        return shape(fill, border, radius, 1);
    }

    private GradientDrawable shape(int fill, int border, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(px(radius));
        d.setStroke(Math.max(1, px(stroke)), border);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold, int x, int y, int w, int h) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        place(view, x, y, w, h);
        return view;
    }

    private void panel(int x, int y, int w, int h) {
        View view = new View(this);
        view.setBackground(shape(panel, outline, 29));
        place(view, x, y, w, h);
    }

    private void line(int x, int y, int width) {
        View view = new View(this);
        view.setBackgroundColor(outline);
        place(view, x, y, width, 1);
    }

    private void verticalLine(int x, int y, int height) {
        View view = new View(this);
        view.setBackgroundColor(outline);
        place(view, x, y, 1, height);
    }

    private TextView action(String label, int x, int y, int w, int h, Runnable click) {
        TextView view = text(label, 19, ink, false, x, y, w, h);
        view.setGravity(Gravity.CENTER);
        view.setFocusable(true);
        view.setClickable(true);
        view.setBackground(shape(panel, outline, 18));
        view.setOnFocusChangeListener((v, hasFocus) -> {
            view.setBackground(shape(hasFocus ? (night ? 0xCC416B9E : 0xFFFFFFFF) : panel,
                    hasFocus ? 0xFF4C9CFF : outline, 18));
            view.setScaleX(hasFocus ? 1.04f : 1f);
            view.setScaleY(hasFocus ? 1.04f : 1f);
        });
        view.setOnClickListener(v -> click.run());
        return view;
    }

    private ImageView logo(int drawable, int x, int y, int w, int h) {
        ImageView image = new ImageView(this);
        int targetWidth = px(w);
        int targetHeight = px(h);
        String cacheKey = drawable + ":" + targetWidth + ":" + targetHeight;
        Bitmap scaled = logoCache.get(cacheKey);
        if (scaled == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap current = BitmapFactory.decodeResource(getResources(), drawable, options);
            if (current != null) {
                float ratio = Math.min(targetWidth / (float) current.getWidth(), targetHeight / (float) current.getHeight());
                int finalWidth = Math.max(1, Math.round(current.getWidth() * ratio));
                int finalHeight = Math.max(1, Math.round(current.getHeight() * ratio));
                while (current.getWidth() > finalWidth * 2 && current.getHeight() > finalHeight * 2) {
                    Bitmap next = Bitmap.createScaledBitmap(current, Math.max(finalWidth, current.getWidth() / 2),
                            Math.max(finalHeight, current.getHeight() / 2), true);
                    if (next != current) current.recycle();
                    current = next;
                }
                Bitmap result = Bitmap.createScaledBitmap(current, finalWidth, finalHeight, true);
                if (result != current) current.recycle();
                scaled = result;
                logoCache.put(cacheKey, scaled);
            }
        }
        if (scaled != null) image.setImageBitmap(scaled);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        place(image, x, y, w, h);
        return image;
    }

    private void buildScreen() {
        if (isFinishing() || isDestroyed()) return;
        boolean restoreScreensaver = screensaverActive;
        if (restoreScreensaver) hideScreensaver();
        boolean restoreDrawer = appDrawerOpen;
        int restoreRow = 0, restoreColumn = 0;
        if (restoreDrawer) {
            View focused = getCurrentFocus();
            for (int row = 0; row < extraAppRows.size(); row++) {
                int column = extraAppRows.get(row).indexOf(focused);
                if (column >= 0) { restoreRow = row; restoreColumn = column; break; }
            }
        }
        final int focusRow = restoreRow, focusColumn = restoreColumn;
        if (drawerBlurBitmap != null) {
            if (drawerBlurImage != null) drawerBlurImage.setImageDrawable(null);
            drawerBlurBitmap.recycle();
            drawerBlurBitmap = null;
        }
        suspendedFocusables.clear();
        crestSlots.clear();
        clubOneCalendar.cardViews.clear();
        clubTwoCalendar.cardViews.clear();
        dockCards.clear();
        dockGlows.clear();
        appDrawerOpen = false;
        lastDockFocus = null;
        night = isNight();
        ink = night ? Color.WHITE : 0xFF15223E;
        muted = night ? 0xFFC6D1E4 : 0xFF34435B;
        panel = night ? 0x80102443 : 0x80FAF7EF;
        outline = night ? 0x888292AF : 0xAAFFFFFF;

        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0B1F3B);
        setContentView(root);
        ImageView wallpaper = new ImageView(this);
        showSavedWallpaper(wallpaper);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));

        View veil = new View(this);
        veil.setBackgroundColor(night ? 0x330A1036 : 0x220A1036);
        root.addView(veil, new FrameLayout.LayoutParams(-1, -1));

        clock = text("", 144, Color.WHITE, false, 60, 26, 590, 161);
        date = text("", 37, Color.WHITE, false, 62, 189, 610, 55);

        circleAction(R.drawable.ic_toolbar_television, tr("Oglądaj telewizję", "Watch TV"), 1238,
                () -> launch("org.droidtv.playtv"));
        circleAction(R.drawable.ic_toolbar_cleanup, tr("Zamknij aplikacje w tle", "Close background apps"), 1138,
                this::closeBackgroundApps);
        circleAction(R.drawable.ic_toolbar_sources, tr("Źródła", "Inputs"), 1338, this::showSources);
        circleAction(R.drawable.ic_toolbar_gallery, tr("Galeria tapet", "Wallpaper gallery"), 1438, this::showWallpaperGallery);
        circleAction(R.drawable.ic_toolbar_settings, tr("Ustawienia", "Settings"), 1538,
                this::showPublicSettings);

        boolean singleColumn = !clubTwoCalendar.configured();
        renderWeatherCard(singleColumn ? SINGLE_COLUMN_X : 56);
        if (!clubOneCalendar.configured()) renderSetupCard();
        else if (singleColumn)
            renderMatchCard(SINGLE_COLUMN_X, clubOneCalendar.club, clubOneCalendar.league, clubOneCalendar);
        else {
            renderMatchCard(585, clubOneCalendar.club, clubOneCalendar.league, clubOneCalendar);
            renderMatchCard(1114, clubTwoCalendar.club, clubTwoCalendar.league, clubTwoCalendar);
        }

        buildAppDock();
        handler.removeCallbacks(updateTime);
        updateTime.run();
        scheduleNextMatchBoundary();
        if (restoreScreensaver) showScreensaver();
        if (restoreDrawer) handler.post(() -> openAppDrawer(focusRow, focusColumn));
    }

    private void colorCard(int x, int y, int w, int h) {
        View card = new View(this);
        card.setBackground(shape(panel, outline, 34));
        place(card, x, y, w, h);
    }

    private void renderSetupCard() {
        int x = SINGLE_COLUMN_X, y = SINGLE_MATCH_Y, w = 500, h = 286;
        View glow = tileGlow(x, y, w, h, 34);
        colorCard(x, y, w, h);
        text(tr("Kalendarz meczów", "Match calendar"), 21, ink, false, x + 30, y + 25, 430, 38);
        text(tr("Wybierz swój klub", "Choose your club"), 34, ink, true, x + 30, y + 95, 440, 52);
        text(tr("Możesz dodać jeden lub dwa kluby z aktualnych lig.",
                "Add one or two clubs from available leagues."), 19,
                ink, false, x + 30, y + 166, 430, 70);
        View action = new View(this);
        action.setFocusable(true);
        action.setClickable(true);
        action.setContentDescription(tr("Wybierz klub do kalendarza meczów", "Choose a club for the match calendar"));
        action.setOnFocusChangeListener((v, focused) -> showTileGlow(glow, focused));
        action.setOnClickListener(v -> showTeamSetup());
        place(action, x, y, w, h);
    }

    private void showPublicSettings() {
        showChoiceDialog(tr("Ustawienia LinarTV ", "LinarTV settings ") + appVersion(),
                new String[]{tr("Wybierz kluby", "Choose clubs"),
                        tr("Miasto pogody: ", "Weather city: ") + cityName,
                        tr("Język: ", "Language: ") + (english ? "English" : "Polski"),
                        tr("Temperatura: ", "Temperature: ") + (fahrenheit ? "°F" : "°C"),
                        tr("Ustawienia systemu TV", "TV system settings")}, which -> {
                    if (which == 0) showTeamSetup();
                    else if (which == 1) showCitySetup();
                    else if (which == 2) showLanguageSetup();
                    else if (which == 3) showTemperatureUnitSetup();
                    else openSettings(Settings.ACTION_SETTINGS);
                });
    }

    private void showTemperatureUnitSetup() {
        showChoiceDialog(tr("Jednostka temperatury", "Temperature unit"),
                new String[]{"°C", "°F"}, which -> {
            boolean selectedFahrenheit = which == 1;
            if (fahrenheit == selectedFahrenheit) return;
            fahrenheit = selectedFahrenheit;
            getPreferences(MODE_PRIVATE).edit().putString("temperature_unit", fahrenheit ? "f" : "c").apply();
            buildScreen();
        });
    }

    private int shownTemperature(double celsius) {
        return (int) Math.round(fahrenheit ? celsius * 9.0 / 5.0 + 32.0 : celsius);
    }

    private CharSequence temperatureLabel(double celsius) {
        return temperatureLabelParts(shownTemperature(celsius) + "°");
    }

    private CharSequence temperatureLabelParts(String value) {
        String text = value.replace('°', (char) (fahrenheit ? 0xf045 : 0xf03c));
        SpannableString label = new SpannableString(text);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == (char) (fahrenheit ? 0xf045 : 0xf03c))
                label.setSpan(new TemperatureUnitSpan(), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return label;
    }

    private final class TemperatureUnitSpan extends MetricAffectingSpan {
        private void apply(TextPaint paint) {
            if (weatherTypeface == null) weatherTypeface = Typeface.createFromAsset(getAssets(),
                    "weathericons-regular-webfont.ttf");
            paint.setTypeface(weatherTypeface);
            paint.setTextSize(paint.getTextSize() * 0.68f);
        }
        @Override public void updateDrawState(TextPaint paint) { apply(paint); }
        @Override public void updateMeasureState(TextPaint paint) { apply(paint); }
    }

    private void showLanguageSetup() {
        showChoiceDialog(tr("Język", "Language"), new String[]{"Polski", "English"}, which -> {
            boolean selectedEnglish = which == 1;
            if (english == selectedEnglish) return;
            english = selectedEnglish;
            getPreferences(MODE_PRIVATE).edit().putString("language", english ? "en" : "pl").apply();
            buildScreen();
        });
    }

    private String appVersion() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (PackageManager.NameNotFoundException e) { return ""; }
    }

    private void showCitySetup() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = popupContent();
        popupTitle(content, tr("Miasto do prognozy pogody", "Weather forecast city"));
        popupDescription(content, tr("Wpisz nazwę miasta i wybierz wynik.",
                "Type a city name and choose a result."));
        EditText query = new EditText(this);
        query.setSingleLine(true);
        query.setTextColor(ink);
        query.setHintTextColor(muted);
        query.setHint(tr("Np. Gdańsk", "E.g. London"));
        query.setText(cityName);
        query.selectAll();
        query.setPadding(px(20), 0, px(20), 0);
        query.setBackground(shape(night ? 0xFF2B4268 : 0xFFE3E9F0,
                night ? 0xFF7899C3 : 0xFFB7C4D5, 15));
        content.addView(query, new LinearLayout.LayoutParams(-1, px(66)));
        TextView status = popupDescription(content, "");
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        ScrollView resultScroll = new ScrollView(this);
        resultScroll.addView(results);
        LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(-1, px(300));
        resultParams.topMargin = px(8);
        content.addView(resultScroll, resultParams);
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        TextView close = setupButton(tr("Zamknij", "Close"));
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        actions.addView(close, new LinearLayout.LayoutParams(px(150), px(58)));
        TextView search = setupButton(tr("Szukaj", "Search"));
        search.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(px(150), px(58));
        searchParams.leftMargin = px(14);
        actions.addView(search, searchParams);
        content.addView(actions);
        search.setOnClickListener(v -> searchCity(query.getText().toString().trim(), dialog, results, status));
        query.setOnEditorActionListener((v, actionId, event) -> {
            search.performClick();
            return true;
        });
        dialog.setContentView(content);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        showDialogWithBlur(dialog);
        dialog.getWindow().setLayout(px(780), -2);
        query.requestFocus();
    }

    private void searchCity(String name, Dialog dialog, LinearLayout results, TextView status) {
        if (name.length() < 2) {
            status.setText(tr("Wpisz co najmniej dwie litery nazwy miasta.",
                    "Enter at least two letters of the city name."));
            return;
        }
        status.setText(tr("Szukanie miasta…", "Searching for city…"));
        results.removeAllViews();
        new Thread(() -> {
            List<PublicForecastClient.Place> places;
            try { places = PublicForecastClient.searchPlaces(name, english ? "en" : "pl"); }
            catch (Exception e) {
                android.util.Log.w("LinarTV", "City search failed", e);
                places = null;
            }
            final List<PublicForecastClient.Place> found = places;
            handler.post(() -> {
                if (isFinishing() || isDestroyed() || !dialog.isShowing()) return;
                if (found == null) {
                    status.setText(tr("Nie udało się wyszukać miasta. Sprawdź połączenie z internetem.",
                            "Could not search for a city. Check your internet connection."));
                    return;
                }
                if (found.isEmpty()) {
                    status.setText(tr("Nie znaleziono miasta: ", "City not found: ") + name);
                    return;
                }
                status.setText(tr("Wybierz właściwe miasto:", "Choose the correct city:"));
                for (PublicForecastClient.Place place : found) {
                    TextView item = setupButton(place.label());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, px(62));
                    params.bottomMargin = px(8);
                    results.addView(item, params);
                    item.setOnClickListener(v -> { saveCity(place); dialog.dismiss(); });
                }
                if (results.getChildCount() > 0) results.getChildAt(0).requestFocus();
            });
        }, "city-search").start();
    }

    private LinearLayout popupContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(36), px(28), px(36), px(30));
        content.setBackground(shape(night ? 0xFF142548 : 0xFFF5F2EA,
                night ? 0xFF899BC6 : 0xFFB7C4D5, 26));
        return content;
    }

    private void popupTitle(LinearLayout content, String value) {
        TextView title = new TextView(this);
        title.setText(value);
        title.setTextColor(ink);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(32));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title);
    }

    private TextView popupDescription(LinearLayout content, String value) {
        TextView description = new TextView(this);
        description.setText(value);
        description.setTextColor(muted);
        description.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, px(8), 0, px(20));
        content.addView(description, params);
        return description;
    }

    private void showChoiceDialog(String title, String[] labels, java.util.function.IntConsumer onChoose) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = popupContent();
        popupTitle(content, title);
        ScrollView scroll = new ScrollView(this);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(choices);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, px(Math.min(520,
                Math.max(90, labels.length * 72))));
        scrollParams.topMargin = px(22);
        content.addView(scroll, scrollParams);
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView button = setupButton(labels[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, px(62));
            params.bottomMargin = px(10);
            choices.addView(button, params);
            button.setOnClickListener(v -> {
                dialog.dismiss();
                onChoose.accept(index);
            });
        }
        TextView close = setupButton(tr("Zamknij", "Close"));
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(px(150), px(54));
        closeParams.gravity = Gravity.END;
        content.addView(close, closeParams);
        dialog.setContentView(content);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        showDialogWithBlur(dialog);
        dialog.getWindow().setLayout(px(780), -2);
        if (choices.getChildCount() > 0) choices.getChildAt(0).requestFocus();
    }

    private void showMessageDialog(String title, String message) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = popupContent();
        popupTitle(content, title);
        popupDescription(content, message);
        TextView close = setupButton(tr("Zamknij", "Close"));
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(px(150), px(54));
        closeParams.gravity = Gravity.END;
        content.addView(close, closeParams);
        dialog.setContentView(content);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        showDialogWithBlur(dialog);
        dialog.getWindow().setLayout(px(780), -2);
        close.requestFocus();
    }

    private void saveCity(PublicForecastClient.Place place) {
        cityName = place.name;
        cityLatitude = place.latitude;
        cityLongitude = place.longitude;
        getPreferences(MODE_PRIVATE).edit().putString("city_name", cityName)
                .putLong("city_lat", Double.doubleToLongBits(cityLatitude))
                .putLong("city_lon", Double.doubleToLongBits(cityLongitude))
                .remove("weather_json").remove("weather_updated").apply();
        forecast = null;
        forecastUpdatedAt = 0;
        forecastAttemptedAt = 0;
        buildScreen();
        refreshWeather();
    }

    private TextView setupButton(String label) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextColor(night ? Color.WHITE : 0xFF15223E);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(21));
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(px(22), 0, px(18), 0);
        button.setBackground(shape(night ? 0xFF2B4268 : 0xFFE3E9F0,
                night ? 0xFF7899C3 : 0xFFB7C4D5, 15));
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setClickable(true);
        button.setOnFocusChangeListener((v, focused) -> button.setBackground(shape(
                focused ? (night ? 0xFF4B75A5 : 0xFFCEDCEC)
                        : (night ? 0xFF2B4268 : 0xFFE3E9F0),
                focused ? FOCUS_RING : night ? 0xFF7899C3 : 0xFFB7C4D5, 15,
                focused ? 2 : 1)));
        return button;
    }

    private void showTeamSetup() {
        setupShown = true;
        CalendarFeed[] draft = {clubOneCalendar, clubTwoCalendar};
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(36), px(28), px(36), px(30));
        content.setBackground(shape(night ? 0xFF142548 : 0xFFF5F2EA,
                night ? 0xFF899BC6 : 0xFFB7C4D5, 26));
        TextView title = new TextView(this);
        title.setText(tr("Twoje kluby", "Your clubs"));
        title.setTextColor(ink);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(32));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title);
        TextView description = new TextView(this);
        description.setText(tr("Wybierz jeden lub dwa zespoły. Terminarze będą aktualizowane automatycznie.",
                "Choose one or two teams. Fixtures update automatically."));
        description.setTextColor(muted);
        description.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
        descriptionParams.setMargins(0, px(8), 0, px(24));
        content.addView(description, descriptionParams);
        TextView first = setupButton(draft[0].configured() ? "1. " + draft[0].club + " · " + draft[0].league
                : tr("1. Wybierz klub", "1. Choose a club"));
        content.addView(first, new LinearLayout.LayoutParams(-1, px(66)));
        TextView second = setupButton(draft[1].configured() ? "2. " + draft[1].club + " · " + draft[1].league
                : tr("2. Dodaj drugi klub (opcjonalnie)", "2. Add a second club (optional)"));
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(-1, px(66));
        secondParams.setMargins(0, px(14), 0, 0);
        content.addView(second, secondParams);
        TextView status = new TextView(this);
        status.setTextColor(muted);
        status.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(17));
        status.setMinHeight(px(46));
        status.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(status);
        first.setOnClickListener(v -> chooseLeague(0, draft, first, dialog, status));
        second.setOnClickListener(v -> chooseLeague(1, draft, second, dialog, status));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.RIGHT);
        TextView remove = setupButton(tr("Usuń drugi klub", "Remove second club"));
        remove.setOnClickListener(v -> {
            draft[1] = new CalendarFeed("club2", "", "", "", "");
            second.setText(tr("2. Dodaj drugi klub (opcjonalnie)", "2. Add a second club (optional)"));
        });
        actions.addView(remove, new LinearLayout.LayoutParams(px(260), px(54)));
        TextView save = setupButton(tr("Zapisz", "Save"));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(px(145), px(54));
        saveParams.leftMargin = px(14);
        actions.addView(save, saveParams);
        save.setOnClickListener(v -> {
            if (!draft[0].configured()) { status.setText(tr("Najpierw wybierz pierwszy klub.",
                    "Choose the first club before saving.")); return; }
            saveConfiguredClubs(draft);
            dialog.dismiss();
        });
        content.addView(actions);
        dialog.setContentView(content);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        showDialogWithBlur(dialog);
        dialog.getWindow().setLayout(px(960), -2);
        first.requestFocusFromTouch();
    }

    private void chooseLeague(int slot, CalendarFeed[] draft, TextView button,
                              Dialog setup, TextView status) {
        status.setText(tr("Pobieranie listy lig…", "Loading leagues…"));
        new Thread(() -> {
            try {
                List<LeagueCatalogClient.League> leagues = LeagueCatalogClient.leagues();
                handler.post(() -> {
                    if (!setup.isShowing()) return;
                    status.setText("");
                    String[] names = new String[leagues.size()];
                    for (int i = 0; i < names.length; i++) names[i] = leagues.get(i).name;
                    showChoiceDialog(tr("Wybierz ligę", "Choose a league"), names,
                            index -> chooseClub(slot, leagues.get(index), draft, button, setup, status));
                });
            } catch (Exception e) {
                handler.post(() -> { if (setup.isShowing()) status.setText(tr("Nie udało się pobrać lig. Spróbuj ponownie.",
                        "Could not load leagues. Try again.")); });
            }
        }, "league-catalog").start();
    }

    private void chooseClub(int slot, LeagueCatalogClient.League league, CalendarFeed[] draft,
                            TextView button, Dialog setup, TextView status) {
        status.setText(tr("Pobieranie drużyn…", "Loading teams…"));
        new Thread(() -> {
            try {
                List<LeagueCatalogClient.Team> teams = LeagueCatalogClient.teams(league);
                handler.post(() -> {
                    if (!setup.isShowing()) return;
                    if (teams.isEmpty()) { status.setText(tr("Brak drużyn w tej lidze.",
                            "No teams in this league.")); return; }
                    status.setText("");
                    String[] names = new String[teams.size()];
                    for (int i = 0; i < names.length; i++) names[i] = teams.get(i).name;
                    showChoiceDialog(league.name, names, index -> {
                        LeagueCatalogClient.Team team = teams.get(index);
                        int other = 1 - slot;
                        if (draft[other].configured() && draft[other].url.equals(team.calendarUrl(league))) {
                            status.setText(tr("Ten klub jest już wybrany.", "This club is already selected."));
                            return;
                        }
                        draft[slot] = new CalendarFeed("club" + (slot + 1), team.calendarUrl(league),
                                team.name, league.name, league.slug);
                        button.setText((slot + 1) + ". " + team.name + " · " + league.name);
                        button.requestFocusFromTouch();
                    });
                });
            } catch (Exception e) {
                handler.post(() -> { if (setup.isShowing()) status.setText(tr("Nie udało się pobrać drużyn. Spróbuj ponownie.",
                        "Could not load teams. Try again.")); });
            }
        }, "team-catalog").start();
    }

    private void saveConfiguredClubs(CalendarFeed[] chosen) {
        SharedPreferences saved = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor edit = saved.edit();
        for (int i = 0; i < 2; i++) {
            String key = "club" + (i + 1);
            edit.putString(key + "_url", chosen[i].url)
                    .putString(key + "_name", chosen[i].club)
                    .putString(key + "_league", chosen[i].league)
                    .putString(key + "_league_slug", chosen[i].leagueSlug);
            if (!chosen[i].url.equals(i == 0 ? clubOneCalendar.url : clubTwoCalendar.url))
                edit.remove(key + "_calendar_ics").remove(key + "_calendar_updated");
        }
        edit.apply();
        clubOneCalendar = configuredCalendar(saved, 1);
        clubTwoCalendar = configuredCalendar(saved, 2);
        restoreCalendar(clubOneCalendar, saved);
        restoreCalendar(clubTwoCalendar, saved);
        buildScreen();
    }

    private View tileGlow(int x, int y, int w, int h, int radius) {
        View glow = new View(this);
        glow.setBackground(new AppGlowDrawable(radius, true));
        glow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        glow.setAlpha(0f);
        place(glow, x - 24, y - 22, w + 48, h + 44);
        return glow;
    }

    private void showTileGlow(View glow, boolean focused) {
        glow.animate().alpha(focused ? 1f : 0f).setDuration(170).start();
    }

    private void refreshStamp(long updatedAt, int x, int y, int width) {
        String label = updatedAt > 0
                ? tr("odświeżono dane:\n", "updated:\n")
                    + new SimpleDateFormat(english ? "dd MMM yyyy HH:mm" : "dd.MM.yyyy HH:mm",
                            displayLocale()).format(new Date(updatedAt))
                : tr("oczekiwanie na dane", "waiting for data");
        TextView stamp = text(label, 13, muted, false, x, y, width, 43);
        stamp.setGravity(Gravity.RIGHT | Gravity.TOP);
        stamp.setSingleLine(false);
        stamp.setMaxLines(2);
        stamp.setEllipsize(null);
    }

    private void circleAction(int icon, String description, int x, Runnable click) {
        View glow = tileGlow(x, 45, 76, 76, 38);
        FrameLayout button = new FrameLayout(this);
        place(button, x, 45, 76, 76);
        ImageView symbol = new ImageView(this);
        symbol.setImageResource(icon);
        symbol.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.addView(symbol, new FrameLayout.LayoutParams(px(34), px(34), Gravity.CENTER));
        button.setContentDescription(description);
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(shape(TILE_BG, 0x556D7ECC, 38));
        button.setOnFocusChangeListener((v, focused) -> {
            showTileGlow(glow, focused);
        });
        button.setOnClickListener(v -> click.run());
    }

    private long matchVisibleUntil(IcsMatchParser.Match match) {
        if (match.hasTime) return match.startsAt + MATCH_VISIBLE_AFTER_KICKOFF_MS;
        Calendar nextDay = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"));
        nextDay.setTimeInMillis(match.startsAt);
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        return nextDay.getTimeInMillis();
    }

    private List<IcsMatchParser.Match> upcomingMatches(CalendarFeed feed, long now) {
        List<IcsMatchParser.Match> upcoming = new ArrayList<>();
        for (IcsMatchParser.Match candidate : feed.matches)
            if (now < matchVisibleUntil(candidate)) upcoming.add(candidate);
        return upcoming;
    }

    private boolean matchIsLive(IcsMatchParser.Match match, long now) {
        return match != null && match.hasTime && now >= match.startsAt
                && now < matchVisibleUntil(match);
    }

    private boolean matchCardNeedsUpdate(CalendarFeed feed, long now) {
        if (!feed.configured()) return false;
        List<IcsMatchParser.Match> upcoming = upcomingMatches(feed, now);
        long first = upcoming.isEmpty() ? 0 : upcoming.get(0).startsAt;
        if (first != feed.renderedFirstMatchStart) return true;
        IcsMatchParser.Match shown = upcoming.isEmpty() ? null
                : upcoming.get(Math.min(feed.selected, upcoming.size() - 1));
        return matchIsLive(shown, now) != feed.renderedLive;
    }

    private void scheduleNextMatchBoundary() {
        handler.removeCallbacks(updateMatchCards);
        if (!dashboardVisible) return;
        long now = System.currentTimeMillis();
        long next = Long.MAX_VALUE;
        for (CalendarFeed feed : new CalendarFeed[]{clubOneCalendar, clubTwoCalendar}) {
            if (!feed.configured()) continue;
            for (IcsMatchParser.Match match : feed.matches) {
                if (match.hasTime && match.startsAt > now)
                    next = Math.min(next, match.startsAt);
                long end = matchVisibleUntil(match);
                if (end > now) next = Math.min(next, end);
            }
        }
        if (next != Long.MAX_VALUE)
            handler.postDelayed(updateMatchCards, Math.max(1, next - now + 100));
    }

    private void renderMatchCard(int x, String club, String league, CalendarFeed feed) {
        int firstChild = root.getChildCount();
        renderMatchCardContent(x, club, league, feed);
        feed.cardViews.clear();
        for (int i = firstChild; i < root.getChildCount(); i++)
            feed.cardViews.add(root.getChildAt(i));
    }

    private void replaceMatchCard(int x, String club, String league, CalendarFeed feed) {
        int position = feed.cardViews.isEmpty() ? root.getChildCount()
                : root.indexOfChild(feed.cardViews.get(0));
        for (List<CrestSlot> slots : crestSlots.values())
            slots.removeIf(slot -> feed.cardViews.contains(slot.image));
        for (View view : feed.cardViews) root.removeView(view);
        renderMatchCard(x, club, league, feed);
        for (View view : feed.cardViews) {
            root.removeView(view);
            root.addView(view, Math.min(position++, root.getChildCount()));
        }
        if (feed.tileButton != null) feed.tileButton.requestFocus();
    }

    private void renderMatchCardContent(int x, String club, String league, CalendarFeed feed) {
        final int y = feed == clubOneCalendar && !clubTwoCalendar.configured()
                ? SINGLE_MATCH_Y : DASHBOARD_CARD_Y;
        final int w = 500;
        final int h = 286;
        long now = System.currentTimeMillis();
        List<IcsMatchParser.Match> upcoming = upcomingMatches(feed, now);
        long first = upcoming.isEmpty() ? 0 : upcoming.get(0).startsAt;
        if (feed.renderedFirstMatchStart != 0 && first != feed.renderedFirstMatchStart)
            feed.selected = 0;
        feed.renderedFirstMatchStart = first;
        if (feed.selected >= upcoming.size()) feed.selected = 0;
        IcsMatchParser.Match match = upcoming.isEmpty() ? null : upcoming.get(feed.selected);
        boolean live = matchIsLive(match, now);
        feed.renderedLive = live;
        boolean home = match != null && match.home.startsWith(club);
        View glow = tileGlow(x, y, w, h, 34);
        colorCard(x, y, w, h);
        int color = ink;
        if (match == null) {
            TextView placeTag = text(tr("TERMINARZ", "FIXTURES"), 18, color, true, x + 28, y + 24, 115, 32);
            placeTag.setGravity(Gravity.CENTER);
            placeTag.setBackground(shape(0x55FFFFFF, Color.TRANSPARENT, 9));
        } else {
            ImageView placeIcon = new ImageView(this);
            placeIcon.setImageResource(home ? R.drawable.ic_match_home : R.drawable.ic_match_away);
            placeIcon.setColorFilter(color);
            placeIcon.setContentDescription(home ? tr("Mecz u siebie", "Home match")
                    : tr("Mecz na wyjeździe", "Away match"));
            placeIcon.setBackground(shape(0x55FFFFFF, Color.TRANSPARENT, 9));
            placeIcon.setPadding(px(7), px(4), px(7), px(4));
            place(placeIcon, x + 28, y + 22, 42, 36);
        }
        if (match != null && match.round > 0) {
            TextView leagueLabel = text(league + tr(" - Kolejka ", " - Round ") + match.round, 18, color, false,
                    x + 84, y + 22, 240, 35);
            leagueLabel.setAutoSizeTextTypeUniformWithConfiguration(
                    px(15), px(18), 1, TypedValue.COMPLEX_UNIT_PX);
        } else {
            text(league, 20, color, false, x + (match == null ? 155 : 84), y + 22,
                    match == null ? 169 : 240, 35);
        }
        refreshStamp(feed.updatedAt, x + 330, y + 19, 144);
        String title = match == null ? tr("Brak nadchodzących meczów", "No upcoming matches")
                : shortName(match.home) + " – " + shortName(match.away);
        TextView teams = text(title, 29, color, true, x + 29, y + 68, 405, 80);
        teams.setSingleLine(false);
        teams.setMaxLines(2);
        teams.setGravity(Gravity.TOP);
        if (match == null) {
            text(feed.loading ? tr("Pobieranie terminarza…", "Loading fixtures…")
                    : tr("Terminarz chwilowo niedostępny", "Fixtures temporarily unavailable"), 20, color, false,
                    x + 29, y + 176, 445, 50);
            return;
        }
        String crestLeague = "ekstraklasa".equals(feed.leagueSlug) ? "ekstraklasa"
                : "3-liga-grupa-4".equals(feed.leagueSlug) ? "iii_liga" : "other";
        crestBadge(match.home, crestLeague, x + 14, y + 145);
        crestBadge(match.away, crestLeague, x + 150, y + 145);
        String time = match.hasTime ? new SimpleDateFormat("HH:mm", PL).format(new Date(match.startsAt)) : "—:—";
        text(time, 61, color, true, x + 290, y + 163, 190, 75).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        if (live) {
            TextView liveBadge = text(tr("Teraz!", "Live!"), 20, 0xFFFFFFFF, true,
                    x + 367, y + 229, 110, 34);
            liveBadge.setGravity(Gravity.CENTER);
            liveBadge.setBackground(shape(0xFFD8283F, 0xFFFF9AA6, 15));
        } else {
            text(matchDateLabel(match), 18, color, false,
                    x + 260, y + 231, 215, 27).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        }
        feed.tileButton = null;
        if (upcoming.size() > 1) {
            View tile = new View(this);
            place(tile, x, y, w, h);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            tile.setClickable(true);
            tile.setOnFocusChangeListener((v, focused) -> showTileGlow(glow, focused));
            tile.setContentDescription(feed.selected == 0 ? tr("Następny mecz ", "Next match ") + club
                    : tr("Wróć do najbliższego meczu ", "Return to nearest match ") + club);
            tile.setOnClickListener(v -> {
                feed.selected = feed.selected == 0 ? 1 : 0;
                replaceMatchCard(x, club, league, feed);
            });
            feed.tileButton = tile;
        }
    }

    private String shortName(String name) {
        String shortened = name.trim();
        while (shortened.length() > 24 && shortened.lastIndexOf(' ') > 0)
            shortened = shortened.substring(0, shortened.lastIndexOf(' '));
        return shortened;
    }

    private String matchDateLabel(IcsMatchParser.Match match) {
        String day = new SimpleDateFormat("EEE d.MM", displayLocale()).format(new Date(match.startsAt));
        day = Character.toUpperCase(day.charAt(0)) + day.substring(1);
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance(); then.setTimeInMillis(match.startsAt);
        now.set(Calendar.HOUR_OF_DAY, 12); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0);
        then.set(Calendar.HOUR_OF_DAY, 12); then.set(Calendar.MINUTE, 0); then.set(Calendar.SECOND, 0);
        int days = (int) Math.round((then.getTimeInMillis() - now.getTimeInMillis()) / 86_400_000.0);
        return day + (days <= 0 ? tr(" · dziś", " · today") : days == 1
                ? tr(" · jutro", " · tomorrow") : tr(" · za ", " · in ") + days
                + tr(" dni", " days"));
    }

    private void crestBadge(String club, String league, int x, int y) {
        final int size = 128;
        Bitmap bitmap = crests.get(club, league);
        TextView fallback = text(club.substring(0, Math.min(2, club.length())).toUpperCase(PL), 26,
                ink, true, x, y, size, size);
        fallback.setGravity(Gravity.CENTER);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (bitmap != null) setCrestBitmap(image, bitmap);
        image.setVisibility(bitmap == null ? View.INVISIBLE : View.VISIBLE);
        fallback.setVisibility(bitmap == null ? View.VISIBLE : View.INVISIBLE);
        place(image, x, y, size, size);
        crestSlots.computeIfAbsent(league + "|" + club, k -> new ArrayList<>())
                .add(new CrestSlot(image, fallback, club, league));
    }

    private void updateCrestViews(String club) {
        for (List<CrestSlot> slots : crestSlots.values()) for (CrestSlot slot : slots) {
            if (!club.equals(slot.name)) continue;
            Bitmap bitmap = crests.get(slot.name, slot.league);
            if (bitmap == null) continue;
            setCrestBitmap(slot.image, bitmap);
            slot.image.setVisibility(View.VISIBLE);
            slot.fallback.setVisibility(View.INVISIBLE);
        }
    }

    private void setCrestBitmap(ImageView image, Bitmap bitmap) {
        Bitmap result = scaledCrests.get(bitmap);
        if (result == null || result.isRecycled()) {
            int target = px(128);
            float ratio = Math.min(target / (float) bitmap.getWidth(), target / (float) bitmap.getHeight());
            int finalWidth = Math.max(1, Math.round(bitmap.getWidth() * ratio));
            int finalHeight = Math.max(1, Math.round(bitmap.getHeight() * ratio));
            Bitmap current = bitmap;
            while (current.getWidth() > finalWidth * 4 && current.getHeight() > finalHeight * 4) {
                Bitmap next = Bitmap.createScaledBitmap(current,
                        Math.max(finalWidth, current.getWidth() / 2),
                        Math.max(finalHeight, current.getHeight() / 2), true);
                if (current != bitmap && next != current) current.recycle();
                current = next;
            }
            result = Bitmap.createScaledBitmap(current, finalWidth, finalHeight, true);
            if (current != bitmap && current != result) current.recycle();
            result.setDensity(getResources().getDisplayMetrics().densityDpi);
            scaledCrests.put(bitmap, result);
        }
        image.setScaleType(ImageView.ScaleType.CENTER);
        image.setImageBitmap(result);
    }

    private void placeForecastIcon(String code, int x, int y, int width, int height) {
        place(new WeatherIcon(code), x, y, width, height);
    }

    private void renderWeatherCard(int x) {
        final int y = clubTwoCalendar.configured() ? DASHBOARD_CARD_Y : SINGLE_WEATHER_Y;
        final int w = 500, h = 286;
        View glow = tileGlow(x, y, w, h, 34);
        colorCard(x, y, w, h);
        int weatherInk = ink;
        String currentIcon = forecast == null ? "" : forecast.optString("current_icon", "");
        ImageView weatherHeaderIcon = new ImageView(this);
        weatherHeaderIcon.setImageResource(R.drawable.ic_weather_thermometer);
        weatherHeaderIcon.setColorFilter(weatherInk);
        weatherHeaderIcon.setBackground(shape(0x55FFFFFF, Color.TRANSPARENT, 9));
        weatherHeaderIcon.setPadding(px(7), px(4), px(7), px(4));
        place(weatherHeaderIcon, x + 28, y + 22, 42, 36);
        text(tr("Pogoda", "Weather"), 20, weatherInk, false, x + 84, y + 22, 155, 35);
        refreshStamp(forecastUpdatedAt, x + 242, y + 19, 233);
        placeForecastIcon(currentIcon, x + 43, y + 67, 84, 72);
        JSONObject now = currentWeather();
        TextView current = text("", 81, weatherInk, true, x + 132, y + 59, 170, 88);
        current.setText(now == null || !now.has("temperature") ? "—"
                : temperatureLabel(now.optDouble("temperature", 0)));
        current.setGravity(Gravity.CENTER);
        TextView city = text(cityName, cityName.length() > 9 ? 22 : 32, weatherInk, true, x + 310, y + 85, 170, 52);
        city.setGravity(Gravity.CENTER);
        city.setSingleLine(true);
        city.setEllipsize(android.text.TextUtils.TruncateAt.END);
        List<JSONObject> hours = hourlySamples(6);
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", displayLocale());
        hourFormat.setTimeZone(forecastZone());
        for (int i = 0; i < hours.size(); i++) {
            JSONObject item = hours.get(i);
            int hourX = x + 16 + i * 79;
            text(hourFormat.format(new Date(item.optLong("time"))), 15, weatherInk, true,
                    hourX, y + 155, 75, 23).setGravity(Gravity.CENTER);
            placeForecastIcon(item.optString("icon", ""), hourX + 9, y + 178, 58, 49);
            TextView temperature = text("", 18, weatherInk, true, hourX, y + 226, 75, 27);
            temperature.setText(temperatureLabel(item.optDouble("temperature")));
            temperature.setGravity(Gravity.CENTER);
        }
        if (hours.isEmpty()) text(tr("Pobieranie prognozy godzinowej…", "Loading hourly forecast…"), 18, weatherInk, false,
                x + 24, y + 189, 455, 34).setGravity(Gravity.CENTER);
        View openDetails = new View(this);
        openDetails.setFocusable(true);
        openDetails.setClickable(true);
        openDetails.setContentDescription(tr("Pogoda ", "Weather ") + cityName
                + tr(". Otwórz szczegóły", ". Open details"));
        openDetails.setOnFocusChangeListener((view, focused) -> showTileGlow(glow, focused));
        openDetails.setOnClickListener(view -> showWeatherDetails());
        place(openDetails, x, y, w, h);
    }

    private List<JSONObject> hourlySamples(int limit) {
        List<JSONObject> samples = new ArrayList<>();
        JSONArray hours = forecast == null ? null : forecast.optJSONArray("hourly");
        if (hours == null) return samples;
        long last = 0;
        for (int i = 0; i < hours.length() && samples.size() < limit; i++) {
            JSONObject item = hours.optJSONObject(i);
            if (item == null) continue;
            long time = item.optLong("time");
            if (time <= 0 || !samples.isEmpty() && time - last < 90 * 60_000L) continue;
            samples.add(item);
            last = time;
        }
        return samples;
    }

    private void showWeatherDetails() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        detailText = night ? Color.WHITE : 0xFF15223E;
        detailMuted = night ? 0xFFC5D3E9 : 0xFF4A5C75;
        detailCard = night ? 0xFF2B4268 : 0xFFE3E9F0;
        detailAstronomy = night ? 0xFF101E38 : 0xFFE5EBF1;
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(36), px(24), px(36), px(28));
        content.setBackground(shape(night ? 0xFF142548 : 0xFFF5F2EA,
                night ? 0xFF899BC6 : 0xFFB7C4D5, 26, 1));
        detailLine(content, cityName + tr("  ·  szczegóły pogody", "  ·  weather details"), 31, true, detailText);
        detailLine(content, tr("Aktualne warunki  ·  ", "Current conditions  ·  ")
                + currentWeatherTime(), 18, false, detailMuted);
        detailLine(content, tr("Prognoza na 7 dni · Open-Meteo", "7-day forecast · Open-Meteo"), 24, true, detailText);
        addDailyForecast(content);
        detailLine(content, tr("Warunki teraz", "Conditions now"), 24, true, detailText);
        addWeatherMetrics(content);
        detailLine(content, tr("Słońce i Księżyc", "Sun and Moon"), 24, true, detailText);
        addAstronomyCards(content);
        detailLine(content, tr("Dane pogodowe: Open-Meteo.com (CC BY 4.0).",
                "Weather data: Open-Meteo.com (CC BY 4.0)."), 14, false, detailMuted);
        TextView close = new TextView(this);
        close.setText(tr("Zamknij", "Close"));
        close.setTextColor(detailText);
        close.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        close.setGravity(Gravity.CENTER);
        close.setFocusable(true);
        close.setClickable(true);
        close.setBackground(shape(night ? 0xFF2D466F : 0xFFE3E9F0,
                night ? 0xFF7899C3 : 0xFFB7C4D5, 13));
        close.setOnFocusChangeListener((view, focused) -> close.setBackground(shape(
                focused ? (night ? 0xFF4B75A5 : 0xFFCEDCEC)
                        : (night ? 0xFF2D466F : 0xFFE3E9F0),
                focused ? FOCUS_RING : night ? 0xFF7899C3 : 0xFFB7C4D5, 13,
                focused ? 2 : 1)));
        close.setOnClickListener(view -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(px(160), px(48));
        closeParams.gravity = Gravity.END;
        closeParams.topMargin = px(8);
        content.addView(close, closeParams);
        dialog.setContentView(content);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        showDialogWithBlur(dialog);
        dialog.getWindow().setLayout(px(1080), -2);
        close.requestFocus();
    }

    private void showDialogWithBlur(Dialog dialog) {
        showDialogWithBlur(dialog, null);
    }

    private void showDialogWithBlur(Dialog dialog, Runnable afterDismiss) {
        popupDepth++;
        popupOpen = true;
        handler.removeCallbacks(startScreensaver);
        final FrameLayout dashboard = root;
        if (dashboard == null || dashboard.getWidth() == 0 || dashboard.getHeight() == 0) {
            dialog.setOnDismissListener(d -> {
                popupOpen = --popupDepth > 0;
                if (afterDismiss != null) afterDismiss.run();
                resetScreensaverTimer();
            });
            dialog.show();
            return;
        }
        int width = Math.max(1, dashboard.getWidth() / 4);
        int height = Math.max(1, dashboard.getHeight() / 4);
        Bitmap backdrop = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas capture = new Canvas(backdrop);
        capture.scale(width / (float) dashboard.getWidth(), height / (float) dashboard.getHeight());
        dashboard.draw(capture);
        blurBackdrop(backdrop);
        ImageView blur = new ImageView(this);
        blur.setImageBitmap(backdrop);
        blur.setScaleType(ImageView.ScaleType.FIT_XY);
        blur.setFocusable(false);
        dashboard.addView(blur, new FrameLayout.LayoutParams(-1, -1));
        dialog.setOnDismissListener(d -> {
            ViewGroup parent = (ViewGroup) blur.getParent();
            if (parent != null) parent.removeView(blur);
            blur.setImageDrawable(null);
            backdrop.recycle();
            popupOpen = --popupDepth > 0;
            if (afterDismiss != null) afterDismiss.run();
            resetScreensaverTimer();
        });
        dialog.show();
    }

    private void blurBackdrop(Bitmap bitmap) {
        blurBackdrop(bitmap, 4, 2);
    }

    private void blurBackdrop(Bitmap bitmap, int radius, int passes) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        int[] temp = new int[pixels.length];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int pass = 0; pass < passes; pass++) {
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int red = 0, green = 0, blue = 0, count = 0;
                for (int dx = -radius; dx <= radius; dx++) {
                    int color = pixels[y * width + Math.max(0, Math.min(width - 1, x + dx))];
                    red += Color.red(color); green += Color.green(color); blue += Color.blue(color); count++;
                }
                temp[y * width + x] = Color.rgb(red / count, green / count, blue / count);
            }
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int red = 0, green = 0, blue = 0, count = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    int color = temp[Math.max(0, Math.min(height - 1, y + dy)) * width + x];
                    red += Color.red(color); green += Color.green(color); blue += Color.blue(color); count++;
                }
                pixels[y * width + x] = Color.rgb(red / count, green / count, blue / count);
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    private void addDailyForecast(LinearLayout content) {
        JSONObject daily = forecast == null ? null : forecast.optJSONObject("daily");
        if (daily == null) {
            detailLine(content, tr("Prognoza chwilowo niedostępna", "Forecast temporarily unavailable"),
                    18, false, detailMuted);
            return;
        }
        JSONArray dates = daily.optJSONArray("time");
        JSONArray highs = daily.optJSONArray("temperature_2m_max");
        JSONArray lows = daily.optJSONArray("temperature_2m_min");
        JSONArray icons = daily.optJSONArray("icon");
        if (dates == null || highs == null || lows == null || icons == null) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, px(125));
        rowParams.bottomMargin = px(16);
        content.addView(row, rowParams);
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", PL);
        SimpleDateFormat labelFormat = new SimpleDateFormat("EEE d", displayLocale());
        for (int i = 0; i < Math.min(7, dates.length()); i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setBackground(shape(detailCard, Color.TRANSPARENT, 15));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, -1, 1);
            cardParams.setMargins(px(4), 0, px(4), 0);
            row.addView(card, cardParams);
            TextView day = new TextView(this);
            try {
                String label = labelFormat.format(iso.parse(dates.getString(i))).replace(".", "");
                day.setText(Character.toUpperCase(label.charAt(0)) + label.substring(1));
            } catch (Exception ignored) { day.setText("—"); }
            day.setTextColor(detailMuted);
            day.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(16));
            day.setGravity(Gravity.CENTER);
            card.addView(day, new LinearLayout.LayoutParams(-1, px(30)));
            LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(px(51), px(46));
            symbolParams.gravity = Gravity.CENTER_HORIZONTAL;
            card.addView(detailWeatherIcon(icons.optString(i, "")), symbolParams);
            TextView temperature = new TextView(this);
            temperature.setText(temperatureLabelParts(shownTemperature(highs.optDouble(i)) + "° / "
                    + shownTemperature(lows.optDouble(i)) + "°"));
            temperature.setTextColor(detailText);
            temperature.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(18));
            temperature.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            temperature.setGravity(Gravity.CENTER);
            card.addView(temperature, new LinearLayout.LayoutParams(-1, px(34)));
        }
    }

    private View detailWeatherIcon(String code) {
        return new WeatherIcon(code);
    }

    private void addWeatherMetrics(LinearLayout content) {
        String[] labels = {tr("Wiatr", "Wind"), tr("Wilgotność", "Humidity"),
                tr("Ciśnienie", "Pressure"), tr("Opad", "Precipitation")};
        String[] values = {observed("wind", " m/s"),
                observed("humidity", "%"), observed("pressure", " hPa"),
                observed("precipitation", " mm")};
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, px(80));
        rowParams.bottomMargin = px(16);
        content.addView(row, rowParams);
        for (int i = 0; i < labels.length; i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(px(14), 0, px(8), 0);
            card.setBackground(shape(detailCard, Color.TRANSPARENT, 14));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, -1, 1);
            cardParams.setMargins(px(4), 0, px(4), 0);
            row.addView(card, cardParams);
            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setTextColor(detailMuted);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(15));
            card.addView(label);
            TextView value = new TextView(this);
            value.setText(values[i]);
            value.setTextColor(detailText);
            value.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(21));
            value.setSingleLine(true);
            card.addView(value);
        }
    }

    private void addAstronomyCards(LinearLayout content) {
        JSONObject sun = forecast == null ? null : forecast.optJSONObject("sun");
        TimeZone weatherZone = TimeZone.getTimeZone(
                forecast == null ? "Europe/Warsaw" : forecast.optString("timezone", "Europe/Warsaw"));
        Date sunrise = sun == null ? null : PublicForecastClient.parseTime(sun.optString("Sunrise"), weatherZone);
        Date sunset = sun == null ? null : PublicForecastClient.parseTime(sun.optString("Sunset"), weatherZone);
        SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", PL);
        clockFormat.setTimeZone(weatherZone);
        String rise = sunrise == null ? "—" : clockFormat.format(sunrise);
        String set = sunset == null ? "—" : clockFormat.format(sunset);
        int phase = moonPhaseIndex();
        String[] phases = {tr("Nów", "New moon"), tr("Sierp przybywający", "Waxing crescent"),
                tr("Pierwsza kwadra", "First quarter"), tr("Księżyc garbaty przybywający", "Waxing gibbous"),
                tr("Pełnia", "Full moon"), tr("Księżyc garbaty ubywający", "Waning gibbous"),
                tr("Ostatnia kwadra", "Last quarter"), tr("Sierp ubywający", "Waning crescent")};

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, px(169));
        rowParams.bottomMargin = px(12);
        content.addView(row, rowParams);

        LinearLayout sunCard = astronomyCard(row, tr("Pozycja Słońca", "Sun position"));
        SunTrack track = new SunTrack(sunrise, sunset);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(px(190), px(78));
        trackParams.gravity = Gravity.CENTER_HORIZONTAL;
        sunCard.addView(track, trackParams);
        astronomyLabel(sunCard, tr("Wschód  ", "Sunrise  ") + rise
                + tr("      Zachód  ", "      Sunset  ") + set);

        LinearLayout moonCard = astronomyCard(row, tr("Faza Księżyca", "Moon phase"));
        MoonPhaseGraphic moon = new MoonPhaseGraphic(phase);
        LinearLayout.LayoutParams moonParams = new LinearLayout.LayoutParams(px(72), px(72));
        moonParams.gravity = Gravity.CENTER_HORIZONTAL;
        moonCard.addView(moon, moonParams);
        astronomyLabel(moonCard, phases[phase]);
    }

    private LinearLayout astronomyCard(LinearLayout row, String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(px(10), px(10), px(10), px(8));
        card.setBackground(shape(detailAstronomy, night ? 0xFF48638A : 0xFFC9D4E0, 17));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(px(3), 0, px(3), 0);
        row.addView(card, params);
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(detailMuted);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(18));
        heading.setGravity(Gravity.CENTER);
        card.addView(heading, new LinearLayout.LayoutParams(-1, px(32)));
        return card;
    }

    private void astronomyLabel(LinearLayout card, String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(detailText);
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(17));
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        card.addView(label, new LinearLayout.LayoutParams(-1, px(38)));
    }

    // Same eight-phase calendar approximation used by the IMGW website.
    private int moonPhaseIndex() {
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"));
        int year = date.get(Calendar.YEAR), month = date.get(Calendar.MONTH) + 1;
        if (month < 3) { year--; month += 12; }
        double age = (365.25 * year + 30.6 * (month + 1) + date.get(Calendar.DAY_OF_MONTH) - 694039.09) / 29.5305882;
        return Math.round((float) (8 * (age - (int) age))) % 8;
    }

    private final class SunTrack extends View {
        private final Date rise, set;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        SunTrack(Date rise, Date set) { super(DashboardActivity.this); this.rise = rise; this.set = set; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save(); canvas.scale(getWidth() / 190f, getHeight() / 78f);
            RectF arc = new RectF(40, 13, 150, 123);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(9);
            paint.setColor(night ? 0xFF374963 : 0xFFA7B4C6);
            canvas.drawArc(arc, 180, 180, false, paint);
            if (rise != null && set != null && set.after(rise)) {
                double progress = Math.max(0, Math.min(1,
                        (System.currentTimeMillis() - rise.getTime()) / (double) (set.getTime() - rise.getTime())));
                float angle = (float) Math.toRadians(180 + progress * 180);
                paint.setStyle(Paint.Style.FILL); paint.setColor(0xFFFFB42D);
                canvas.drawCircle(95 + 55 * (float) Math.cos(angle), 68 + 55 * (float) Math.sin(angle), 9, paint);
            }
            canvas.restore();
        }
    }

    private final class MoonPhaseGraphic extends View {
        private final int phase;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        MoonPhaseGraphic(int phase) { super(DashboardActivity.this); this.phase = phase; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f, cy = getHeight() / 2f, radius = Math.min(cx, cy) - 2;
            paint.setColor(0xFF293344);
            canvas.drawCircle(cx, cy, radius, paint);
            Path disk = new Path(); disk.addCircle(cx, cy, radius, Path.Direction.CW);
            Path lit = new Path(disk);
            Path shifted = new Path();
            if (phase == 0) lit.reset();
            else if (phase == 1 || phase == 7) {
                shifted.addCircle(cx + (phase == 1 ? -radius * 0.72f : radius * 0.72f), cy, radius, Path.Direction.CW);
                lit.op(shifted, Path.Op.DIFFERENCE);
            } else if (phase == 2 || phase == 6) {
                Path half = new Path();
                half.addRect(phase == 2 ? cx : cx - radius, cy - radius,
                        phase == 2 ? cx + radius : cx, cy + radius, Path.Direction.CW);
                lit.op(half, Path.Op.INTERSECT);
            } else if (phase == 3 || phase == 5) {
                shifted.addCircle(cx + (phase == 3 ? radius * 0.22f : -radius * 0.22f), cy, radius, Path.Direction.CW);
                lit.op(shifted, Path.Op.INTERSECT);
            }
            canvas.save(); canvas.clipPath(lit);
            paint.setColor(0xFFE5E5DC);
            canvas.drawCircle(cx, cy, radius, paint);
            canvas.restore();
        }
    }

    private void detailLine(LinearLayout parent, String value, int size, boolean bold, int color) {
        TextView line = new TextView(this);
        line.setText(value);
        line.setTextColor(color);
        line.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        if (bold) line.setTypeface(null, Typeface.BOLD);
        line.setPadding(0, 0, 0, px(14));
        parent.addView(line);
    }

    private void buildAppDock() {
        for (int i = 0; i < DOCK_BRANDS.length; i++)
            dockTile(DOCK_BRANDS[i], DOCK_PACKAGES[i], 56 + i * 406);

        drawerBackdrop = new FrameLayout(this);
        drawerBackdrop.setClickable(true);
        drawerBackdrop.setOnClickListener(v -> closeAppDrawer());
        drawerBlurImage = new ImageView(this);
        drawerBlurImage.setScaleType(ImageView.ScaleType.FIT_XY);
        drawerBackdrop.addView(drawerBlurImage, new FrameLayout.LayoutParams(-1, -1));
        View drawerTint = new View(this);
        drawerTint.setBackgroundColor(night ? 0x800B1B37 : 0x8CECF1F8);
        drawerBackdrop.addView(drawerTint, new FrameLayout.LayoutParams(-1, -1));
        root.addView(drawerBackdrop, new FrameLayout.LayoutParams(-1, -1));
        drawerBackdrop.setVisibility(View.GONE);

        drawerTitle = text(tr("Aplikacje", "Apps"), 31, night ? Color.WHITE : 0xFF15223E,
                true, 56, 378, 500, 52);
        drawerTitle.setVisibility(View.GONE);

        appDrawer = new ScrollView(this);
        appDrawer.setVerticalScrollBarEnabled(false);
        appDrawer.setFillViewport(false);
        appDrawer.setFocusable(false);
        appDrawer.setClipChildren(false);
        appDrawer.setClipToPadding(false);
        place(appDrawer, 36, 450, 1600, APP_ROW_HEIGHT * 3);
        appDrawer.setVisibility(View.GONE);
        drawerRows = new LinearLayout(this);
        drawerRows.setOrientation(LinearLayout.VERTICAL);
        drawerRows.setClipChildren(false);
        appDrawer.addView(drawerRows, new ScrollView.LayoutParams(-1, -2));
        extraAppRows.clear();
        drawerRowsLoaded = false;

        View expandGlow = tileGlow(817, 887, 38, 38, 19);
        dockExpand = new ImageView(this);
        dockExpand.setImageResource(R.drawable.ic_dock_expand);
        dockExpand.setContentDescription(tr("Więcej aplikacji — strzałka w dół", "More apps — press down"));
        dockExpand.setPadding(px(8), px(7), px(8), px(7));
        dockExpand.setBackground(shape(TILE_BG, 0x66869BBE, 19));
        dockExpand.setFocusable(true);
        dockExpand.setClickable(true);
        dockExpand.setOnFocusChangeListener((view, focused) -> showTileGlow(expandGlow, focused));
        dockExpand.setOnClickListener(v -> openAppDrawer());
        place(dockExpand, 817, 887, 38, 38);
    }

    private void dockTile(String brand, String pkg, int x) {
        View glow = new View(this);
        glow.setBackground(new AppGlowDrawable());
        glow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        glow.setAlpha(0f);
        place(glow, x - 24, 743, 388, 164);
        dockGlows.add(glow);
        FrameLayout card = new FrameLayout(this);
        place(card, x, 765, 340, 120);
        dockCards.add(card);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);
        card.setContentDescription(brand);
        card.setBackground(shape(TILE_BG, Color.TRANSPARENT, 32));
        card.setClipToOutline(true);
        Drawable artwork = null;
        if (pkg != null) {
            try {
                artwork = getPackageManager().getApplicationBanner(pkg);
                if (artwork == null) artwork = getPackageManager().getApplicationIcon(pkg);
            } catch (PackageManager.NameNotFoundException ignored) { }
        }
        if (artwork != null) {
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(artwork);
            icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            icon.setAlpha(0.93f);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(-1, -1);
            card.addView(icon, iconParams);
        } else {
            String fallback = pkg == null ? "•••" : brand;
            TextView label = new TextView(this);
            label.setText(fallback);
            label.setTextColor(Color.WHITE);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(pkg == null ? 42 : 30));
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            label.setGravity(Gravity.CENTER);
            card.addView(label, new FrameLayout.LayoutParams(-1, -1));
        }
        card.setOnFocusChangeListener((view, focused) -> {
            if (focused) lastDockFocus = card;
            setAppTileFocus(card, glow, focused);
        });
        card.setOnClickListener(view -> launch(pkg));
    }

    private void addMainDrawerRow(LinearLayout rows, String[] brands, String[] packages) {
        LinearLayout row = new LinearLayout(this);
        row.setClipChildren(false);
        rows.addView(row, new LinearLayout.LayoutParams(-1, px(APP_ROW_HEIGHT)));
        List<View> base = new ArrayList<>();
        extraAppRows.add(base);
        for (int i = 0; i < brands.length; i++) {
            String brand = brands[i], pkg = packages[i];
            int column = i;
            FrameLayout wrapper = new FrameLayout(this);
            wrapper.setClipChildren(false);
            View glow = new View(this);
            glow.setBackground(new AppGlowDrawable());
            glow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            glow.setAlpha(0f);
            FrameLayout.LayoutParams glowParams = new FrameLayout.LayoutParams(px(388), px(164));
            glowParams.leftMargin = -px(4);
            glowParams.topMargin = -px(11);
            wrapper.addView(glow, glowParams);
            FrameLayout tile = new FrameLayout(this);
            tile.setBackground(shape(night ? 0xFF263451 : 0xFFE3EAF2,
                    Color.TRANSPARENT, 32));
            tile.setClipToOutline(true);
            tile.setContentDescription(brand);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            tile.setClickable(true);
            tile.setOnFocusChangeListener((v, focused) -> {
                setAppTileFocus(tile, glow, focused);
                if (focused) { drawerFocusRow = 0; drawerFocusColumn = column; }
            });
            Drawable artwork = null;
            try {
                artwork = getPackageManager().getApplicationBanner(pkg);
                if (artwork == null) artwork = getPackageManager().getApplicationIcon(pkg);
            } catch (PackageManager.NameNotFoundException ignored) { }
            if (artwork != null) {
                ImageView image = new ImageView(this);
                image.setImageDrawable(artwork);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setAlpha(0.93f);
                tile.addView(image, new FrameLayout.LayoutParams(-1, -1));
            } else {
                TextView label = new TextView(this);
                label.setText(brand);
                label.setTextColor(Color.WHITE);
                label.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(30));
                label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                label.setGravity(Gravity.CENTER);
                tile.addView(label, new FrameLayout.LayoutParams(-1, -1));
            }
            tile.setOnClickListener(v -> launch(pkg));
            FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(px(340), px(120));
            cardParams.leftMargin = px(20);
            cardParams.topMargin = px(11);
            wrapper.addView(tile, cardParams);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(px(380), px(APP_ROW_HEIGHT));
            if (i < brands.length - 1) itemParams.rightMargin = px(26);
            row.addView(wrapper, itemParams);
            base.add(tile);
        }
    }

    private void populateExtraApps(LinearLayout rows) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> apps = new ArrayList<>(getPackageManager().queryIntentActivities(query, 0));
        HashSet<String> excluded = new HashSet<>();
        excluded.add(getPackageName());
        excluded.add("com.google.android.youtube.tv");
        excluded.add("com.netflix.ninja");
        excluded.add("com.canal.android.canal");
        excluded.add("com.spotify.tv.android");
        excluded.add("org.droidtv.playtv");
        excluded.add("org.droidtv.eum");
        excluded.add("org.droidtv.channels");
        excluded.add("org.droidtv.contentexplorer");
        List<ResolveInfo> extras = new ArrayList<>();
        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            if (excluded.add(pkg)) extras.add(info);
        }
        String[] priorities = {"tvp vod", "youtube kids", "cda", "google play"};
        List<ResolveInfo> ordered = new ArrayList<>();
        for (String priority : priorities) {
            for (ResolveInfo info : extras) {
                String label = info.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT);
                String pkg = info.activityInfo.packageName.toLowerCase(Locale.ROOT);
                boolean match = priority.equals("tvp vod") ? (label.contains("tvp") && label.contains("vod")) || pkg.contains("tvp.vod")
                        : priority.equals("youtube kids") ? (label.contains("youtube") && label.contains("kids")) || pkg.contains("youtubekids")
                        : priority.equals("cda") ? label.contains("cda") || pkg.contains("cda")
                        : label.contains("sklep play") || label.contains("play store") || pkg.equals("com.android.vending");
                if (match && !ordered.contains(info)) { ordered.add(info); break; }
            }
        }
        for (ResolveInfo info : extras) if (!ordered.contains(info)) ordered.add(info);
        firstExtraApp = null;
        LinearLayout row = null;
        for (int i = 0; i < ordered.size(); i++) {
            ResolveInfo info = ordered.get(i);
            String pkg = info.activityInfo.packageName;
            int drawerRow = 1 + i / 4, drawerColumn = i % 4;
            if (i % 4 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, px(APP_ROW_HEIGHT));
                rows.addView(row, rowParams);
                extraAppRows.add(new ArrayList<>());
            }
            FrameLayout wrapper = new FrameLayout(this);
            wrapper.setClipChildren(false);
            View glow = new View(this);
            glow.setBackground(new AppGlowDrawable());
            glow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            glow.setAlpha(0f);
            FrameLayout.LayoutParams glowParams = new FrameLayout.LayoutParams(px(388), px(164));
            glowParams.leftMargin = -px(4);
            glowParams.topMargin = -px(11);
            wrapper.addView(glow, glowParams);
            FrameLayout tile = new FrameLayout(this);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            tile.setClickable(true);
            tile.setClipToOutline(true);
            tile.setContentDescription(info.loadLabel(getPackageManager()));
            tile.setBackground(shape(night ? 0xFF263451 : 0xFFE3EAF2,
                    Color.TRANSPARENT, 32));
            tile.setOnFocusChangeListener((v, focused) -> {
                setAppTileFocus(tile, glow, focused);
                if (focused) { drawerFocusRow = drawerRow; drawerFocusColumn = drawerColumn; }
            });
            ImageView icon = new ImageView(this);
            Drawable banner = null;
            try { banner = getPackageManager().getApplicationBanner(pkg); }
            catch (PackageManager.NameNotFoundException ignored) { }
            icon.setImageDrawable(banner != null ? banner : info.loadIcon(getPackageManager()));
            icon.setScaleType(banner != null ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(-1, -1);
            if (banner == null) icon.setPadding(px(20), px(10), px(20), px(10));
            icon.setAlpha(0.93f);
            tile.addView(icon, iconParams);
            tile.setOnClickListener(v -> {
                closeAppDrawer();
                Intent start = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                start.setClassName(pkg, info.activityInfo.name);
                startActivity(start);
            });
            FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(px(340), px(120));
            cardParams.leftMargin = px(20);
            cardParams.topMargin = px(11);
            wrapper.addView(tile, cardParams);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px(380), px(APP_ROW_HEIGHT));
            if (i % 4 != 3) params.rightMargin = px(26);
            row.addView(wrapper, params);
            extraAppRows.get(1 + i / 4).add(tile);
            if (firstExtraApp == null) firstExtraApp = tile;
        }
        if (firstExtraApp == null) {
            TextView empty = new TextView(this);
            empty.setText(tr("Nie znaleziono dodatkowych aplikacji", "No additional apps found"));
            empty.setTextColor(Color.WHITE);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
            empty.setGravity(Gravity.CENTER_VERTICAL);
            rows.addView(empty, new LinearLayout.LayoutParams(px(500), px(100)));
        }
    }

    private void openAppDrawer() {
        openAppDrawer(1, 0);
    }

    private void openAppDrawer(int focusRow, int focusColumn) {
        if (appDrawerOpen) return;
        if (!drawerRowsLoaded) {
            addMainDrawerRow(drawerRows, DOCK_BRANDS, DOCK_PACKAGES);
            populateExtraApps(drawerRows);
            drawerRowsLoaded = true;
        }
        appDrawerOpen = true;
        int width = Math.max(1, root.getWidth() / 4);
        int height = Math.max(1, root.getHeight() / 4);
        drawerBlurBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas capture = new Canvas(drawerBlurBitmap);
        capture.scale(width / (float) root.getWidth(), height / (float) root.getHeight());
        for (View card : dockCards) card.setVisibility(View.INVISIBLE);
        for (View glow : dockGlows) glow.setVisibility(View.INVISIBLE);
        dockExpand.setVisibility(View.INVISIBLE);
        root.draw(capture);
        for (View card : dockCards) card.setVisibility(View.VISIBLE);
        for (View glow : dockGlows) glow.setVisibility(View.VISIBLE);
        dockExpand.setVisibility(View.VISIBLE);
        blurBackdrop(drawerBlurBitmap, 2, 1);
        drawerBlurImage.setImageBitmap(drawerBlurBitmap);
        drawerBackdrop.setAlpha(0f);
        drawerBackdrop.setVisibility(View.VISIBLE);
        drawerBackdrop.animate().alpha(1f).setDuration(220).start();
        drawerTitle.setAlpha(0f);
        drawerTitle.setVisibility(View.VISIBLE);
        drawerTitle.animate().alpha(1f).setDuration(260).start();
        int targetRow = Math.min(focusRow, Math.max(0, extraAppRows.size() - 1));
        List<View> row = extraAppRows.isEmpty() ? null : extraAppRows.get(targetRow);
        View target = row == null || row.isEmpty() ? null
                : row.get(Math.min(focusColumn, row.size() - 1));
        drawerFocusRow = targetRow;
        drawerFocusColumn = target == null ? 0 : Math.min(focusColumn, row.size() - 1);
        appDrawer.scrollTo(0, px(Math.max(0, targetRow - 1) * APP_ROW_HEIGHT));
        appDrawer.setTranslationY(px(110));
        appDrawer.setAlpha(0f);
        appDrawer.setVisibility(View.VISIBLE);
        appDrawer.animate().translationY(0).alpha(1f).setDuration(320).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        dockExpand.setVisibility(View.GONE);
        suspendBackgroundFocus(root);
        if (target != null) target.requestFocus();
        if (target != null) handler.postDelayed(() -> {
            if (appDrawerOpen) target.requestFocus();
        }, 340);
    }

    private void suspendBackgroundFocus(View view) {
        if (view == appDrawer) return;
        if (view != root && view.isFocusable()) {
            suspendedFocusables.add(view);
            view.setFocusable(false);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
                suspendBackgroundFocus(group.getChildAt(i));
        }
    }

    private void closeAppDrawer() {
        if (!appDrawerOpen) return;
        appDrawerOpen = false;
        for (View view : suspendedFocusables) view.setFocusable(true);
        suspendedFocusables.clear();
        drawerBackdrop.animate().alpha(0).setDuration(220).withEndAction(() -> {
            if (!appDrawerOpen) {
                drawerBackdrop.setVisibility(View.GONE);
                drawerBlurImage.setImageDrawable(null);
                if (drawerBlurBitmap != null) {
                    drawerBlurBitmap.recycle();
                    drawerBlurBitmap = null;
                }
            }
        }).start();
        drawerTitle.animate().alpha(0).setDuration(220).withEndAction(() -> {
            if (!appDrawerOpen) drawerTitle.setVisibility(View.GONE);
        }).start();
        appDrawer.animate().translationY(px(110)).alpha(0).setDuration(260).withEndAction(() -> {
            if (!appDrawerOpen) appDrawer.setVisibility(View.GONE);
        }).start();
        dockExpand.setVisibility(View.VISIBLE);
        View restore = lastDockFocus != null ? lastDockFocus : (dockCards.isEmpty() ? null : dockCards.get(0));
        if (restore != null) handler.postDelayed(restore::requestFocus, 40);
    }

    @Override public void onBackPressed() {
        if (appDrawerOpen) closeAppDrawer();
        // The dashboard is Home: Back has nowhere to navigate when no overlay is open.
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && suppressSaverKeyUp) {
            suppressSaverKeyUp = false;
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (screensaverActive) {
                hideScreensaver();
                suppressSaverKeyUp = true;
                resetScreensaverTimer();
                return true;
            }
            resetScreensaverTimer();
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int key = event.getKeyCode();
            if (appDrawerOpen && key == KeyEvent.KEYCODE_BACK) {
                closeAppDrawer();
                return true;
            }
            if (appDrawerOpen) {
                if (!extraAppRows.isEmpty()) {
                    int row = Math.min(drawerFocusRow, extraAppRows.size() - 1);
                    int column = Math.min(drawerFocusColumn, extraAppRows.get(row).size() - 1);
                    View focused = extraAppRows.get(row).get(column);
                    if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                        focused.performClick();
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        int nextColumn = column + (key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                        if (nextColumn >= 0 && nextColumn < extraAppRows.get(row).size())
                            extraAppRows.get(row).get(nextColumn).requestFocus();
                        return true;
                    }
                    if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                        int nextRow = row + (key == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1);
                        if (nextRow < 0) {
                            closeAppDrawer();
                        } else if (nextRow < extraAppRows.size()) {
                            List<View> next = extraAppRows.get(nextRow);
                            next.get(Math.min(column, next.size() - 1)).requestFocus();
                            appDrawer.smoothScrollTo(0,
                                    px(Math.max(0, nextRow - 1) * APP_ROW_HEIGHT));
                        }
                        return true;
                    }
                }
            }
            if (!appDrawerOpen && key == KeyEvent.KEYCODE_DPAD_DOWN) {
                boolean dockFocused = dockExpand != null && dockExpand.hasFocus();
                for (View card : dockCards) dockFocused |= card.hasFocus();
                if (dockFocused) { openAppDrawer(); return true; }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (suppressSaverTouch) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) suppressSaverTouch = false;
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (screensaverActive) {
                hideScreensaver();
                suppressSaverTouch = true;
                resetScreensaverTimer();
                return true;
            }
            resetScreensaverTimer();
        }
        return super.dispatchTouchEvent(event);
    }

    private void setAppTileFocus(FrameLayout tile, View glow, boolean focused) {
        glow.animate().alpha(focused ? 1f : 0f).setDuration(170).start();
        tile.setElevation(focused ? px(14) : 0);
        tile.setScaleX(focused ? 1.015f : 1f);
        tile.setScaleY(focused ? 1.015f : 1f);
    }

    private final class AppGlowDrawable extends Drawable {
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int radius;
        private final boolean outerOnly;
        AppGlowDrawable() { this(32, false); }
        AppGlowDrawable(int radius, boolean outerOnly) {
            this.radius = radius;
            this.outerOnly = outerOnly;
        }
        @Override public void draw(Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            RectF card = new RectF(bounds.left + px(24), bounds.top + px(22),
                    bounds.right - px(24), bounds.bottom - px(22));
            if (outerOnly) {
                // Use the app tile's full-strength aura, then mask its centre because
                // these dashboard cards are translucent and would reveal the fill.
                glow.setStyle(Paint.Style.FILL);
                glow.setColor(0xBA55BAFF);
                glow.setMaskFilter(new android.graphics.BlurMaskFilter(px(26),
                        android.graphics.BlurMaskFilter.Blur.NORMAL));
                canvas.drawRoundRect(card, px(radius), px(radius), glow);
                glow.setColor(0xA57FDBFF);
                glow.setMaskFilter(new android.graphics.BlurMaskFilter(px(11),
                        android.graphics.BlurMaskFilter.Blur.NORMAL));
                canvas.drawRoundRect(card, px(radius), px(radius), glow);
                glow.setMaskFilter(null);
                glow.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
                canvas.drawRoundRect(card, px(radius), px(radius), glow);
                glow.setXfermode(null);
                return;
            }
            glow.setStyle(Paint.Style.FILL);
            glow.setColor(0xBA55BAFF);
            glow.setMaskFilter(new android.graphics.BlurMaskFilter(px(26),
                    android.graphics.BlurMaskFilter.Blur.NORMAL));
            canvas.drawRoundRect(card, px(radius), px(radius), glow);
            glow.setColor(0xA57FDBFF);
            glow.setMaskFilter(new android.graphics.BlurMaskFilter(px(11),
                    android.graphics.BlurMaskFilter.Blur.NORMAL));
            canvas.drawRoundRect(card, px(radius), px(radius), glow);
            glow.setMaskFilter(null);
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    private void launch(String pkg) {
        Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent != null) startActivity(intent);
        else Toast.makeText(this, tr("Nie znaleziono aplikacji: ", "App not found: ") + pkg,
                Toast.LENGTH_SHORT).show();
    }

    private void closeBackgroundApps() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager == null) return;
        new Thread(() -> {
            for (String pkg : CLEANUP_PACKAGES) {
                try { manager.killBackgroundProcesses(pkg); }
                catch (SecurityException ignored) { }
            }
            handler.post(() -> Toast.makeText(this,
                    tr("Poproszono system o zamknięcie aplikacji w tle",
                            "Asked the system to close background apps"), Toast.LENGTH_SHORT).show());
        }, "background-cleanup").start();
    }

    private void openSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (Exception e) { Toast.makeText(this, tr("Ustawienia niedostępne", "Settings unavailable"),
                Toast.LENGTH_SHORT).show(); }
    }

    private void showSavedWallpaper(ImageView image) {
        String choice = selectedWallpaper();
        if ("aurora".equals(choice)) image.setImageResource(R.drawable.aurora_wallpaper);
        else if (choice.startsWith("folder:")) {
            File file = new File(wallpaperFolder(), choice.substring("folder:".length()));
            if (file.getParentFile().equals(wallpaperFolder()) && showWallpaperFile(image, file)) return;
            image.setImageResource(R.drawable.sunset_wallpaper);
        }
        else if ("custom".equals(choice)) {
            File file = new File(getFilesDir(), "custom-wallpaper.img");
            if (showWallpaperFile(image, file)) return;
            image.setImageResource(R.drawable.sunset_wallpaper);
        } else image.setImageResource(R.drawable.sunset_wallpaper);
    }

    private String selectedWallpaper() {
        String choice = getPreferences(MODE_PRIVATE).getString("wallpaper", "sunset");
        if ("dussehra".equals(choice)) {
            getPreferences(MODE_PRIVATE).edit().putString("wallpaper", "sunset").apply();
            return "sunset";
        }
        return choice;
    }

    private void resetScreensaverTimer() {
        handler.removeCallbacks(startScreensaver);
        if (!popupOpen && !screensaverActive && root != null)
            handler.postDelayed(startScreensaver, SCREENSAVER_IDLE_MS);
    }

    private void showScreensaver() {
        if (screensaverActive || popupOpen || root == null) return;
        handler.removeCallbacks(startScreensaver);
        screensaverChoices.clear();
        String selected = selectedWallpaper();
        if ("custom".equals(selected) && new File(getFilesDir(), "custom-wallpaper.img").isFile())
            screensaverChoices.add(selected);
        else if (selected.startsWith("folder:")) {
            File selectedFile = new File(wallpaperFolder(), selected.substring("folder:".length()));
            if (selectedFile.getParentFile().equals(wallpaperFolder()) && selectedFile.isFile())
                screensaverChoices.add(selected);
        }
        File[] photos = wallpaperFolder().listFiles(file -> file.isFile()
                && file.length() <= 25_000_000
                && file.getName().matches("(?i).+\\.(jpg|jpeg|png|webp)"));
        if (photos != null) {
            Arrays.sort(photos, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File file : photos) {
                String choice = "folder:" + file.getName();
                if (!screensaverChoices.contains(choice)) screensaverChoices.add(choice);
            }
        }
        if (new File(getFilesDir(), "custom-wallpaper.img").isFile()
                && !screensaverChoices.contains("custom")) screensaverChoices.add("custom");
        if (screensaverChoices.isEmpty()) return;
        screensaverIndex = 0;
        screensaver = new FrameLayout(this);
        screensaver.setBackgroundColor(0xFF081126);
        screensaverImage = new ImageView(this);
        screensaverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        screensaver.addView(screensaverImage, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        shade.setBackgroundColor(0x33000000);
        screensaver.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        screensaverInfo = new LinearLayout(this);
        screensaverInfo.setOrientation(LinearLayout.VERTICAL);
        screensaverClock = new TextView(this);
        screensaverClock.setTextColor(Color.WHITE);
        screensaverClock.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(104));
        screensaverClock.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
        screensaverClock.setLetterSpacing(0.045f);
        screensaverClock.setShadowLayer(px(12), 0, px(3), 0x99000000);
        screensaverClock.setIncludeFontPadding(false);
        screensaverInfo.addView(screensaverClock);
        screensaverDate = new TextView(this);
        screensaverDate.setTextColor(Color.WHITE);
        screensaverDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(22));
        screensaverDate.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        screensaverDate.setLetterSpacing(0.13f);
        screensaverDate.setShadowLayer(px(7), 0, px(2), 0xBB000000);
        screensaverDate.setPadding(px(3), px(8), 0, 0);
        screensaverInfo.addView(screensaverDate);
        screensaver.addView(screensaverInfo);
        root.addView(screensaver, new FrameLayout.LayoutParams(-1, -1));
        screensaverActive = true;
        showScreensaverSlide();
        handler.removeCallbacks(advanceScreensaver);
        if (screensaverActive) handler.postDelayed(advanceScreensaver, SCREENSAVER_SLIDE_MS);
    }

    private void showScreensaverSlide() {
        if (!screensaverActive || screensaverChoices.isEmpty()) return;
        Bitmap next = null;
        while (!screensaverChoices.isEmpty()) {
            String choice = screensaverChoices.get(screensaverIndex);
            if (choice.startsWith("folder:"))
                next = decodeWallpaperFile(new File(wallpaperFolder(), choice.substring("folder:".length())));
            else if ("custom".equals(choice))
                next = decodeWallpaperFile(new File(getFilesDir(), "custom-wallpaper.img"));
            if (next != null) break;
            screensaverChoices.remove(screensaverIndex);
            if (!screensaverChoices.isEmpty()) screensaverIndex %= screensaverChoices.size();
        }
        if (next == null) {
            hideScreensaver();
            return;
        }
        screensaverImage.animate().cancel();
        screensaverImage.setImageBitmap(next);
        if (screensaverBitmap != null) screensaverBitmap.recycle();
        screensaverBitmap = next;
        screensaverImage.setScaleX(1.02f);
        screensaverImage.setScaleY(1.02f);
        screensaverImage.animate().scaleX(1.09f).scaleY(1.09f)
                .setDuration(SCREENSAVER_SLIDE_MS).start();
        Date now = new Date();
        screensaverClock.setText(new SimpleDateFormat("HH:mm", PL).format(now));
        String formatted = new SimpleDateFormat("EEEE, d MMMM", displayLocale()).format(now);
        screensaverDate.setText(formatted.toUpperCase(PL));
        FrameLayout.LayoutParams info = new FrameLayout.LayoutParams(-2, -2);
        int corner = screensaverIndex % 4;
        info.gravity = (corner % 2 == 0 ? Gravity.LEFT : Gravity.RIGHT)
                | (corner < 2 ? Gravity.TOP : Gravity.BOTTOM);
        info.setMargins(px(70), px(55), px(70), px(65));
        screensaverInfo.setLayoutParams(info);
    }

    private void hideScreensaver() {
        if (!screensaverActive) return;
        screensaverActive = false;
        handler.removeCallbacks(advanceScreensaver);
        if (screensaverImage != null) {
            screensaverImage.animate().cancel();
            screensaverImage.setImageDrawable(null);
        }
        if (screensaverBitmap != null) {
            screensaverBitmap.recycle();
            screensaverBitmap = null;
        }
        if (screensaver != null && screensaver.getParent() instanceof ViewGroup)
            ((ViewGroup) screensaver.getParent()).removeView(screensaver);
        screensaver = null;
        screensaverImage = null;
        screensaverInfo = null;
    }

    private File wallpaperFolder() {
        File folder = getExternalFilesDir("wallpapers");
        if (folder == null) folder = new File(getFilesDir(), "wallpapers");
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    private boolean showWallpaperFile(ImageView image, File file) {
        Bitmap bitmap = decodeWallpaperFile(file);
        if (bitmap == null) return false;
        image.setImageBitmap(bitmap);
        return true;
    }

    private Bitmap decodeWallpaperFile(File file) {
        try (InputStream boundsStream = new FileInputStream(file);
                 InputStream imageStream = new FileInputStream(file)) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(boundsStream, null, options);
                options.inJustDecodeBounds = false;
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 2560
                        || options.outHeight / options.inSampleSize > 1440) options.inSampleSize *= 2;
                 return BitmapFactory.decodeStream(imageStream, null, options);
            } catch (Exception ignored) { }
        return null;
    }

    private void showPhoneUpload() {
        String address = PhotoUploadServer.localIpv4();
        if (address == null) {
            showMessageDialog(tr("Dodawanie zdjęć", "Add photos"),
                    tr("Telewizor nie ma adresu w sieci lokalnej. Połącz go i telefon z tą samą siecią Wi‑Fi.",
                            "The TV has no local network address. Connect it and your phone to the same Wi-Fi network."));
            return;
        }
        if (photoUploadServer != null) photoUploadServer.close();
        TextView status = new TextView(this);
        final boolean[] receivedPhoto = {false};
        PhotoUploadServer server;
        try {
            server = new PhotoUploadServer(wallpaperFolder(), english, filename -> handler.post(() -> {
                receivedPhoto[0] = true;
                status.setText(tr("Zdjęcie dodane. Po zamknięciu zobaczysz je w galerii.",
                        "Photo added. It will appear in the gallery when you close this window."));
            }));
        } catch (Exception e) {
            showMessageDialog(tr("Dodawanie zdjęć", "Add photos"),
                    tr("Nie udało się uruchomić przesyłania. Spróbuj ponownie za chwilę.",
                            "Could not start photo upload. Try again shortly."));
            return;
        }
        photoUploadServer = server;
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(36), px(28), px(36), px(30));
        content.setBackground(shape(night ? 0xFF142548 : 0xFFF5F2EA,
                night ? 0xFF899BC6 : 0xFFB7C4D5, 26, 1));
        detailLine(content, tr("Dodaj zdjęcia z telefonu", "Add photos from phone"), 31, true, ink);
        detailLine(content, tr("Telefon i telewizor muszą być w tej samej sieci.",
                "Phone and TV must be on the same network."), 19, false, muted);
        detailLine(content, tr("Zeskanuj kod aparatem telefonu:",
                "Scan the code with your phone camera:"), 18, false, muted);
        String uploadUrl = "http://" + address + ":" + PhotoUploadServer.PORT + "/";
        try {
            ImageView qr = new ImageView(this);
            qr.setImageBitmap(qrBitmap(uploadUrl, 350));
            qr.setBackground(shape(Color.WHITE, Color.TRANSPARENT, 16));
            qr.setPadding(px(12), px(12), px(12), px(12));
            LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(px(238), px(238));
            qrParams.gravity = Gravity.CENTER_HORIZONTAL;
            qrParams.setMargins(0, px(12), 0, px(12));
            content.addView(qr, qrParams);
        } catch (WriterException e) {
            detailLine(content, tr("Nie udało się przygotować kodu QR.",
                    "Could not generate the QR code."), 17, false, muted);
        }
        detailLine(content, tr("Na telefonie wpisz kod widoczny poniżej. Jeśli skanowanie nie zadziała, wpisz adres:",
                "Enter the code below on your phone. If scanning fails, enter this address:"), 16, false, muted);
        TextView url = new TextView(this);
        url.setText("http://" + address + ":" + PhotoUploadServer.PORT);
        url.setTextColor(ink);
        url.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        url.setPadding(0, px(5), 0, px(5));
        content.addView(url);
        TextView pin = new TextView(this);
        pin.setText(tr("Kod: ", "Code: ") + server.pin());
        pin.setTextColor(ink);
        pin.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(24));
        pin.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pin.setPadding(0, 0, 0, px(12));
        content.addView(pin);
        status.setText(tr("Wybierz zdjęcia na telefonie i naciśnij „Dodaj zdjęcia”.",
                "Select photos on your phone and tap Add photos."));
        status.setTextColor(muted);
        status.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(17));
        status.setMinHeight(px(65));
        content.addView(status);
        TextView close = new TextView(this);
        close.setText(tr("Zamknij", "Close"));
        close.setTextColor(ink);
        close.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        close.setGravity(Gravity.CENTER);
        close.setBackground(shape(night ? 0xFF2D466F : 0xFFE3E9F0,
                night ? 0xFF7899C3 : 0xFFB7C4D5, 13));
        close.setFocusable(true);
        close.setClickable(true);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(px(160), px(48));
        closeParams.gravity = Gravity.END;
        content.addView(close, closeParams);
        dialog.setContentView(content);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        showDialogWithBlur(dialog, () -> {
            server.close();
            if (photoUploadServer == server) photoUploadServer = null;
            if (receivedPhoto[0] && !isFinishing()) handler.post(this::showWallpaperGallery);
        });
        dialog.getWindow().setLayout(px(700), -2);
        close.requestFocus();
    }

    private Bitmap qrBitmap(String value, int size) throws WriterException {
        java.util.Map<EncodeHintType, Object> hints = new java.util.EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size, hints);
        int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++)
            pixels[y * size + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888);
    }

    private void showWallpaperGallery() {
        File[] files = wallpaperFolder().listFiles(file -> file.isFile()
                && file.length() <= 25_000_000
                && file.getName().matches("(?i).+\\.(jpg|jpeg|png|webp)"));
        if (files == null) files = new File[0];
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(34), px(26), px(34), px(29));
        content.setBackground(shape(night ? 0xFF142548 : 0xFFF5F2EA,
                night ? 0xFF899BC6 : 0xFFB7C4D5, 26));
        detailLine(content, tr("Tapety", "Wallpapers"), 31, true, ink);
        detailLine(content, tr("Wybierz zdjęcie, aby ustawić je jako tło pulpitu.",
                "Choose a photo to set it as your background."), 18, false, muted);
        android.widget.HorizontalScrollView gallery = new android.widget.HorizontalScrollView(this);
        gallery.setHorizontalScrollBarEnabled(false);
        gallery.setClipChildren(false);
        gallery.setClipToPadding(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        File[] deleteTarget = {null};
        String[] deleteKey = {null};
        FrameLayout deleteWrap = new FrameLayout(this);
        deleteWrap.setClipChildren(false);
        deleteWrap.setVisibility(View.GONE);
        View deleteGlow = new View(this);
        deleteGlow.setBackground(new AppGlowDrawable(13, false));
        deleteGlow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        deleteGlow.setAlpha(0f);
        deleteWrap.addView(deleteGlow, new FrameLayout.LayoutParams(px(251), px(92)));
        TextView delete = new TextView(this);
        delete.setId(View.generateViewId());
        delete.setText(tr("Usuń zdjęcie", "Delete photo"));
        delete.setGravity(Gravity.CENTER);
        delete.setTextColor(ink);
        delete.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        delete.setBackground(shape(night ? 0xFF2D466F : 0xFFE3E9F0,
                night ? 0xFF7899C3 : 0xFFB7C4D5, 13));
        delete.setFocusable(true);
        delete.setClickable(true);
        delete.setOnFocusChangeListener((v, focused) -> showTileGlow(deleteGlow, focused));
        delete.setOnClickListener(v -> {
            if (deleteTarget[0] != null) confirmWallpaperDeletion(dialog, deleteKey[0], deleteTarget[0]);
        });
        FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(px(203), px(48));
        deleteParams.leftMargin = px(24);
        deleteParams.topMargin = px(22);
        deleteWrap.addView(delete, deleteParams);
        String selected = selectedWallpaper();
        addWallpaperThumbnail(row, dialog, "sunset", R.drawable.sunset_wallpaper, null, selected,
                deleteWrap, delete, deleteTarget, deleteKey);
        addWallpaperThumbnail(row, dialog, "aurora", R.drawable.aurora_wallpaper, null, selected,
                deleteWrap, delete, deleteTarget, deleteKey);
        File legacy = new File(getFilesDir(), "custom-wallpaper.img");
        if (legacy.isFile()) addWallpaperThumbnail(row, dialog, "custom", 0, legacy, selected,
                deleteWrap, delete, deleteTarget, deleteKey);
        for (File file : files)
            addWallpaperThumbnail(row, dialog, "folder:" + file.getName(), 0, file, selected,
                    deleteWrap, delete, deleteTarget, deleteKey);
        gallery.addView(row);
        content.addView(gallery, new LinearLayout.LayoutParams(-1, px(232)));
        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, px(8), 0, 0);
        footer.setClipChildren(false);
        footer.setClipToPadding(false);
        FrameLayout addWrap = new FrameLayout(this);
        addWrap.setClipChildren(false);
        View addGlow = new View(this);
        addGlow.setBackground(new AppGlowDrawable(14, false));
        addGlow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        addGlow.setAlpha(0f);
        addWrap.addView(addGlow, new FrameLayout.LayoutParams(px(328), px(97)));
        LinearLayout add = new LinearLayout(this);
        add.setGravity(Gravity.CENTER);
        add.setPadding(px(15), 0, px(15), 0);
        add.setBackground(shape(night ? 0xFF2D466F : 0xFF385E8F,
                night ? 0xFF7899C3 : 0xFF385E8F, 14));
        ImageView phone = new ImageView(this);
        phone.setImageResource(R.drawable.ic_phone_add);
        add.addView(phone, new LinearLayout.LayoutParams(px(29), px(29)));
        TextView addLabel = new TextView(this);
        addLabel.setText(tr("Dodaj z telefonu", "Add from phone"));
        addLabel.setTextColor(Color.WHITE);
        addLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        addLabel.setPadding(px(10), 0, 0, 0);
        add.addView(addLabel);
        add.setFocusable(true);
        add.setFocusableInTouchMode(true);
        add.setClickable(true);
        add.setId(View.generateViewId());
        add.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        add.setOnFocusChangeListener((v, focused) -> {
            showTileGlow(addGlow, focused);
            if (focused) { deleteWrap.setVisibility(View.GONE); deleteTarget[0] = null; }
        });
        add.setOnClickListener(v -> { dialog.dismiss(); handler.post(this::showPhoneUpload); });
        add.setOnKeyListener((v, key, event) -> {
            if ((key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER)
                    && event.getAction() == KeyEvent.ACTION_UP) {
                add.performClick();
                return true;
            }
            return false;
        });
        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(px(280), px(53));
        addParams.leftMargin = px(24);
        addParams.topMargin = px(22);
        addWrap.addView(add, addParams);
        footer.addView(addWrap, new LinearLayout.LayoutParams(px(328), px(97)));
        View spacer = new View(this);
        footer.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        footer.addView(deleteWrap, new LinearLayout.LayoutParams(px(251), px(92)));
        FrameLayout closeWrap = new FrameLayout(this);
        closeWrap.setClipChildren(false);
        View closeGlow = new View(this);
        closeGlow.setBackground(new AppGlowDrawable(13, false));
        closeGlow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        closeGlow.setAlpha(0f);
        closeWrap.addView(closeGlow, new FrameLayout.LayoutParams(px(203), px(92)));
        TextView close = new TextView(this);
        close.setText(tr("Zamknij", "Close"));
        close.setGravity(Gravity.CENTER);
        close.setTextColor(ink);
        close.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        close.setBackground(shape(night ? 0xFF2D466F : 0xFFE3E9F0,
                night ? 0xFF7899C3 : 0xFFB7C4D5, 13));
        close.setFocusable(true);
        close.setFocusableInTouchMode(true);
        close.setClickable(true);
        close.setId(View.generateViewId());
        close.setOnFocusChangeListener((v, focused) -> {
            showTileGlow(closeGlow, focused);
            if (focused) { deleteWrap.setVisibility(View.GONE); deleteTarget[0] = null; }
        });
        add.setNextFocusRightId(close.getId());
        delete.setNextFocusLeftId(add.getId());
        delete.setNextFocusRightId(close.getId());
        close.setNextFocusLeftId(add.getId());
        close.setOnClickListener(v -> dialog.dismiss());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(px(155), px(48));
        closeParams.leftMargin = px(24);
        closeParams.topMargin = px(22);
        closeWrap.addView(close, closeParams);
        footer.addView(closeWrap, new LinearLayout.LayoutParams(px(203), px(92)));
        content.addView(footer);
        dialog.setContentView(content);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        showDialogWithBlur(dialog);
        dialog.getWindow().setLayout(px(1080), -2);
        add.requestFocusFromTouch();
    }

    private void addWallpaperThumbnail(LinearLayout row, Dialog dialog, String key,
            int resource, File file, String selected, View deleteWrap, TextView delete,
            File[] deleteTarget, String[] deleteKey) {
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);
        View glow = new View(this);
        glow.setBackground(new AppGlowDrawable(18, false));
        glow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        glow.setAlpha(0f);
        wrapper.addView(glow, new FrameLayout.LayoutParams(px(308), px(226)));
        FrameLayout tile = new FrameLayout(this);
        tile.setBackground(shape(night ? 0xFF263A5E : 0xFFE0E7EF,
                key.equals(selected) ? 0xFF80CAFF : Color.TRANSPARENT, 18,
                key.equals(selected) ? 4 : 1));
        tile.setClipToOutline(true);
        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (file == null) preview.setImageResource(resource);
        else preview.setImageBitmap(decodeWallpaperThumbnail(file));
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(-1, -1);
        imageParams.setMargins(px(4), px(4), px(4), px(4));
        tile.addView(preview, imageParams);
        if (key.equals(selected)) {
            TextView check = new TextView(this);
            check.setText("✓");
            check.setTextColor(Color.WHITE);
            check.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(24));
            check.setGravity(Gravity.CENTER);
            check.setBackground(shape(0xDD1E426C, Color.TRANSPARENT, 25));
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(px(38), px(38),
                    Gravity.TOP | Gravity.RIGHT);
            checkParams.setMargins(0, px(8), px(8), 0);
            tile.addView(check, checkParams);
        }
        tile.setContentDescription(file == null ? key : file.getName());
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setOnFocusChangeListener((v, focused) -> {
            setAppTileFocus(tile, glow, focused);
            if (focused) {
                deleteTarget[0] = file;
                deleteKey[0] = file == null ? null : key;
                deleteWrap.setVisibility(file == null ? View.GONE : View.VISIBLE);
            }
        });
        if (file != null) tile.setNextFocusDownId(delete.getId());
        tile.setOnClickListener(v -> {
            getPreferences(MODE_PRIVATE).edit().putString("wallpaper", key).apply();
            dialog.dismiss();
            buildScreen();
        });
        FrameLayout.LayoutParams tileParams = new FrameLayout.LayoutParams(px(260), px(182));
        tileParams.leftMargin = px(24);
        tileParams.topMargin = px(22);
        wrapper.addView(tile, tileParams);
        row.addView(wrapper, new LinearLayout.LayoutParams(px(308), px(226)));
    }

    private void confirmWallpaperDeletion(Dialog gallery, String key, File file) {
        if (key == null || file == null) return;
        Dialog confirmation = new Dialog(this);
        confirmation.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(34), px(24), px(34), px(24));
        content.setBackground(shape(night ? 0xFF142548 : 0xFFF5F2EA,
                night ? 0xFF899BC6 : 0xFFB7C4D5, 26, 1));
        detailLine(content, tr("Usunąć zdjęcie?", "Delete this photo?"), 29, true, ink);
        detailLine(content, tr("Zdjęcie zniknie z galerii i wygaszacza.",
                "The photo will be removed from the gallery and screensaver."), 19, false, muted);
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        actions.setClipChildren(false);
        actions.setClipToPadding(false);
        TextView cancel = wallpaperConfirmationAction(actions, tr("Anuluj", "Cancel"), ink, confirmation::dismiss);
        wallpaperConfirmationAction(actions, tr("Usuń", "Delete"), night ? 0xFFFFB8BE : 0xFFA12636, () -> {
            try {
                File folder = wallpaperFolder().getCanonicalFile();
                File target = file.getCanonicalFile();
                File legacy = new File(getFilesDir(), "custom-wallpaper.img").getCanonicalFile();
                if (!(target.getParentFile().equals(folder) || target.equals(legacy))
                        || !target.isFile() || !target.delete())
                    throw new IOException("Nie udało się usunąć zdjęcia");
                if (key.equals(getPreferences(MODE_PRIVATE).getString("wallpaper", "sunset")))
                    getPreferences(MODE_PRIVATE).edit().putString("wallpaper", "sunset").apply();
                confirmation.dismiss();
                gallery.dismiss();
                buildScreen();
                handler.post(this::showWallpaperGallery);
            } catch (IOException e) {
                Toast.makeText(this, tr("Nie udało się usunąć zdjęcia", "Could not delete photo"), Toast.LENGTH_LONG).show();
            }
        });
        content.addView(actions);
        confirmation.setContentView(content);
        confirmation.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        confirmation.show();
        confirmation.getWindow().setLayout(px(620), -2);
        cancel.requestFocusFromTouch();
    }

    private TextView wallpaperConfirmationAction(LinearLayout actions, String label,
            int textColor, Runnable click) {
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setClipChildren(false);
        View glow = new View(this);
        glow.setBackground(new AppGlowDrawable(13, false));
        glow.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        glow.setAlpha(0f);
        wrapper.addView(glow, new FrameLayout.LayoutParams(px(188), px(92)));
        TextView button = new TextView(this);
        button.setText(label);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(textColor);
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        button.setBackground(shape(night ? 0xFF2D466F : 0xFFE3E9F0,
                night ? 0xFF7899C3 : 0xFFB7C4D5, 13));
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setClickable(true);
        button.setOnFocusChangeListener((v, focused) -> showTileGlow(glow, focused));
        button.setOnClickListener(v -> click.run());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(px(140), px(48));
        params.leftMargin = px(24);
        params.topMargin = px(22);
        wrapper.addView(button, params);
        actions.addView(wrapper, new LinearLayout.LayoutParams(px(188), px(92)));
        return button;
    }

    private Bitmap decodeWallpaperThumbnail(File file) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (bounds.outWidth / options.inSampleSize > 520
                    || bounds.outHeight / options.inSampleSize > 360) options.inSampleSize *= 2;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Exception ignored) { return null; }
    }

    private void showSources() {
        TvInputManager manager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        List<TvInputInfo> inputs = new ArrayList<>();
        if (manager != null) for (TvInputInfo info : manager.getTvInputList()) {
            if (info.getType() == TvInputInfo.TYPE_HDMI) inputs.add(info);
        }
        inputs.sort((a, b) -> String.valueOf(a.loadLabel(this))
                .compareToIgnoreCase(String.valueOf(b.loadLabel(this))));
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setPadding(px(30), px(25), px(30), px(29));
        content.setBackground(shape(night ? 0xFC1A2B4B : 0xFFF2EEE7,
                night ? 0x777F9BC5 : 0x8895A6C0, 27, 1));
        TextView title = new TextView(this);
        title.setText(tr("Źródła", "Inputs"));
        title.setTextColor(night ? Color.WHITE : 0xFF17284B);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(31));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title);
        TextView hint = new TextView(this);
        hint.setText(tr("Wybierz telewizję lub wejście HDMI", "Choose live TV or an HDMI input"));
        hint.setTextColor(night ? 0xFFBFCDE8 : 0xFF5B6B85);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(17));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = px(3);
        hintParams.bottomMargin = px(23);
        content.addView(hint, hintParams);
        LinearLayout row = null;
        View first = null;
        for (int i = 0; i <= inputs.size(); i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, px(139));
                content.addView(row, rowParams);
            }
            final int item = i;
            String label;
            if (i == 0) label = tr("Telewizja", "Live TV");
            else {
                CharSequence inputLabel = inputs.get(i - 1).loadLabel(this);
                label = inputLabel == null || inputLabel.length() == 0 ? "HDMI " + i : inputLabel.toString();
            }
            View tile = sourceTile(label, i == 0, () -> {
                dialog.dismiss();
                if (item == 0) { launch("org.droidtv.playtv"); return; }
                try {
                    Intent view = new Intent(Intent.ACTION_VIEW,
                            TvContract.buildChannelUriForPassthroughInput(inputs.get(item - 1).getId()));
                    startActivity(view);
                } catch (Exception e) {
                    Toast.makeText(this, tr("Telewizor nie otworzył wejścia HDMI",
                            "The TV could not open the HDMI input"), Toast.LENGTH_SHORT).show();
                }
            });
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(px(240), px(122));
            tileParams.setMargins(px(7), px(4), px(7), px(11));
            row.addView(tile, tileParams);
            if (first == null) first = tile;
        }
        dialog.setContentView(content);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        showDialogWithBlur(dialog);
        dialog.getWindow().setLayout(px(820), -2);
        if (first != null) first.requestFocus();
    }

    private View sourceTile(String label, boolean television, Runnable action) {
        int normalText = night ? Color.WHITE : 0xFF17284B;
        int focusedText = night ? 0xFF152849 : Color.WHITE;
        int normalFill = night ? 0xFF344B72 : 0xFFFFFFFF;
        int focusedFill = night ? 0xFFF0F6FF : 0xFF244574;
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setContentDescription(label);
        tile.setBackground(shape(normalFill, night ? 0x668DA8CF : 0x99C1CCE0, 21, 1));
        SourceGlyph glyph = new SourceGlyph(television);
        glyph.setColor(normalText);
        tile.addView(glyph, new LinearLayout.LayoutParams(px(49), px(49)));
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(normalText);
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(21));
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.setMargins(px(9), px(6), px(9), 0);
        tile.addView(name, nameParams);
        tile.setOnFocusChangeListener((view, focused) -> {
            tile.setBackground(shape(focused ? focusedFill : normalFill,
                    focused ? Color.TRANSPARENT : night ? 0x668DA8CF : 0x99C1CCE0,
                    21, focused ? 0 : 1));
            name.setTextColor(focused ? focusedText : normalText);
            glyph.setColor(focused ? focusedText : normalText);
            tile.setElevation(focused ? px(12) : 0);
            tile.setScaleX(focused ? 1.035f : 1f);
            tile.setScaleY(focused ? 1.035f : 1f);
        });
        tile.setOnClickListener(view -> action.run());
        return tile;
    }

    private final class SourceGlyph extends View {
        private final boolean television;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        SourceGlyph(boolean television) { super(DashboardActivity.this); this.television = television; }
        void setColor(int color) { paint.setColor(color); invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.scale(getWidth() / 64f, getHeight() / 64f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            if (television) {
                canvas.drawRoundRect(new RectF(8, 11, 56, 46), 5, 5, paint);
                canvas.drawLine(25, 46, 25, 53, paint);
                canvas.drawLine(39, 46, 39, 53, paint);
                canvas.drawLine(20, 53, 44, 53, paint);
                Path play = new Path();
                play.moveTo(28, 22); play.lineTo(28, 36); play.lineTo(40, 29); play.close();
                canvas.drawPath(play, paint);
            } else {
                Path port = new Path();
                port.moveTo(12, 20); port.lineTo(52, 20); port.lineTo(47, 44);
                port.lineTo(17, 44); port.close();
                canvas.drawPath(port, paint);
                canvas.drawLine(23, 27, 41, 27, paint);
                canvas.drawLine(24, 34, 40, 34, paint);
            }
            canvas.restore();
        }
    }

    private final class WeatherIcon extends View {
        private final String condition;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        WeatherIcon(String condition) { super(DashboardActivity.this); this.condition = condition; }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (weatherTypeface == null) weatherTypeface = Typeface.createFromAsset(getAssets(), "weathericons-regular-webfont.ttf");
            boolean day = !condition.startsWith("n");
            int code;
            try { code = Integer.parseInt(condition.substring(1)); }
            catch (Exception ignored) { code = 3; }
            int glyph;
            int tint;
            if (code == 0) { glyph = day ? 0xf00d : 0xf02e; tint = 0xFFFFD65D; }
            else if (code == 1) { glyph = day ? 0xf00c : 0xf081; tint = 0xFFFFD65D; }
            else if (code == 2) { glyph = day ? 0xf002 : 0xf086; tint = 0xFFFFD65D; }
            else if (code == 3) { glyph = 0xf013; tint = 0xFFE4F0FF; }
            else if (code == 45 || code == 48) { glyph = day ? 0xf003 : 0xf04a; tint = 0xFFCCD9E9; }
            else if (code >= 51 && code <= 55) { glyph = day ? 0xf00b : 0xf02b; tint = 0xFF82CFFF; }
            else if (code == 56 || code == 57 || code == 66 || code == 67) {
                glyph = day ? 0xf006 : 0xf026; tint = 0xFFA7DBF9;
            }
            else if (code >= 61 && code <= 65) { glyph = day ? 0xf008 : 0xf028; tint = 0xFF82CFFF; }
            else if (code >= 71 && code <= 77 || code == 85 || code == 86) {
                glyph = day ? 0xf00a : 0xf02a; tint = 0xFFB5E7FF;
            }
            else if (code >= 80 && code <= 82) { glyph = day ? 0xf009 : 0xf029; tint = 0xFF82CFFF; }
            else if (code == 96 || code == 99) { glyph = day ? 0xf004 : 0xf024; tint = 0xFFFFC860; }
            else if (code >= 95) { glyph = day ? 0xf010 : 0xf02d; tint = 0xFFFFC860; }
            else { glyph = 0xf013; tint = 0xFFE4F0FF; }
            p.reset(); p.setAntiAlias(true); p.setTypeface(weatherTypeface);
            p.setStyle(Paint.Style.FILL); p.setColor(tint);
            p.setTextSize(Math.min(getWidth() * 0.86f, getHeight() * 0.9f));
            String symbol = new String(Character.toChars(glyph));
            Paint.FontMetrics fm = p.getFontMetrics();
            float baseline = (getHeight() - fm.ascent - fm.descent) / 2f;
            c.drawText(symbol, (getWidth() - p.measureText(symbol)) / 2f, baseline, p);
        }
    }

}
