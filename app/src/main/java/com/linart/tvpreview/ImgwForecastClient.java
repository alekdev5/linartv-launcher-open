package com.linart.tvpreview;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** IMGW forecast and current model analysis for a selected location in Poland. */
final class ImgwForecastClient {
    private static final String ROOT = "https://meteo.imgw.pl/api/v1/forecast/";
    // Public client token distributed in the JavaScript of meteo.imgw.pl (October 2026).
    private static final String TOKEN = "p4DXKjsYadfBV21TYrDk";
    private static final TimeZone WARSAW = TimeZone.getTimeZone("Europe/Warsaw");

    static JSONObject fetch(double latitude, double longitude, String city) throws Exception {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < 49 || latitude > 55 || longitude < 14 || longitude > 24.2)
            throw new IllegalArgumentException("Select a location in Poland");
        String location = String.format(Locale.ROOT, "&lat=%.6f&lon=%.6f", latitude, longitude);
        JSONObject shortTerm = read("fcapi?token=" + TOKEN + location + "&m=hybrid").getJSONObject("data");
        JSONObject longTerm = read("forecastapi?token=" + TOKEN + location).getJSONObject("data");
        if (!shortTerm.optBoolean("Valid") || !longTerm.optBoolean("Valid"))
            throw new IllegalStateException("IMGW forecast invalid");
        JSONArray future = longTerm.getJSONArray("Daily_Data");
        if (future.length() < 6) throw new IllegalStateException("IMGW forecast incomplete");

        SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        day.setTimeZone(WARSAW);
        String today = day.format(new Date());
        JSONArray dates = new JSONArray();
        JSONArray highs = new JSONArray();
        JSONArray lows = new JSONArray();
        JSONArray icons = new JSONArray();
        dates.put(today);

        JSONArray hourly = shortTerm.getJSONArray("Data");
        JSONArray nextHours = new JSONArray();
        long now = System.currentTimeMillis();
        long nearest = Long.MAX_VALUE;
        String currentIcon = "";
        JSONObject currentHour = null;
        double high = -999, low = 999;
        for (int i = 0; i < hourly.length(); i++) {
            JSONObject item = hourly.getJSONObject(i);
            Date moment = parseTime(item.optString("Date"));
            if (moment == null) continue;
            String icon = item.optString("Icon10", item.optString("Icon", ""));
            if (moment.getTime() >= now - 30 * 60 * 1000L
                    && moment.getTime() <= now + 12 * 60 * 60 * 1000L) {
                double hourlyTemp = kelvin(item.optDouble("Temperature", Double.NaN));
                if (!Double.isNaN(hourlyTemp)) nextHours.put(new JSONObject()
                        .put("time", moment.getTime()).put("temperature", hourlyTemp)
                        .put("icon", icon));
            }
            long distance = Math.abs(moment.getTime() - now);
            if (!icon.isEmpty() && distance < nearest) {
                nearest = distance; currentIcon = icon; currentHour = item;
            }
            if (today.equals(day.format(moment))) {
                double temp = kelvin(item.optDouble("Temperature", Double.NaN));
                if (!Double.isNaN(temp)) { high = Math.max(high, temp); low = Math.min(low, temp); }
            }
        }
        if (currentIcon.isEmpty()) throw new IllegalStateException("IMGW current icon missing");
        highs.put(high > -900 ? high : JSONObject.NULL);
        lows.put(low < 900 ? low : JSONObject.NULL);
        icons.put(currentIcon);

        // IMGW's daily list follows UTC days, so shortly after midnight in Warsaw it can still
        // start with today. Only later days go after today's card.
        int added = 0;
        for (int i = 0; i < future.length() && added < 6; i++) {
            JSONObject item = future.getJSONObject(i);
            Date date = parseTime(item.getString("Date"));
            if (date == null) throw new IllegalStateException("IMGW daily date missing");
            String label = day.format(date);
            if (label.compareTo(today) <= 0) continue;
            dates.put(label);
            highs.put(kelvin(item.getDouble("Temp_Max")));
            lows.put(kelvin(item.getDouble("Temp_Min")));
            icons.put(item.getString("Icon"));
            added++;
        }
        if (added < 5) throw new IllegalStateException("IMGW forecast incomplete");

        double night = Double.NaN;
        JSONArray dayNight = shortTerm.optJSONArray("Day_Night_Data");
        if (dayNight != null) for (int i = 0; i < dayNight.length(); i++) {
            JSONObject item = dayNight.getJSONObject(i);
            if (!item.optBoolean("isDay")) {
                night = kelvin(item.optDouble("Temp_Min", Double.NaN));
                break;
            }
        }

        JSONObject daily = new JSONObject().put("time", dates)
                .put("temperature_2m_max", highs).put("temperature_2m_min", lows)
                .put("icon", icons);
        JSONObject result = new JSONObject().put("provider", "IMGW").put("icon_format", 3)
                .put("daily", daily).put("hourly", nextHours).put("current_icon", currentIcon);
        if (!Double.isNaN(night)) result.put("night_temperature", night);
        JSONObject sun = shortTerm.optJSONObject("Sun");
        if (sun != null) result.put("sun", sun);
        return result.put("timezone", WARSAW.getID()).put("city", city)
                .put("current", current(currentHour));
    }

    /** The nearest IMGW analysis/forecast step, not a reading from a distant station. */
    private static JSONObject current(JSONObject item) throws Exception {
        if (item == null) throw new IllegalStateException("Missing IMGW current conditions");
        JSONObject current = new JSONObject().put("kind", "model");
        Date time = parseTime(item.optString("Date"));
        if (time != null) current.put("time", time.getTime());
        copy(item, "Temperature", current, "temperature", 1, -273.15);
        copy(item, "Humidity", current, "humidity", 1, 0);
        copy(item, "PressureMSL", current, "pressure", 0.01, 0);
        copy(item, "Wind_Speed", current, "wind", 1, 0);
        copy(item, "Precipitation10m", current, "precipitation", 1, 0);
        return current;
    }

    private static void copy(JSONObject from, String key, JSONObject to, String name,
                             double factor, double offset) throws Exception {
        double value = from.optDouble(key, Double.NaN);
        if (Double.isFinite(value)) to.put(name, value * factor + offset);
    }

    static Date parseTime(String value) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(value);
        } catch (Exception ignored) { return null; }
    }

    private static double kelvin(double value) { return value - 273.15; }

    private static JSONObject read(String path) throws Exception {
        return readUrl(ROOT + path, 2_000_000);
    }

    private static JSONObject readUrl(String address, int limit) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        try {
            if (connection.getResponseCode() != 200) throw new IllegalStateException("IMGW HTTP " + connection.getResponseCode());
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                    if (bytes.size() > limit) throw new IllegalStateException("IMGW response too large");
                }
                return new JSONObject(bytes.toString("UTF-8"));
            }
        } finally { connection.disconnect(); }
    }
}
