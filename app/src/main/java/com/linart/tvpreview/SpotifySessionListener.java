package com.linart.tvpreview;

/** Enables Android's media-session access. Notification contents are never read or stored. */
public final class SpotifySessionListener extends android.service.notification.NotificationListenerService {
    private static final java.util.Set<Runnable> connected = new java.util.HashSet<>();
    static void observe(Runnable callback) { connected.add(callback); }
    static void forget(Runnable callback) { connected.remove(callback); }
    @Override public void onListenerConnected() {
        super.onListenerConnected();
        for (Runnable callback : new java.util.ArrayList<>(connected)) callback.run();
    }
}
