package com.linart.tvpreview;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/** Polish city coordinates from the Open-Meteo geocoding service (GeoNames). */
final class PolishCityClient {

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
                + "&count=10&countryCode=PL&language=" + URLEncoder.encode(language, "UTF-8") + "&format=json", 100_000);
        List<Place> places = new ArrayList<>();
        JSONArray results = response.optJSONArray("results");
        if (results == null) return places;
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);
            if (!"PL".equals(item.optString("country_code"))
                    || !item.has("latitude") || !item.has("longitude")) continue;
            places.add(new Place(item.optString("name", query), item.optString("admin1", ""),
                    item.optString("country", ""), item.getDouble("latitude"), item.getDouble("longitude")));
        }
        return places;
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

}
