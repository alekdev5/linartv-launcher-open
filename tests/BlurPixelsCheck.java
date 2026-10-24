package com.linart.tvpreview;

import java.util.Arrays;
import java.util.Random;

/** Compare every output pixel against the previous blur, including tiny edge cases. */
public final class BlurPixelsCheck {
    public static void main(String[] args) {
        Random random = new Random(1234);
        int cases = 0;
        for (int[] dimensions : new int[][]{{1,1},{1,17},{31,1},{7,9},{96,54},{480,270}})
            for (int radius : new int[]{0,2,4}) for (int passes : new int[]{1,2}) {
                int w = dimensions[0], h = dimensions[1];
                int[] source = new int[w * h];
                for (int i = 0; i < source.length; i++) source[i] = random.nextInt();
                int[] expected = source.clone(), actual = source.clone();
                original(expected, w, h, radius, passes);
                BlurPixels.apply(actual, new int[actual.length], w, h, radius, passes);
                if (!Arrays.equals(expected, actual)) throw new AssertionError(
                        "Blur differs at " + w + "x" + h + " radius=" + radius + " passes=" + passes);
                cases++;
            }
        System.out.println("OK: " + cases + " blur cases; every pixel matches the previous algorithm");
    }

    private static void original(int[] pixels, int width, int height, int radius, int passes) {
        int[] temp = new int[pixels.length];
        for (int pass = 0; pass < passes; pass++) {
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0, n = 0;
                for (int dx = -radius; dx <= radius; dx++) {
                    int c = pixels[y * width + Math.max(0, Math.min(width - 1, x + dx))];
                    r += (c >>> 16) & 255; g += (c >>> 8) & 255; b += c & 255; n++;
                }
                temp[y * width + x] = 0xff000000 | ((r / n) << 16) | ((g / n) << 8) | b / n;
            }
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0, n = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    int c = temp[Math.max(0, Math.min(height - 1, y + dy)) * width + x];
                    r += (c >>> 16) & 255; g += (c >>> 8) & 255; b += c & 255; n++;
                }
                pixels[y * width + x] = 0xff000000 | ((r / n) << 16) | ((g / n) << 8) | b / n;
            }
        }
    }
}
