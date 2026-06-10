package com.tandix.hotspotguard;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;

import dev.rikka.shizuku.ShizukuBinderWrapper;
import dev.rikka.shizuku.ShizukuService;
import dev.rikka.shizuku.SystemServiceHelper;

public class HotspotController {

    private static final String TAG = "HotspotController";
    private Context context;

    public HotspotController(Context context) {
        this.context = context;
    }

    public void setHotspotEnabled(boolean enable) {
        if (ShizukuService.pingBinder()) {
            Log.d(TAG, "Shizuku is running, attempting to control hotspot.");
            try {
                // Get WifiManager
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                // Use reflection to get the hidden setWifiApEnabled method
                // This method is usually @SystemApi or hidden, so direct access is not allowed.
                // Shizuku allows us to bypass these restrictions.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // For Android 11 (R) and above, the method signature might change or be more restricted.
                    // This part might need adjustment based on actual Shizuku capabilities and Android version.
                    // As a placeholder, we'll try to use the older method signature first.
                    // A more robust solution would involve checking the exact API for Shizuku's WifiManager interaction.
                    Log.w(TAG, "Hotspot control on Android R+ might require different Shizuku API usage.");
                    // Attempting to use the older method for now, may fail on newer Android versions.
                    Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiManager.WifiConfiguration.class, boolean.class);
                    method.invoke(wifiManager, null, enable);
                } else {
                    Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiManager.WifiConfiguration.class, boolean.class);
                    method.invoke(wifiManager, null, enable);
                }
                Log.d(TAG, "Hotspot set to: " + enable);
            } catch (Exception e) {
                Log.e(TAG, "Failed to control hotspot with Shizuku: " + e.getMessage(), e);
            }
        } else {
            Log.e(TAG, "Shizuku is not running or not authorized.");
            // Fallback or inform user that Shizuku is required
        }
    }
}
