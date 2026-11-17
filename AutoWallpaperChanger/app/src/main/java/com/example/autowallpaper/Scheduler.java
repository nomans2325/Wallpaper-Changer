package com.example.autowallpaper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public final class Scheduler {
    private Scheduler() {}
    static final long[] OPTIONS = {15, 30, 60, 180, 360, 720, 1440};
    static final String[] LABELS = {"15 minutes", "30 minutes", "1 hour", "3 hours", "6 hours", "12 hours", "24 hours"};
    static PendingIntent intent(Context c) {
        Intent i = new Intent(c, ChangeReceiver.class);
        return PendingIntent.getBroadcast(c, 301, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(intent(c));
        if (!WallpaperStore.prefs(c).getBoolean("enabled", false) || WallpaperStore.images(c).isEmpty()) return;
        long min = WallpaperStore.prefs(c).getLong("interval", 60L);
        if (min < 15) min = 15;
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + min * 60_000L, intent(c));
    }
    static void stop(Context c) {
        WallpaperStore.prefs(c).edit().putBoolean("enabled", false).apply();
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(intent(c));
    }
}
