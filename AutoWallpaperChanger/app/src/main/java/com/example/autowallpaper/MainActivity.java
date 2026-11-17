package com.example.autowallpaper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_IMAGES = 101;
    private static final int BLUE = 0xff1768c7;
    private LinearLayout imageList;
    private TextView countText, statusText;
    private Spinner intervalSpinner;
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xff102c4f);
        getWindow().setNavigationBarColor(0xffeff4fa);
        buildUi(); refresh();
    }
    int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
    GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    TextView text(String s, int size, boolean strong) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xff26384d);
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    LinearLayout.LayoutParams margins(int width, int height, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height); p.bottomMargin = dp(bottom); return p;
    }
    Button button(String label, int color) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setTextSize(14); b.setBackground(bg(color, 10)); b.setPadding(dp(12), dp(8), dp(12), dp(8)); return b;
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xfff3f6fb);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18), dp(24), dp(18), dp(24));
        scroll.addView(root); setContentView(scroll);

        TextView heading = text("Auto Wallpaper Changer", 23, true); heading.setTextColor(0xff102c4f);
        root.addView(heading, margins(-1,-2,5));
        TextView sub = text("Your photos. A fresh wallpaper on your schedule.", 13, false);
        root.addView(sub, margins(-1,-2,22));

        Button add = button("+ Add wallpapers", BLUE); root.addView(add, margins(-1,dp(50),12));
        add.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); startActivityForResult(intent, PICK_IMAGES);
        });

        countText = text("0 wallpapers", 16, true); root.addView(countText, margins(-1,-2,12));
        TextView timerTitle = text("Change wallpaper every", 15, true); root.addView(timerTitle, margins(-1,-2,8));
        intervalSpinner = new Spinner(this); intervalSpinner.setPadding(dp(8),dp(8),dp(8),dp(8));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Scheduler.LABELS);
        intervalSpinner.setAdapter(adapter);
        long selected = WallpaperStore.prefs(this).getLong("interval",60L);
        for (int i=0;i<Scheduler.OPTIONS.length;i++) if (Scheduler.OPTIONS[i]==selected) intervalSpinner.setSelection(i);
        root.addView(intervalSpinner, margins(-1,dp(52),12));

        Button start = button("Start automatic changing", BLUE); root.addView(start, margins(-1,dp(50),10));
        start.setOnClickListener(v -> {
            if (WallpaperStore.images(this).isEmpty()) { toast("Add at least one wallpaper first"); return; }
            long interval = Scheduler.OPTIONS[intervalSpinner.getSelectedItemPosition()];
            WallpaperStore.prefs(this).edit().putLong("interval",interval).putBoolean("enabled",true).apply();
            Scheduler.schedule(this); refresh(); toast("Automatic rotation enabled");
        });
        LinearLayout actionRow = new LinearLayout(this); actionRow.setOrientation(LinearLayout.HORIZONTAL);
        Button next = button("Change now", 0xff244e7b);
        Button stop = button("Stop", 0xffb63b3b);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(49),1); p1.rightMargin = dp(8);
        actionRow.addView(next,p1); actionRow.addView(stop,new LinearLayout.LayoutParams(0,dp(49),1));
        root.addView(actionRow, margins(-1,-2,14));
        next.setOnClickListener(v -> {
            next.setEnabled(false);
            executor.execute(() -> {
                String msg;
                try { msg = WallpaperStore.applyNext(this) ? "Wallpaper changed" : "Add wallpapers first"; }
                catch(Exception ex) { msg = "Could not change wallpaper: " + ex.getMessage(); }
                final String message = msg;
                runOnUiThread(() -> { next.setEnabled(true); toast(message); });
            });
        });
        stop.setOnClickListener(v -> { Scheduler.stop(this); refresh(); toast("Automatic rotation stopped"); });
        statusText = text("",13,false); root.addView(statusText,margins(-1,-2,20));
        TextView galleryTitle = text("My wallpapers",17,true); root.addView(galleryTitle,margins(-1,-2,12));
        imageList = new LinearLayout(this); imageList.setOrientation(LinearLayout.VERTICAL);
        root.addView(imageList,margins(-1,-2,18));
        TextView note = text("Home-screen wallpaper only. Changes run approximately at the chosen interval; Android battery restrictions may delay them. Photos remain private on your device.",12,false);
        note.setTextColor(0xff657388); root.addView(note);
    }
    void toast(String msg) { Toast.makeText(this,msg,Toast.LENGTH_LONG).show(); }
    private void refresh() {
        List<File> files = WallpaperStore.images(this);
        countText.setText(files.size() + (files.size()==1 ? " wallpaper" : " wallpapers"));
        boolean running = WallpaperStore.prefs(this).getBoolean("enabled",false);
        statusText.setText(running ? "● Automatic rotation: ON" : "○ Automatic rotation: OFF");
        statusText.setTextColor(running ? 0xff17634e : 0xffa03b3b);
        imageList.removeAllViews();
        if (files.isEmpty()) { imageList.addView(text("No wallpapers added yet.",14,false)); return; }
        int count = 0;
        for (File f : files) {
            if (++count > 200) { imageList.addView(text("Showing first 200 wallpapers",12,false)); break; }
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10),dp(8),dp(10),dp(8)); row.setBackground(bg(Color.WHITE,10));
            ImageView preview = new ImageView(this);
            Bitmap thumb = WallpaperStore.scaledBitmap(f, 160);
            if (thumb != null) preview.setImageBitmap(thumb);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            row.addView(preview,new LinearLayout.LayoutParams(dp(68),dp(68)));
            TextView label = text("Wallpaper " + count,14,true); label.setPadding(dp(12),0,0,0);
            row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            Button remove = button("Remove",0xffb63b3b); remove.setTextSize(12);
            row.addView(remove,new LinearLayout.LayoutParams(dp(90),dp(42)));
            remove.setOnClickListener(v -> {
                WallpaperStore.remove(this,f);
                if (WallpaperStore.images(this).isEmpty()) Scheduler.stop(this);
                else Scheduler.schedule(this);
                refresh();
            });
            imageList.addView(row,margins(-1,dp(84),8));
        }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode != PICK_IMAGES || resultCode != RESULT_OK || data == null) return;
        ArrayList<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            int n = Math.min(100,data.getClipData().getItemCount());
            for (int i=0;i<n;i++) uris.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) uris.add(data.getData());
        toast("Importing selected wallpapers…");
        executor.execute(() -> {
            int imported = 0, failed = 0;
            for (Uri uri : uris) {
                try { if (WallpaperStore.add(this,uri)) imported++; }
                catch (Exception e) { failed++; android.util.Log.w("WallpaperChanger","Image import failed",e); }
            }
            int ok = imported, bad = failed;
            runOnUiThread(() -> { refresh(); toast("Imported " + ok + " wallpapers" + (bad>0 ? "; skipped " + bad : "")); });
        });
    }
    @Override protected void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
