// Notifications as they happen: a light watcher that asks Mandata on the computer every 30 seconds for new ones
// (Notices.fetch), so a new task shows on the phone within half a minute rather than whenever Android runs background
// work (15 minutes at best, much later when the phone is idle). Android shows a silent notification while it runs.
// Off the computer's network it asks every 2 minutes instead. Started when the app opens, after the phone starts, and
// after an update; the 15-minute check (Notices) stays as a fallback.
package org.mandata.phone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public class Watch extends Service {
    static final String CHANNEL = "watch";
    static final long EVERY_MS = 30_000, OFFLINE_MS = 120_000;
    private volatile boolean running;
    private Thread loop;

    /** Start watching (from the app, after the phone starts, after an update): Android allows a start from these. */
    static void start(Context c) {
        if (c.getSharedPreferences("mandata", Context.MODE_PRIVATE).getString("server", null) == null) return;   // not paired yet
        Intent i = new Intent(c, Watch.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
        } catch (Exception e) { /* not allowed now: the 15-minute check still runs */ }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Watching for new tasks", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Notification n = b.setSmallIcon(R.drawable.ic_notify).setContentTitle("Mandata")
            .setContentText("Watching for new tasks from your computer").setOngoing(true).setShowWhen(false)
            .setContentIntent(PendingIntent.getActivity(this, 5, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE)).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
        if (!running) {
            running = true;
            loop = new Thread(() -> {
                while (running) {
                    long wait = EVERY_MS;
                    try { Notices.fetch(getApplicationContext()); } catch (Exception e) { wait = OFFLINE_MS; }
                    try { Thread.sleep(wait); } catch (InterruptedException e) { return; }
                }
            }, "mandata-watch");
            loop.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (loop != null) loop.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) { return null; }

    /** After the phone starts: watch again. */
    public static class Boot extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) { Notices.schedule(c); start(c); }
        }
    }
}
