// Notifications on the phone: what Mandata on the computer would show on its desktop (a new task, the day's deadlines),
// fetched in the background about every 15 minutes (Android's shortest interval for background work), and the
// app's own news (an update ready to install). A tap opens the app where the notification points.
package org.mandata.phone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class Notices extends Worker {
    static final String TASKS = "tasks", APP = "app";

    public Notices(@NonNull Context c, @NonNull WorkerParameters p) { super(c, p); }

    /** The background check, every 15 minutes while the phone has a network; kept across restarts by WorkManager. */
    static void schedule(Context c) {
        PeriodicWorkRequest w = new PeriodicWorkRequest.Builder(Notices.class, 15, TimeUnit.MINUTES)
            .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork("mandata-notices", ExistingPeriodicWorkPolicy.KEEP, w);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        try { fetch(c); } catch (Exception e) { /* the computer is off or on another network: next time */ }
        try { Updater.checkIfDue(c); } catch (Exception e) { /* GitHub not reachable: next time */ }
        return Result.success();
    }

    /** The computer's notifications since the last one shown. The first check only notes where the list stands. */
    // The first look at a computer shows what came in the last 30 minutes, not its whole backlog.
    static final long FIRST_LOOK_MS = 30 * 60 * 1000L;

    /** The computer's notifications since the last one shown. Called by the watcher (Watch.java) and the 15-minute check. */
    static synchronized void fetch(Context c) throws Exception {
        SharedPreferences prefs = c.getSharedPreferences("mandata", Context.MODE_PRIVATE);
        String server = prefs.getString("server", null);
        if (server == null) return;
        String cookie = CookieManager.getInstance().getCookie(server);
        if (cookie == null) return;
        String key = "noticeLast:" + server;   // each computer counts its own
        long last = prefs.getLong(key, -1);
        JSONObject r = new JSONObject(get(server + "api/phone/notices?after=" + Math.max(last, 0), cookie));
        long top = r.optLong("last", 0);
        boolean first = last < 0 || top < last;   // never looked, or the computer's list started again
        if (first) r = new JSONObject(get(server + "api/phone/notices?after=0", cookie));
        JSONArray list = r.optJSONArray("notices");
        long since = System.currentTimeMillis() - FIRST_LOOK_MS;
        for (int i = 0; list != null && i < list.length(); i++) {
            JSONObject n = list.getJSONObject(i);
            if (first && at(n.optString("at")) < since) continue;
            show(c, TASKS, 1000 + (int) (n.optLong("id") % 100000), n.optString("title", "Mandata"), n.optString("body", ""),
                open(c, server + n.optString("route", "#/home")));
        }
        prefs.edit().putLong(key, r.optLong("last", Math.max(last, 0))).apply();
    }

    /** An ISO time (2026-10-14T18:48:50.459Z) in milliseconds; 0 when it cannot be read. */
    static long at(String iso) {
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ROOT);
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return f.parse(iso.substring(0, 19)).getTime();
        } catch (Exception e) { return 0; }
    }

    static String get(String url, String cookie) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(8000); h.setReadTimeout(15000);
        if (cookie != null) h.setRequestProperty("Cookie", cookie);
        try {
            if (h.getResponseCode() != 200) throw new Exception("HTTP " + h.getResponseCode());
            return new String(read(h.getInputStream()), StandardCharsets.UTF_8);
        } finally { h.disconnect(); }
    }

    static byte[] read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[16384];
        for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
        in.close();
        return out.toByteArray();
    }

    /** Opens the app at a page of the computer's phone version. */
    static PendingIntent open(Context c, String url) {
        Intent i = new Intent(c, MainActivity.class).setAction(Intent.ACTION_VIEW).putExtra("url", url)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, url.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void show(Context c, String channel, int id, String title, String body, PendingIntent tap) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(TASKS, "Tasks and deadlines", NotificationManager.IMPORTANCE_DEFAULT));
            nm.createNotificationChannel(new NotificationChannel(APP, "App updates", NotificationManager.IMPORTANCE_LOW));
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, channel) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_notify).setContentTitle(title).setContentText(body)
            .setStyle(new Notification.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(tap)
            .setColor(0xFF15AABF);
        try { nm.notify(id, b.build()); } catch (SecurityException e) { /* notifications not allowed */ }
    }
}
