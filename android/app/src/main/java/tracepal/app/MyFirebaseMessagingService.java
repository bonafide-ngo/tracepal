/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tracepal.app;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;

/**
 * NOTE: There can only be one service in each app that receives FCM messages. If multiple
 * are declared in the Manifest then the first one will be chosen.
 * <p>
 * In order to make this Java sample functional, you must remove the following from the Kotlin messaging
 * service in the AndroidManifest.xml:
 * <p>
 * <intent-filter>
 * <action android:name="com.google.firebase.MESSAGING_EVENT" />
 * </intent-filter>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "Firebase";

    private FusedLocationProviderClient fusedLocationClient;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        //Log.d(TAG, "From Firebase Unique ID: " + remoteMessage.getFrom());

        // Get message data
        Map<String, String> data = remoteMessage.getData();

        // Check if message contains a data payload.
        if (data.size() > 0) {
            //Log.d(TAG, "Data: " + data);

            if (/* Check if data needs to be processed by long running job */ false) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob();
            } else {
                // Handle message now, within 10 seconds
                handleNow(remoteMessage);
            }
        }

        if (remoteMessage.getNotification() != null) {
            // Check if message contains a notification payload.
            //Log.d(TAG, "Title: " + remoteMessage.getNotification().getTitle());
            //Log.d(TAG, "Body: " + remoteMessage.getNotification().getBody());

            // N.B. Let the tray handling the notification to avoid overlapping the in-app toast
            //sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody(), remoteMessage.getData());
        }
    }

    /**
     * There are two scenarios when onNewToken is called:
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed
     * Under #2, there are three scenarios when the existing token is changed:
     * A) App is restored to a new device
     * B) User uninstalls/reinstalls the app
     * C) User clears app data
     */
    @Override
    public void onNewToken(String token) {
        // Not required, a token is always re-synced on app load
    }

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
        // Not required, all activities are in real-time, handled now.
    }

    /**
     * Handle message now
     *
     * @param remoteMessage
     */
    private void handleNow(RemoteMessage remoteMessage) {
        //Log.d(TAG, "HandleNow");

        // Get message data
        Map<String, String> data = remoteMessage.getData();
        // Get notification
        RemoteMessage.Notification notification = remoteMessage.getNotification();

        // Check if the app is on (foreground/background) of off (killed)
        // Get Activity instance to find out :-)
        ActivityWebView vActivityWebView = ActivityWebView.getInstance();

        if (vActivityWebView == null) {
            // App is killed if no ActivityWebView exists... at least yet
            if (data.get("type").equals("webRtc"))
                // Send a call notification
                callNotification(data);
        } else {
            // App is running, either in background or foreground
            try {
                // Get WebView
                WebView vWebView = (WebView) vActivityWebView.findViewById(R.id.webview);
                // Call async JS script
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        JSONObject jParams = new JSONObject(data);
                        String jScript = String.format(ActivityWebView.JS_firebaseHandler, jParams.toString());
                        //Log.d(TAG, jScript);
                        vWebView.evaluateJavascript(jScript, null);
                    }
                });
            } catch (Exception e) {
                // Something wrong, cannot get the webview
                // Do nothing, exit silently
                return;
            }

            // Check Activity is in foreground
            Boolean vIsForeground = vActivityWebView.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);

            // App is in background
            if (!vIsForeground && data.get("type").equals("webRtc"))
                // Send a call notification
                callNotification(data);
        }
    }

    /**
     * Open the incoming call directly, without notification
     * N.B. Deprecated, use callNotification() instead
     *
     * @param data
     */
    @Deprecated
    private void openCall(Map<String, String> data) {
        Intent intent = new Intent(this, ActivityWebView.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Pass the Data to the Intent from the notification
        if (data != null) {
            for (String key : data.keySet()) {
                String value = data.get(key);
                intent.putExtra(key, value);
                //Log.d(TAG, "Data, Key: " + key + " Value: " + value);
            }
        }

        startActivity(intent);
    }

    /**
     * Create and show a simple call notification containing the received FCM.
     *
     * @param data
     */
    private void callNotification(Map<String, String> data) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            String test = notification.getNotification().getChannelId();
            if (notification.getNotification().getChannelId().equals(ActivityWebView.CHANNEL_CALL_ID)) {
                // Do not stack call notifications, one per time only
                // User will get a missed call notification later on
                return;
            }
        }

        Bitmap avatar = null;
        try {
            InputStream in = new URL(data.get("avatartUrl")).openStream();
            avatar = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            // Silent error
            // Do nothing and simply avoid adding large icon later on
        }

        // Check the screen lock status
        KeyguardManager myKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        boolean isLocked = myKeyguardManager.isKeyguardLocked();

        // Generate random notification ID
        Random rand = new Random();
        int upperbound = 999999999;
        int notificationId = rand.nextInt(upperbound);

        // Intent to answer
        Intent intentAnswer = new Intent(this, ActivityWebView.class);
        intentAnswer.setAction(Intent.ACTION_MAIN);
        intentAnswer.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Pass the Data to the Intent from the notification
        if (data != null) {
            for (String key : data.keySet()) {
                String value = data.get(key);
                intentAnswer.putExtra(key, value);
                //Log.d(TAG, "Data, Key: " + key + " Value: " + value);
            }
            int test = Build.VERSION.SDK_INT;
            // Add answer fallthrough if not locked
            intentAnswer.putExtra("isAnswer", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? (isLocked ? 0 : 1) : 0);
        }

        // Pending intent to answer
        // https://developer.android.com/guide/components/intents-filters#DeclareMutabilityPendingIntent
        PendingIntent pendingIntentAnswer = PendingIntent.getActivity(this, notificationId, intentAnswer, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Avail of new incoming call style
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Intent to decline
            Intent intentDecline = new Intent(this, MyBroadcastReceiver.class);
            intentDecline.putExtra(MyBroadcastReceiver.ACTION, MyBroadcastReceiver.DECLINE_CALL);
            intentDecline.putExtra(MyBroadcastReceiver.NOTIFICATION_ID, notificationId);

            // Pending intent to decline
            PendingIntent pendingIntentDecline = PendingIntent.getBroadcast(this, notificationId, intentDecline, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

            Person.Builder person = new Person.Builder().setName(data.get("title"));
            Notification.Builder notificationBuilder31 = new Notification.Builder(this, ActivityWebView.CHANNEL_CALL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(ContextCompat.getColor(this, R.color.bs_primary))
                    .setContentTitle(data.get("title"))
                    .setContentText(data.get("body"))
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setTimeoutAfter(Integer.parseInt(getResources().getString(R.string.app_webrtc_ring)))
                    .setOnlyAlertOnce(true)
                    .setFullScreenIntent(pendingIntentAnswer, true)
                    .setStyle(Notification.CallStyle.forIncomingCall(person.build(), pendingIntentDecline, pendingIntentAnswer));

            // Add avatar
            if (avatar != null)
                notificationBuilder31.setLargeIcon(avatar);

            // Fire notification
            notificationManager.notify(notificationId, notificationBuilder31.build());
        } else {
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, ActivityWebView.CHANNEL_CALL_ID)
                            .setSmallIcon(R.drawable.ic_stat_name)
                            .setColor(ContextCompat.getColor(this, R.color.bs_primary))
                            .setContentTitle(data.get("title"))
                            .setContentText(data.get("body"))
                            .setAutoCancel(true)
                            .setCategory(NotificationCompat.CATEGORY_CALL)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setTimeoutAfter(Integer.parseInt(getResources().getString(R.string.app_webrtc_ring)))
                            .setOnlyAlertOnce(true)
                            .setFullScreenIntent(pendingIntentAnswer, true);

            // Add avatar
            if (avatar != null)
                notificationBuilder.setLargeIcon(avatar);

            // Build notification
            Notification notification = notificationBuilder.build();
            // Set flags after build for looping sound
            notification.flags = Notification.FLAG_INSISTENT;

            // Fire notification
            notificationManager.notify(notificationId, notification);
        }
    }

    /**
     * Create and show a simple message notification containing the received FCM.
     * N.B. Deprecated, let Firebase handling message notifications when the app is killed or in background
     *
     * @param data
     */
    private void messageNotification(Map<String, String> data) {
        // Generate random notification ID
        Random rand = new Random();
        int upperbound = 999999999;
        int notificationId = rand.nextInt(upperbound);

        Intent intent = new Intent(this, ActivityWebView.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Pass the Data to the Intent from the notification
        if (data != null) {
            for (String key : data.keySet()) {
                String value = data.get(key);
                intent.putExtra(key, value);
                //Log.d(TAG, "Data, Key: " + key + " Value: " + value);
            }
        }

        // Set pending intent
        PendingIntent pendingIntent;
        // https://developer.android.com/guide/components/intents-filters#DeclareMutabilityPendingIntent
        pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ActivityWebView.CHANNEL_MESSAGE_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(ContextCompat.getColor(this, R.color.bs_primary))
                .setContentTitle(data.get("title"))
                .setContentText(data.get("body"))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(pendingIntent, true);

        // Fire notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}