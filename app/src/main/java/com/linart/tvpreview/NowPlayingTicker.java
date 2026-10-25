package com.linart.tvpreview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.StyleSpan;
import android.view.View;

/** Left-to-right music ticker. Only this small view redraws while it is visible. */
final class NowPlayingTicker extends View {
    private final TextPaint paint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    private final float speed, gap;
    private StaticLayout line;
    private String song = "";
    private long startedAt;
    NowPlayingTicker(Context context, int size, int color) {
        super(context);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(size);
        paint.setColor(color);
        speed = size * 0.8f;
        gap = size * 3;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void setSong(String artist, String title) {
        String performer = artist == null ? "" : artist.trim();
        String label = performer.isEmpty() ? title : performer + " — " + title;
        if (label.equals(song)) return;
        song = label;
        SpannableString text = new SpannableString(label);
        text.setSpan(new StyleSpan(Typeface.BOLD), 0, performer.isEmpty() ? title.length() : performer.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int width = Math.max(1, (int) Math.ceil(Layout.getDesiredWidth(text, paint)) + 2);
        line = StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setIncludePad(false).setMaxLines(1).build();
        restart();
    }
    private void restart() { startedAt = SystemClock.uptimeMillis(); invalidate(); }
    @Override protected void onVisibilityChanged(View changed, int visibility) {
        super.onVisibilityChanged(changed, visibility);
        if (visibility == VISIBLE) restart();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) restart();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (line == null || getWidth() == 0) return;
        float cycle = Math.max(getWidth(), line.getWidth()) + gap;
        long elapsed = Math.max(0, SystemClock.uptimeMillis() - startedAt - 1200);
        float offset = elapsed * speed / 1000f % cycle;
        int save = canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.translate(offset, (getHeight() - line.getHeight()) / 2f);
        line.draw(canvas);
        canvas.translate(-cycle, 0);
        line.draw(canvas);
        canvas.restoreToCount(save);
        if (isShown() && getWindowVisibility() == VISIBLE) postInvalidateDelayed(33);
    }
}
