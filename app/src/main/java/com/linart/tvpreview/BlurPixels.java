package com.linart.tvpreview;

/** Separable box blur with clamped edges, matching the original integer rounding. */
final class BlurPixels {
    static void apply(int[] pixels, int[] scratch, int width, int height, int radius, int passes) {
        int count = radius * 2 + 1;
        for (int pass = 0; pass < passes; pass++) {
            for (int y = 0; y < height; y++) {
                int row = y * width, red = 0, green = 0, blue = 0;
                for (int offset = -radius; offset <= radius; offset++) {
                    int color = pixels[row + clamp(offset, width)];
                    red += (color >>> 16) & 255;
                    green += (color >>> 8) & 255;
                    blue += color & 255;
                }
                for (int x = 0; x < width; x++) {
                    scratch[row + x] = rgb(red / count, green / count, blue / count);
                    int removed = pixels[row + clamp(x - radius, width)];
                    int added = pixels[row + clamp(x + radius + 1, width)];
                    red += ((added >>> 16) & 255) - ((removed >>> 16) & 255);
                    green += ((added >>> 8) & 255) - ((removed >>> 8) & 255);
                    blue += (added & 255) - (removed & 255);
                }
            }
            for (int x = 0; x < width; x++) {
                int red = 0, green = 0, blue = 0;
                for (int offset = -radius; offset <= radius; offset++) {
                    int color = scratch[clamp(offset, height) * width + x];
                    red += (color >>> 16) & 255;
                    green += (color >>> 8) & 255;
                    blue += color & 255;
                }
                for (int y = 0; y < height; y++) {
                    pixels[y * width + x] = rgb(red / count, green / count, blue / count);
                    int removed = scratch[clamp(y - radius, height) * width + x];
                    int added = scratch[clamp(y + radius + 1, height) * width + x];
                    red += ((added >>> 16) & 255) - ((removed >>> 16) & 255);
                    green += ((added >>> 8) & 255) - ((removed >>> 8) & 255);
                    blue += (added & 255) - (removed & 255);
                }
            }
        }
    }

    private static int clamp(int value, int size) { return Math.max(0, Math.min(size - 1, value)); }
    private static int rgb(int r, int g, int b) { return 0xff000000 | (r << 16) | (g << 8) | b; }
}
