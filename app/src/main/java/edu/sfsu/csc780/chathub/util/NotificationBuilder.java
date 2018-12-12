package edu.sfsu.csc780.chathub.util;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;

import edu.sfsu.csc780.chathub.R;
import edu.sfsu.csc780.chathub.ui.MainActivity;

public class NotificationBuilder {
    public static void showNotification(Context context, String param) {
        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(param);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // adds the back stack for the Intent
        stackBuilder.addParentStack(MainActivity.class);

        //Create an Intent to get the reply_action
        Intent replyIntent = new Intent(context, MainActivity.class);

        PendingIntent replyPendingIntent = PendingIntent.getActivity(context, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        replyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        stackBuilder.addNextIntent(replyIntent);

        Notification notification = new NotificationCompat.Builder(context)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true)
                .setContentTitle("New Message Received!").build();

        replyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notification.contentIntent = replyPendingIntent;

        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);

        mNotificationManager.notify(0, notification);
    }

}
