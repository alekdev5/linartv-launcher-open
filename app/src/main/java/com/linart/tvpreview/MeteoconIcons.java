package com.linart.tvpreview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.caverock.androidsvg.SVG;

import java.io.InputStream;
import java.util.HashMap;

/**
 * Bundled Meteocons (MIT, assets/meteocons) for Open-Meteo weather codes.
 * Codes arrive as "d<WMO>" or "n<WMO>" (day / night) from PublicForecastClient.
 */
final class MeteoconIcons {
    private final Context context;
    private final HashMap<String, Bitmap> cache = new HashMap<>();

    MeteoconIcons(Context context) { this.context = context.getApplicationContext(); }

    /** Asset name (without .svg) for an Open-Meteo icon code such as "d61" or "n2". */
    static String assetFor(String code) {
        boolean night = code != null && code.startsWith("n");
        int wmo;
        try { wmo = Integer.parseInt(code.substring(1)); }
        catch (Exception ignored) { wmo = 3; }
        String dn = night ? "night" : "day";
        switch (wmo) {
            case 0: return "clear-" + dn;
            case 1: return "mostly-clear-" + dn;
            case 2: return "partly-cloudy-" + dn;
            case 45: case 48: return "fog-" + dn;
            case 51: case 53: case 55: return "drizzle";
            case 56: case 57: case 66: case 67: return "sleet";
            case 61: case 63: return "rain";
            case 65: return "extreme-rain";
            case 80: case 81: case 82: return "partly-cloudy-" + dn + "-rain";
            case 71: case 73: case 77: return "snow";
            case 75: return "extreme-snow";
            case 85: case 86: return "partly-cloudy-" + dn + "-snow";
            case 95: return "thunderstorms-" + dn + "-rain";
            case 96: case 99: return "thunderstorms-" + dn + "-hail";
            default: return "overcast";
        }
    }

    /** Rightmost column (in pixels) that contains visible artwork, or the width if none. */
    static int inkRight(Bitmap bitmap) {
        if (bitmap == null) return 0;
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] column = new int[h];
        for (int x = w - 1; x >= 0; x--) {
            bitmap.getPixels(column, 0, 1, x, 0, 1, h);
            for (int pixel : column) if ((pixel >>> 24) > 40) return x + 1;
        }
        return w;
    }

    /** Leftmost column (in pixels) that contains visible artwork, or 0 if none. */
    static int inkLeft(Bitmap bitmap) {
        if (bitmap == null) return 0;
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] column = new int[h];
        for (int x = 0; x < w; x++) {
            bitmap.getPixels(column, 0, 1, x, 0, 1, h);
            for (int pixel : column) if ((pixel >>> 24) > 40) return x;
        }
        return 0;
    }

    /** Square bitmap of the icon, cached per asset and size. Returns null if the asset cannot be read. */
    Bitmap bitmap(String code, int size) {
        String name = assetFor(code);
        String key = name + "@" + size;
        Bitmap cached = cache.get(key);
        if (cached != null) return cached;
        try (InputStream in = context.getAssets().open("meteocons/" + name + ".svg")) {
            SVG svg = SVG.getFromInputStream(in);
            // All icons draw inside 16..112 of the 128-unit box (sun rays reach the edges);
            // crop the shared empty margin so icons fill their slot, same scale for every icon.
            svg.setDocumentViewBox(14, 14, 100, 100);
            svg.setDocumentWidth(size);
            svg.setDocumentHeight(size);
            Bitmap result = Bitmap.createBitmap(Math.max(1, size), Math.max(1, size), Bitmap.Config.ARGB_8888);
            svg.renderToCanvas(new Canvas(result));
            cache.put(key, result);
            return result;
        } catch (Exception e) {
            android.util.Log.w("LinarTV", "Meteocon unavailable: " + name, e);
            return null;
        }
    }
}
