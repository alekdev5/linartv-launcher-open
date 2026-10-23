package com.linart.tvpreview;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Keyless forecast, current conditions and city search from Open-Meteo (CC BY 4.0). */
final class PublicForecastClient {
    private static final String FORECAST = "https://api.open-meteo.com/v1/forecast";
    private static final String GEOCODING = "https://geocoding-api.open-meteo.com/v1/search";

    /** A city returned by the Open-Meteo geocoding search. */
    static final class Place {
        final String name, region, country;
        final double latitude, longitude;
        Place(String name, String region, String country, double latitude, double longitude) {
            this.name = name; this.region = region; this.country = country;
            this.latitude = latitude; this.longitude = longitude;
        }
        String label() {
            StringBuilder label = new StringBuilder(name);
            if (!region.isEmpty()) label.append(", ").append(region);
            if (!country.isEmpty()) label.append(", ").append(country);
            return label.toString();
        }
    }

    static List<Place> searchPlaces(String query, String language) throws Exception {
        JSONObject response = read(GEOCODING + "?name=" + URLEncoder.encode(query, "UTF-8")
                + "&count=10&language=" + URLEncoder.encode(language, "UTF-8") + "&format=json", 100_000);
        List<Place> places = new ArrayList<>();
        JSONArray results = response.optJSONArray("results");
        if (results == null) return places;
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);
            if (!item.has("latitude") || !item.has("longitude")) continue;
            places.add(new Place(item.optString("name", query), item.optString("admin1", ""),
                    item.optString("country", ""), item.getDouble("latitude"), item.getDouble("longitude")));
        }
        return places;
    }

    static JSONObject fetch(double latitude, double longitude) throws Exception {
        JSONObject response = read(FORECAST
                + "?latitude=" + String.format(Locale.ROOT, "%.4f", latitude)
                + "&longitude=" + String.format(Locale.ROOT, "%.4f", longitude)
                + "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,pressure_msl,wind_speed_10m,is_day"
                + "&hourly=temperature_2m,weather_code,is_day"
                + "&daily=temperature_2m_max,temperature_2m_min,weather_code,sunrise,sunset"
                + "&wind_speed_unit=ms&timezone=auto&forecast_days=7", 300_000);
        TimeZone zone = TimeZone.getTimeZone(response.optString("timezone", "UTC"));

        JSONObject rawHourly = response.getJSONObject("hourly");
        JSONArray times = rawHourly.getJSONArray("time");
        JSONArray temperatures = rawHourly.getJSONArray("temperature_2m");
        JSONArray codes = rawHourly.getJSONArray("weather_code");
        JSONArray daylight = rawHourly.getJSONArray("is_day");
        JSONArray hours = new JSONArray();
        long now = System.currentTimeMillis();
        String currentIcon = "";
        long nearest = Long.MAX_VALUE;
        for (int i = 0; i < times.length(); i++) {
            Date time = parseTime(times.getString(i), zone);
            if (time == null) continue;
            long distance = Math.abs(time.getTime() - now);
            String icon = icon(codes.getInt(i), daylight.optInt(i, 1) == 1);
            if (distance < nearest) { nearest = distance; currentIcon = icon; }
            if (time.getTime() >= now - 30 * 60_000L && time.getTime() <= now + 12 * 60 * 60_000L)
                hours.put(new JSONObject().put("time", time.getTime())
                        .put("temperature", temperatures.getDouble(i)).put("icon", icon));
        }

        JSONObject rawDaily = response.getJSONObject("daily");
        JSONArray dayIcons = new JSONArray();
        JSONArray dayCodes = rawDaily.getJSONArray("weather_code");
        for (int i = 0; i < dayCodes.length(); i++) dayIcons.put(icon(dayCodes.getInt(i), true));
        JSONObject daily = new JSONObject()
                .put("time", rawDaily.getJSONArray("time"))
                .put("temperature_2m_max", rawDaily.getJSONArray("temperature_2m_max"))
                .put("temperature_2m_min", rawDaily.getJSONArray("temperature_2m_min"))
                .put("icon", dayIcons);
        JSONObject sun = new JSONObject().put("Sunrise", rawDaily.getJSONArray("sunrise").getString(0))
                .put("Sunset", rawDaily.getJSONArray("sunset").getString(0));
        JSONObject rawCurrent = response.optJSONObject("current");
        JSONObject current = new JSONObject();
        if (rawCurrent != null) {
            Date measured = parseTime(rawCurrent.optString("time", ""), zone);
            if (measured != null) current.put("time", measured.getTime());
            copy(rawCurrent, "temperature_2m", current, "temperature");
            copy(rawCurrent, "relative_humidity_2m", current, "humidity");
            copy(rawCurrent, "precipitation", current, "precipitation");
            copy(rawCurrent, "pressure_msl", current, "pressure");
            copy(rawCurrent, "wind_speed_10m", current, "wind");
            if (rawCurrent.has("weather_code"))
                currentIcon = icon(rawCurrent.optInt("weather_code"), rawCurrent.optInt("is_day", 1) == 1);
        }
        return new JSONObject().put("provider", "Open-Meteo").put("icon_format", 2)
                .put("timezone", zone.getID()).put("hourly", hours)
                .put("daily", daily).put("current_icon", currentIcon).put("sun", sun)
                .put("current", current);
    }

    private static void copy(JSONObject from, String key, JSONObject to, String name) throws Exception {
        if (from.has(key) && !from.isNull(key)) to.put(name, from.getDouble(key));
    }

    private static JSONObject read(String address, int limit) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        try {
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException("Open-Meteo HTTP " + connection.getResponseCode());
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                    if (bytes.size() > limit) throw new IllegalStateException("Open-Meteo response too large");
                }
                return new JSONObject(bytes.toString("UTF-8"));
            }
        } finally { connection.disconnect(); }
    }

    static Date parseTime(String value, TimeZone zone) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(
                    value.endsWith("Z") ? "yyyy-MM-dd'T'HH:mm:ss'Z'" : "yyyy-MM-dd'T'HH:mm", Locale.ROOT);
            format.setTimeZone(value.endsWith("Z") ? TimeZone.getTimeZone("UTC") : zone);
            return format.parse(value);
        } catch (Exception ignored) { return null; }
    }

    private static String icon(int code, boolean day) {
        return (day ? "d" : "n") + code;
    }
}
