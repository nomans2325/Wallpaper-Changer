package com.example.autowallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ChangeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PendingResult result = goAsync();
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                if (WallpaperStore.prefs(app).getBoolean("enabled", false)) {
                    WallpaperStore.applyNext(app);
                }
            } catch (Exception e) {
                android.util.Log.e("WallpaperChanger", "Could not change wallpaper", e);
            } finally {
                Scheduler.schedule(app);
                result.finish();
            }
        }, "wallpaper-change").start();
    }
}
