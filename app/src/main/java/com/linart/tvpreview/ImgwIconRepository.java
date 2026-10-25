package com.linart.tvpreview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Renders IMGW's own forecast symbol for the exact icon code returned by its forecast. */
final class ImgwIconRepository {
    private static final String BASE = "https://meteo.imgw.pl/assets/images/weather-icons/";
    private final File directory;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HashMap<String, Bitmap> memory = new HashMap<>();
    private final HashMap<String, List<Slot>> pending = new HashMap<>();

    private static final class Slot {
        final ImageView image;
        final View fallback;
        Slot(ImageView image, View fallback) { this.image = image; this.fallback = fallback; }
    }

    ImgwIconRepository(Context context) {
        directory = new File(context.getFilesDir(), "imgw-icons-v3");
        directory.mkdirs();
    }

    void bind(String code, ImageView image, View fallback) {
        if (code == null || !code.matches("[a-zA-Z0-9]{6,12}")) return;
        Bitmap bitmap = memory.get(code);
        if (bitmap == null) {
            File cached = new File(directory, code + ".png");
            if (cached.isFile()) bitmap = android.graphics.BitmapFactory.decodeFile(cached.getAbsolutePath());
            if (bitmap != null) memory.put(code, bitmap);
        }
        if (bitmap != null) {
            image.setImageBitmap(bitmap);
            image.setVisibility(View.VISIBLE);
            fallback.setVisibility(View.INVISIBLE);
            return;
        }
        List<Slot> slots = pending.get(code);
        if (slots != null) { slots.add(new Slot(image, fallback)); return; }
        slots = new ArrayList<>();
        slots.add(new Slot(image, fallback));
        pending.put(code, slots);
        new Thread(() -> download(code), "imgw-icon-" + code).start();
    }

    private void download(String code) {
        Bitmap result = null;
        HttpURLConnection connection = null;
        try {
            String dayOrNight = code.substring(code.length() - 1);
            if (!"d".equals(dayOrNight) && !"n".equals(dayOrNight)) throw new IllegalArgumentException("Invalid IMGW icon");
            String path = code.substring(0, code.length() - 1);
            connection = (HttpURLConnection) new URL(BASE + dayOrNight + "/" + path + ".svg").openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            if (connection.getResponseCode() != 200 || connection.getContentLength() > 120_000)
                throw new IllegalStateException("IMGW icon unavailable");
            try (InputStream stream = connection.getInputStream()) {
                SVG svg = SVG.getFromInputStream(stream);
                Picture picture = svg.renderToPicture();
                result = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(result);
                canvas.scale(160f / picture.getWidth(), 160f / picture.getHeight());
                canvas.drawPicture(picture);
                int left = 160, top = 160, right = 0, bottom = 0;
                for (int y = 0; y < 160; y++) for (int x = 0; x < 160; x++) {
                    if ((result.getPixel(x, y) >>> 24) < 20) continue;
                    left = Math.min(left, x); top = Math.min(top, y);
                    right = Math.max(right, x); bottom = Math.max(bottom, y);
                }
                if (left < right && top < bottom) {
                    Bitmap trimmed = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
                    float sourceWidth = right - left + 1f, sourceHeight = bottom - top + 1f;
                    float size = 144f / Math.max(sourceWidth, sourceHeight);
                    float targetWidth = sourceWidth * size, targetHeight = sourceHeight * size;
                    new Canvas(trimmed).drawBitmap(result, new Rect(left, top, right + 1, bottom + 1),
                            new RectF((160 - targetWidth) / 2, (160 - targetHeight) / 2,
                                    (160 + targetWidth) / 2, (160 + targetHeight) / 2),
                            new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG));
                    result.recycle();
                    result = trimmed;
                }
            }
            try (FileOutputStream out = new FileOutputStream(new File(directory, code + ".png"))) {
                result.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } catch (Exception ignored) { result = null; }
        finally { if (connection != null) connection.disconnect(); }
        final Bitmap ready = result;
        main.post(() -> {
            List<Slot> slots = pending.remove(code);
            if (ready == null || slots == null) return;
            memory.put(code, ready);
            for (Slot slot : slots) {
                slot.image.setImageBitmap(ready);
                slot.image.setVisibility(View.VISIBLE);
                slot.fallback.setVisibility(View.INVISIBLE);
            }
        });
    }
}
