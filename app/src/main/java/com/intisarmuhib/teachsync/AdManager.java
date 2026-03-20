package com.intisarmuhib.teachsync;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class AdManager {
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_ADS_REMOVED = "ads_removed";

    public static void initAd(Context context, AdView adView) {
        if (adView == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isAdsRemoved = prefs.getBoolean(KEY_ADS_REMOVED, false);

        if (isAdsRemoved) {
            adView.setVisibility(View.GONE);
        } else {
            adView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    public static boolean isAdsRemoved(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ADS_REMOVED, false);
    }
}
