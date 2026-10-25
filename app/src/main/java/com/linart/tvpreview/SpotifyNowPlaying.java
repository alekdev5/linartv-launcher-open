package com.linart.tvpreview;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

/** Event-driven: no polling, web requests, Spotify login or credentials. */
final class SpotifyNowPlaying {
    interface Listener { void changed(String title, String artist, Bitmap cover, boolean playing); }
    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<MediaController> controllers = new ArrayList<>();
    private MediaSessionManager manager;
    private boolean registered;
    private boolean started;
    private final Runnable accessReady = () -> { if (started) start(); };
    private final MediaSessionManager.OnActiveSessionsChangedListener sessions = this::bind;
    private final MediaController.Callback updates = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata metadata) { publish(); }
        @Override public void onPlaybackStateChanged(PlaybackState state) { publish(); }
        @Override public void onSessionDestroyed() { refresh(); }
    };
    SpotifyNowPlaying(Context context, Listener listener) {
        this.context = context.getApplicationContext(); this.listener = listener;
    }
    void start() {
        stop();
        started = true;
        SpotifySessionListener.observe(accessReady);
        manager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (manager == null) return;
        ComponentName component = new ComponentName(context, SpotifySessionListener.class);
        try {
            manager.addOnActiveSessionsChangedListener(sessions, component, main);
            registered = true;
            refresh();
        } catch (SecurityException denied) {
            android.util.Log.w("LinarTVSpotify", "Media session access not ready", denied);
            listener.changed(null, null, null, false);
        }
    }
    private void refresh() {
        try { bind(manager.getActiveSessions(new ComponentName(context, SpotifySessionListener.class))); }
        catch (SecurityException denied) { bind(new ArrayList<>()); }
    }
    private void bind(List<MediaController> active) {
        for (MediaController c : controllers) c.unregisterCallback(updates);
        controllers.clear();
        if (active != null) for (MediaController c : active) {
            if (!"com.spotify.tv.android".equals(c.getPackageName())
                    && !"com.spotify.music".equals(c.getPackageName())) continue;
            controllers.add(c);
            c.registerCallback(updates, main);
        }
        publish();
        android.util.Log.d("LinarTVSpotify", "Spotify sessions=" + controllers.size());
    }
    private void publish() {
        MediaController selected = null;
        for (MediaController c : controllers) {
            PlaybackState state = c.getPlaybackState();
            if (state == null || state.getState() == PlaybackState.STATE_STOPPED
                    || state.getState() == PlaybackState.STATE_NONE
                    || state.getState() == PlaybackState.STATE_ERROR) continue;
            selected = c;
            if (state.getState() == PlaybackState.STATE_PLAYING) break;
        }
        MediaMetadata data = selected == null ? null : selected.getMetadata();
        String title = data == null ? null : data.getString(MediaMetadata.METADATA_KEY_TITLE);
        android.util.Log.d("LinarTVSpotify", "Metadata available=" + (data != null) + ", title available=" + (title != null));
        if (title == null || title.trim().isEmpty()) { listener.changed(null, null, null, false); return; }
        Bitmap cover = data.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (cover == null) cover = data.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (cover == null) cover = data.getBitmap(MediaMetadata.METADATA_KEY_ART);
        listener.changed(title, data.getString(MediaMetadata.METADATA_KEY_ARTIST), cover,
                selected.getPlaybackState() != null && selected.getPlaybackState().getState() == PlaybackState.STATE_PLAYING);
    }
    void stop() {
        started = false;
        SpotifySessionListener.forget(accessReady);
        if (registered && manager != null) manager.removeOnActiveSessionsChangedListener(sessions);
        registered = false;
        for (MediaController c : controllers) c.unregisterCallback(updates);
        controllers.clear();
    }
}
