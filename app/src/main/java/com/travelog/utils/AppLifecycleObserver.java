package com.travelog.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class AppLifecycleObserver implements DefaultLifecycleObserver {

    private final Context context;

    private static final String TAG = "AppLifecycleObserver";

    public AppLifecycleObserver(Context context) {
        // Use application context to avoid memory leaks
        this.context = context.getApplicationContext();
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        // App moved to the FOREGROUND
        Log.d(TAG, "App is in FOREGROUND");

        // Stop your notification service
        Intent serviceIntent = new Intent(context, PostsNotificationService.class);
        context.stopService(serviceIntent);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // App moved to the BACKGROUND
        Log.d(TAG, "App is in BACKGROUND");

        // Start your notification service
        Intent serviceIntent = new Intent(context, PostsNotificationService.class);

        // Handle background service restrictions on Android 8 (Oreo) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}

