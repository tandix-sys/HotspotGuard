package com.tandix.hotspotguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class HotspotMonitorService extends Service {

    private static final String TAG = "HotspotMonitorService";
    private static final String CHANNEL_ID = "HotspotGuardChannel";
    private static final int NOTIFICATION_ID = 1;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private HotspotController hotspotController;

    private boolean isHotspotOn = false; // Track hotspot state

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification("Monitoring network..."));

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        hotspotController = new HotspotController(this);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                checkNetworkAndControlHotspot(networkCapabilities);
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                // Handle network loss, e.g., assume 4G/3G if no 5G
                Log.d(TAG, "Network lost. Checking capabilities...");
                checkNetworkAndControlHotspot(null); // Pass null to indicate network loss
            }
        };

        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        // Ensure hotspot is off when service stops, or based on user preference
        if (isHotspotOn) {
            hotspotController.setHotspotEnabled(false);
            isHotspotOn = false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Hotspot Guard Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Hotspot Guard")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Placeholder icon
                .setContentIntent(pendingIntent)
                .build();
    }

    private void checkNetworkAndControlHotspot(NetworkCapabilities networkCapabilities) {
        boolean is5G = false;
        if (networkCapabilities != null) {
            is5G = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                   networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED); // This is a simplified check for 5G, actual 5G capability might be more complex or require API 31+ specific checks.
            // For more accurate 5G detection, one might need to check TelephonyManager.getDataNetworkType() on API 30+ and handle specific constants.
            // For simplicity and given minSdk 31, we'll use this as a proxy for now.
        }

        Log.d(TAG, "Current network is 5G: " + is5G + ", Hotspot is on: " + isHotspotOn);

        if (is5G) {
            if (!isHotspotOn) {
                Log.d(TAG, "5G detected, turning hotspot ON");
                hotspotController.setHotspotEnabled(true);
                isHotspotOn = true;
                updateNotification("5G detected, Hotspot ON");
            }
        } else {
            if (isHotspotOn) {
                Log.d(TAG, "Not 5G, turning hotspot OFF");
                hotspotController.setHotspotEnabled(false);
                isHotspotOn = false;
                updateNotification("Not 5G, Hotspot OFF");
            }
        }
    }

    private void updateNotification(String contentText) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, getNotification(contentText));
        }
    }
}
