package com.orcchg.chatclient.util;

import android.content.Context;
import android.util.DisplayMetrics;

import timber.log.Timber;

public class WindowUtility {

    public static void logScreenParams(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        Timber.i("Dots-per-Inch: %s, density: %s", metrics.densityDpi, metrics.density);
    }
}
