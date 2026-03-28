package com.example.eyalproject.ui.cart;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.example.eyalproject.MainActivity;
import com.example.eyalproject.R;

/**
 * BroadcastReceiver responsible for handling system broadcasts (like Alarms)
 * and triggering local notifications, primarily for purchase confirmations.
 */
public class CartReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "PURCHASE_CHANNEL";
    private static final int NOTIFICATION_ID = 2; // Unique ID for the notification type.

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // 1. Get the message and initialize the title
        String message = intent.getStringExtra("message");
        String title = "Notification";

        if (message == null) {
            message = "A notification event occurred.";
        }

        // 2. Determine the correct title based on the action
        if ("PURCHASE_CONFIRMATION".equals(action)) {
            title = "Purchase Confirmation";
        } else if ("NEW_SERVICE_REQUEST".equals(action)) {
            title = intent.getStringExtra("title");
            if (title == null) {
                title = "New Service Request";
            }
        }

        // 3. Show the notification with the determined title
        showNotification(context, title, message);
    }

    private void showNotification(Context context, String title, String message) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_store)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Purchase Notifications";
            String description = "Notifications for purchase confirmations";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}