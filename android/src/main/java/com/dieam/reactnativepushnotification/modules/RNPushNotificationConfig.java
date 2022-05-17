package com.dieam.reactnativepushnotification.modules;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.core.content.res.ResourcesCompat;
import android.os.Bundle;
import android.util.Log;

class RNPushNotificationConfig {
    private static final String KEY_NOTIFICATION_FIREBASE_DEFAULT_CHANNEL_ID = "com.google.firebase.messaging.default_notification_channel_id";
    private static final String KEY_NOTIFICATION_DEFAULT_CHANNEL_ID = "com.dieam.reactnativepushnotification.default_notification_channel_id";
    private static final String KEY_NOTIFICATION_FOREGROUND = "com.dieam.reactnativepushnotification.notification_foreground";
    private static final String KEY_NOTIFICATION_COLOR = "com.dieam.reactnativepushnotification.notification_color";

    private static String FILE_UPLOAD_CHANNEL = "fn_channel_file_upload";
    private static String PHOTO_UPLOAD_CHANNEL = "fn_channel_photo_upload";
    private static String PUSH_NOTIFICATION_CHANNEL = "fn_channel_push_notification";
    private static String AUTO_REQUEST_CHANNEL = "fn_channel_auto_request";
    private static String CHECK_IN_CHANNEL = "fn_channel_check_in";
    private static String PAYMENTS_CHANNEL = "fn_channel_payments";
    private static String TRIP_LOG_CHANNEL = "fn_trip_log";
    private static String OTHER_CHANNEL = "fn_channel_other";

    private static Bundle metadata;
    private Context context;

    public RNPushNotificationConfig(Context context) {
        this.context = context;
        if (metadata == null) {
            try {
                ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                metadata = applicationInfo.metaData;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                Log.e(RNPushNotification.LOG_TAG, "Error reading application meta, falling back to defaults");
                metadata = new Bundle();
            }
        }
    }

    private String getStringValue(String key, String defaultValue) {
        try {
            final String value = metadata.getString(key);

            if (value != null && value.length() > 0) {
                return value;
            }
        } catch (Exception e) {
            Log.w(RNPushNotification.LOG_TAG, "Unable to find " + key + " in manifest. Falling back to default");
        }

        // Default
        return defaultValue;
    }

    public int getNotificationColor() {
        try {
            int resourceId = metadata.getInt(KEY_NOTIFICATION_COLOR);
            return ResourcesCompat.getColor(context.getResources(), resourceId, null);
        } catch (Exception e) {
            Log.w(RNPushNotification.LOG_TAG, "Unable to find " + KEY_NOTIFICATION_COLOR + " in manifest. Falling back to default");
        }
        // Default
        return -1;
    }

    public boolean getNotificationForeground() {
        try {
            return metadata.getBoolean(KEY_NOTIFICATION_FOREGROUND, false);
        } catch (Exception e) {
            Log.w(RNPushNotification.LOG_TAG, "Unable to find " + KEY_NOTIFICATION_FOREGROUND + " in manifest. Falling back to default");
        }
        // Default
        return false;
    }

    public String getNotificationDefaultChannelId(String category) {
        if (category.equals("AUTO_REQUEST"))
            return  AUTO_REQUEST_CHANNEL;
        else if (category.equals("ON_MY_WAY") || category.equals("CHECK_IN"))
            return CHECK_IN_CHANNEL;

        // Default
        return PUSH_NOTIFICATION_CHANNEL;
    }
}
