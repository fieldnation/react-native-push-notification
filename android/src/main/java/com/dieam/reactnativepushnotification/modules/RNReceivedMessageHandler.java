package com.dieam.reactnativepushnotification.modules;

import static com.dieam.reactnativepushnotification.modules.RNPushNotification.LOG_TAG;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Map;

public class RNReceivedMessageHandler {
    private FirebaseMessagingService mFirebaseMessagingService;

    public RNReceivedMessageHandler(@NonNull FirebaseMessagingService service) {
        this.mFirebaseMessagingService = service;
    }

    public void handleReceivedMessage(RemoteMessage message) {
        String from = message.getFrom();
        RemoteMessage.Notification remoteNotification = message.getNotification();
        final Bundle bundle = new Bundle();
        // Putting it from remoteNotification first so it can be overriden if message
        // data has it
        if (remoteNotification != null) {
            // ^ It's null when message is from GCM
            RNPushNotificationConfig config = new RNPushNotificationConfig(mFirebaseMessagingService.getApplication());

            String title = getLocalizedString(remoteNotification.getTitle(), remoteNotification.getTitleLocalizationKey(), remoteNotification.getTitleLocalizationArgs());
            String body = getLocalizedString(remoteNotification.getBody(), remoteNotification.getBodyLocalizationKey(), remoteNotification.getBodyLocalizationArgs());

            bundle.putString("title", title);
            bundle.putString("message", body);
            bundle.putString("sound", remoteNotification.getSound());
            bundle.putString("color", remoteNotification.getColor());
            bundle.putString("tag", remoteNotification.getTag());

            if(remoteNotification.getIcon() != null) {
                bundle.putString("smallIcon", remoteNotification.getIcon());
            } else {
                bundle.putString("smallIcon", "ic_notification");
            }

            if(remoteNotification.getChannelId() != null) {
                bundle.putString("channelId", remoteNotification.getChannelId());
            }
            else {
                bundle.putString("channelId", config.getNotificationDefaultChannelId(""));
            }

            Integer visibilty = remoteNotification.getVisibility();
            String visibilityString = "private";

            if (visibilty != null) {
                switch (visibilty) {
                    case NotificationCompat.VISIBILITY_PUBLIC:
                        visibilityString = "public";
                        break;
                    case NotificationCompat.VISIBILITY_SECRET:
                        visibilityString = "secret";
                        break;
                }
            }

            bundle.putString("visibility", visibilityString);

            Integer priority = remoteNotification.getNotificationPriority();
            String priorityString = "high";

            if (priority != null) {
                switch (priority) {
                    case NotificationCompat.PRIORITY_MAX:
                        priorityString = "max";
                        break;
                    case NotificationCompat.PRIORITY_LOW:
                        priorityString = "low";
                        break;
                    case NotificationCompat.PRIORITY_MIN:
                        priorityString = "min";
                        break;
                    case NotificationCompat.PRIORITY_DEFAULT:
                        priorityString = "default";
                        break;
                }
            }

            bundle.putString("priority", priorityString);

            Uri uri = remoteNotification.getImageUrl();

            if(uri != null) {
                String imageUrl = uri.toString();

                bundle.putString("bigPictureUrl", imageUrl);
                bundle.putString("largeIconUrl", imageUrl);
            }
        }

        Bundle dataBundle = new Bundle();
        Map<String, String> notificationData = message.getData();

        for(Map.Entry<String, String> entry : notificationData.entrySet()) {
            dataBundle.putString(entry.getKey(), entry.getValue());
        }

        bundle.putParcelable("data", dataBundle);

        if (remoteNotification == null && notificationData != null) {
            try {
                String cloudMessage = notificationData.get("message").toString();
                JSONObject jsonObject = new JSONObject(cloudMessage);
                bundle.putString("title",  jsonObject.getString("title"));
                bundle.putString("message",  jsonObject.getString("body"));
                bundle.putString("smallIcon", "ic_notif_logo");
                bundle.putString("category", jsonObject.getString("category"));

                JSONArray actions = jsonObject.getJSONArray("actions");
                if (actions != null) {
                    bundle.putString("actions", actions.toString());
                }
            } catch (Exception err){
                err.printStackTrace();
            }
        }

        if (bundle.getString("id") == null) {
            SecureRandom randomNumberGenerator = new SecureRandom();
            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
        }
        RNPushNotificationHelper pushNotificationHelper = new RNPushNotificationHelper(mFirebaseMessagingService.getApplication());
        boolean isForeground = pushNotificationHelper.isApplicationInForeground();
        bundle.putBoolean("foreground", isForeground);
        bundle.putBoolean("userInteraction", false);
        pushNotificationHelper.sendToNotificationCentre(bundle);

        // We need to run this on the main thread, as the React code assumes that is true.
        // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
        // "Can't create handler inside thread that has not called Looper.prepare()"
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                // Construct and load our normal React JS code bundle
                final ReactInstanceManager mReactInstanceManager = ((ReactApplication) mFirebaseMessagingService.getApplication()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();
                // If it's constructed, send a notificationre
                if (context != null) {
                    // handleRemotePushNotification((ReactApplicationContext) context, bundle);
                    RNPushNotificationJsDelivery jsDelivery = new RNPushNotificationJsDelivery(context);
                    jsDelivery.notifyDelivered(bundle);
                } else {
                    // Otherwise wait for construction, then send the notification
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            // handleRemotePushNotification((ReactApplicationContext) context, bundle);
                            SharedPreferences sharedPreferences = context.getSharedPreferences(RNPushNotificationHelper.PREFERENCES_KEY, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("pending_remote_notification", bundle.getString("category", ""));
                            editor.apply();
                            mReactInstanceManager.removeReactInstanceEventListener(this);
                        }
                    });
                    // if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                    //     // Construct it in the background
                    //     mReactInstanceManager.createReactContextInBackground();
                    // }
                }
            }
        });
    }

    private void handleRemotePushNotification(ReactApplicationContext context, Bundle bundle) {

        // If notification ID is not provided by the user for push notification, generate one at random
        if (bundle.getString("id") == null) {
            SecureRandom randomNumberGenerator = new SecureRandom();
            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
        }

        Application applicationContext = (Application) context.getApplicationContext();

        RNPushNotificationConfig config = new RNPushNotificationConfig(mFirebaseMessagingService.getApplication());
        RNPushNotificationHelper pushNotificationHelper = new RNPushNotificationHelper(applicationContext);

        boolean isForeground = pushNotificationHelper.isApplicationInForeground();

        RNPushNotificationJsDelivery jsDelivery = new RNPushNotificationJsDelivery(context);
        bundle.putBoolean("foreground", isForeground);
        bundle.putBoolean("userInteraction", false);
        jsDelivery.notifyNotification(bundle);
/*
        // If contentAvailable is set to true, then send out a remote fetch event
        if (bundle.getString("contentAvailable", "false").equalsIgnoreCase("true")) {
            jsDelivery.notifyRemoteFetch(bundle);
        }

        if (config.getNotificationForeground() || !isForeground) {
            Log.v(LOG_TAG, "sendNotification: " + bundle);

            pushNotificationHelper.sendToNotificationCentre(bundle);
        }
*/
    }

    private String getLocalizedString(String text, String locKey, String[] locArgs) {
        if(text != null) {
            return text;
        }

        Context context = mFirebaseMessagingService.getApplicationContext();
        String packageName = context.getPackageName();

        String result = null;

        if (locKey != null) {
            int id = context.getResources().getIdentifier(locKey, "string", packageName);
            if (id != 0) {
                if (locArgs != null) {
                    result = context.getResources().getString(id, (Object[]) locArgs);
                } else {
                    result = context.getResources().getString(id);
                }
            }
        }

        return result;
    }
}
