package edu.sfsu.csc780.chathub.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Objects;

public class NotificationActivity extends Activity {

    public static final String NOTIFICATION_ACTIVITY = "NotificationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Objects.requireNonNull(manager).cancel(getIntent().getIntExtra(NOTIFICATION_ACTIVITY, -1));
        finish();
    }
}