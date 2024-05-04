package com.satek.soft;

import android.content.Intent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyPushNotificationService extends FirebaseMessagingService {
    public static final String ACTION_BROADCAST = "MyPushNotificationService.action.broadcast";
    public static final String EXTRA_TOKEN = "MyPushNotificationService.extra.token";
    public LocalBroadcastManager localBroadcastManager;
    NotificationManager notificationManager;

    @Override
    public void onCreate() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        String title, message;

        if (notification != null) {
            title = remoteMessage.getNotification().getTitle();
            message = remoteMessage.getNotification().getBody();
        } else {
            title = remoteMessage.getData().get("title");
            message = remoteMessage.getData().get("message");
        }

        String click = remoteMessage.getData().get("click");

        if (message != null && !message.isEmpty()) {
            sendNotification(title, message, click);
        }
    }

    public void onNewToken(@NonNull String token) {
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_TOKEN, token);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void sendNotification(String title, String message, String click) {
        Bitmap icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "default");

        notificationBuilder.setSmallIcon(R.drawable.badge)
                .setLargeIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri soundDefault = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilder.setSound(soundDefault);
            notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (click != null && !click.isEmpty()) {
            intent.putExtra("click", click);
        }

        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 30) {
            flag = PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flag);
        notificationBuilder.setContentIntent(pendingIntent);

        int id = (int)(System.currentTimeMillis() / 1000);
        notificationManager.notify(id, notificationBuilder.build());
    }
}