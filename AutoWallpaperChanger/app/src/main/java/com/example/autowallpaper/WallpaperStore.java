package com.example.autowallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class WallpaperStore {
    private WallpaperStore() {}
    private static final String PREFS = "wallpaper_settings";
    private static final Object LOCK = new Object();
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    static File dir(Context c) { File d = new File(c.getFilesDir(), "wallpapers"); if (!d.exists()) d.mkdirs(); return d; }
    static List<File> images(Context c) {
        File[] files = dir(c).listFiles(f -> f.isFile() && f.getName().endsWith(".image"));
        if (files == null) return new ArrayList<>();
        Arrays.sort(files, Comparator.comparing(File::getName));
        return new ArrayList<>(Arrays.asList(files));
    }
    static boolean add(Context c, android.net.Uri uri) throws IOException {
        File target = new File(dir(c), System.currentTimeMillis() + "_" + UUID.randomUUID() + ".image");
        File temp = new File(dir(c), UUID.randomUUID() + ".partial");
        long bytes = 0;
        try (InputStream in = c.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(temp)) {
            if (in == null) throw new IOException("Could not open selected photo");
            byte[] buf = new byte[16384]; int n;
            while ((n = in.read(buf)) != -1) {
                bytes += n;
                if (bytes > 30L * 1024 * 1024) throw new IOException("Photo exceeds 30 MB");
                out.write(buf, 0, n);
            }
        } catch (Exception e) { temp.delete(); throw e; }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(temp.getPath(), opts);
        if (opts.outWidth <= 0 || opts.outHeight <= 0 || bytes == 0) {
            temp.delete(); throw new IOException("Unsupported image");
        }
        if (!temp.renameTo(target)) { temp.delete(); throw new IOException("Could not save photo"); }
        return true;
    }
    static void remove(Context c, File file) {
        synchronized (LOCK) { if (file.getParentFile().equals(dir(c))) file.delete(); prefs(c).edit().putInt("next", 0).apply(); }
    }
    static Bitmap scaledBitmap(File file, int maxDimension) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true; BitmapFactory.decodeFile(file.getPath(), o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return null;
        int sample = 1;
        while (Math.max(o.outWidth / sample, o.outHeight / sample) > maxDimension) sample *= 2;
        o = new BitmapFactory.Options(); o.inSampleSize = sample;
        return BitmapFactory.decodeFile(file.getPath(), o);
    }
    static boolean applyNext(Context c) throws IOException {
        synchronized (LOCK) {
            List<File> list = images(c); if (list.isEmpty()) return false;
            int index = Math.floorMod(prefs(c).getInt("next", 0), list.size());
            Bitmap image = scaledBitmap(list.get(index), 2160);
            if (image == null) throw new IOException("Selected wallpaper cannot be decoded");
            try {
                WallpaperManager wm = WallpaperManager.getInstance(c);
                wm.setBitmap(image, null, true, WallpaperManager.FLAG_SYSTEM);
            } finally { image.recycle(); }
            prefs(c).edit().putInt("next", (index + 1) % list.size()).apply();
            return true;
        }
    }
}
