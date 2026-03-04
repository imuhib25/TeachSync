package com.intisarmuhib.teachsync;

import android.Manifest;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class ClassReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // Check if sound is enabled in App Preferences
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);

        // Runtime permission check required for Android 13+ (API 33)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w("ClassReminder", "POST_NOTIFICATIONS not granted — skipping.");
            return;
        }

        // Read dynamic content from the scheduled intent
        String title   = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        int notifId    = intent.getIntExtra("notificationId",
                (int) (System.currentTimeMillis() / 1000));

        if (title   == null || title.isEmpty())   title   = "Class Reminder";
        if (message == null || message.isEmpty()) message = "A class is starting soon!";

        // Define a clear, medium-loud sound
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "class_channel")
                        .setSmallIcon(R.drawable.outline_notifications_24)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setAutoCancel(true);

        if (soundEnabled) {
            builder.setSound(soundUri);
            // Default vibration for physical feedback
            builder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        }

        NotificationManagerCompat.from(context).notify(notifId, builder.build());
    }
}
