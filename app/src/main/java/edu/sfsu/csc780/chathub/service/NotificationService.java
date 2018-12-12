package edu.sfsu.csc780.chathub.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import edu.sfsu.csc780.chathub.R;
import edu.sfsu.csc780.chathub.ui.MainActivity;

// referred a tutorial online
public class NotificationService extends FirebaseMessagingService {

    private static final String TAG = "NotificationService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        /* There are two types of messages data messages and notification messages. Data messages
        are handled here in onMessageReceived whether the app is in the foreground or background.
        Data messages are the type traditionally used with GCM. Notification messages are only
        received here in onMessageReceived when the app is in the foreground. When the app is in
        the background an automatically generated notification is displayed. */

        String notificationTitle = null, notificationBody = null;

        // check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            notificationTitle = remoteMessage.getNotification().getTitle();
            notificationBody = remoteMessage.getNotification().getBody();
        }

        /* also if you intend on generating your own notifications as a result of a received FCM
        message, here is where that should be initiated. See sendNotification method below */
        sendNotification(notificationTitle, notificationBody);
    }

    // create and show a simple notification containing the received FCM message.
    public void sendNotification(String messageTitle, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("title", messageTitle);
        intent.putExtra("message", messageBody);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }
}