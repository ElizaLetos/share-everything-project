package com.example.share_everything_project;

import android.app.Application;
import android.util.Log;

public class ShareEverythingApp extends Application {
    private static final String TAG = "ShareEverythingApp";
    private static ShareEverythingApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "Application onCreate called!");
        // No Firebase initialization needed for Supabase
    }

    public static ShareEverythingApp getInstance() {
        return instance;
    }
} 