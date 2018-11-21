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
public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        /* There are two types of messages data messages and notification messages. Data messages
         are handled here in onMessageReceived whether the app is in the foreground or background.
         Data messages are the type traditionally used with GCM. Notification messages are only
         received here in onMessageReceived when the app is in the foreground. When the app is in
          the background an automatically generated notification is displayed. */

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }
}