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
 *
 * This receiver is configured to launch the MainActivity when clicked. Since the
 * Intent does not contain a USERNAME extra, the MainActivity (if updated correctly)
 * will redirect the user to the WelcomeActivity/Login screen.
 */
public class CartReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "PURCHASE_CHANNEL";
    private static final int NOTIFICATION_ID = 2; // Unique ID for the notification type.

    /**
     * Entry point when the broadcast is received (e.g., when the scheduled alarm triggers).
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received, which should contain the action and message.
     */
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
            // Logic for a successful purchase confirmation.
            title = "Purchase Confirmation";
        } else if ("NEW_SERVICE_REQUEST".equals(action)) {
            // Logic for an admin-related service request notification.
            title = intent.getStringExtra("title");
            if (title == null) {
                // Fallback title for safety
                title = "New Service Request";
            }
        }

        // 3. Show the notification with the determined title
        showNotification(context, title, message);
    }

    /**
     * Creates and displays the notification using NotificationCompat.Builder.
     * @param context The application context.
     * @param title The title of the notification.
     * @param message The content text of the notification.
     */
    private void showNotification(Context context, String title, String message) {
        createNotificationChannel(context);

        // Intent to be launched when the user clicks the notification.
        Intent intent = new Intent(context, MainActivity.class);
        // Flags ensure a clean launch of MainActivity, removing prior activity stack.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // NOTE: No 'USERNAME' extra is added here, forcing MainActivity to detect
        // a missing session and redirect to WelcomeActivity/Login.

        // PendingIntent wraps the intent to allow the system (NotificationManager) to fire it later.
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0, // Request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // Use IMMUTABLE flag for modern Android versions
        );

        // Build the notification appearance and behavior
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_store) // Set the notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Use BigTextStyle for full message visibility
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Set high priority for urgency
                .setContentIntent(pendingIntent) // Set the click action
                .setAutoCancel(true); // Notification disappears when clicked

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build()); // Display the notification
    }

    /**
     * Creates the notification channel required for Android O (API 26) and above.
     * This ensures the notification can be displayed.
     * @param context The application context.
     */
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