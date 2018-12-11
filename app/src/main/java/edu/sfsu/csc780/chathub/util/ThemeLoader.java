package edu.sfsu.csc780.chathub.util;

import android.content.Context;
import android.support.v7.app.AppCompatDelegate;
import android.widget.Toast;

// change mode (day/night) -- referred YouTube tutorials
public class ThemeLoader {
    public static void changeMode(Context context) {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_AUTO || AppCompatDelegate.getDefaultNightMode() == -1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(context, "Changed to Night Mode", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(context, "Changed to Day Mode", Toast.LENGTH_SHORT).show();
        }
    }
}
