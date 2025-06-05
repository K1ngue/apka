package com.example.meetingassistant;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.meetingassistant.database.AppDatabase;
import com.example.meetingassistant.database.Meeting;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "meeting_notifications";
    private static final String CHANNEL_NAME = "Powiadomienia o spotkaniach";
    private static final String CHANNEL_DESCRIPTION = "Powiadomienia o nadchodzących spotkaniach";

    @Override
    public void onReceive(Context context, Intent intent) {
        long meetingId = intent.getLongExtra("meeting_id", -1);
        int minutesBefore = intent.getIntExtra("minutes_before", 0);

        if (meetingId != -1) {
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                Meeting meeting = db.meetingDao().getMeetingById(meetingId);

                if (meeting != null) {
                    String title = "Nadchodzące spotkanie";
                    String message = String.format("Za %d minut masz spotkanie z %s %s",
                            minutesBefore,
                            meeting.getName(),
                            meeting.getSurname());

                    showNotification(context, meetingId, title, message);
                }
            }).start();
        }
    }

    private void showNotification(Context context, long meetingId, String title, String message) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        notificationManager.notify((int) meetingId, builder.build());
    }
} 