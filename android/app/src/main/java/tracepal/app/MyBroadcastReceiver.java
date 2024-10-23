package tracepal.app;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Generic broadcast receiver
 */
public class MyBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION = "ACTION";
    public static final String DECLINE_CALL = "DECLINE_CALL";
    public static final String NOTIFICATION_ID = "NOTIFICATION_ID";

    // Shared resources
    private static final String PREFERENCE = "backgroundWorker";
    private static final String IS_FIRST_RUN = "isFirstRun";

    /**
     * PeriodicWorkRequest has a minimum interval of 15 minutes because of battery optimisation
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle intentExtras = intent.getExtras();
        // Get application context, different from the system context
        Context appContext = context.getApplicationContext();

        // Check intent
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Get SharedPreferences
            SharedPreferences prefs = appContext.getSharedPreferences(PREFERENCE, appContext.MODE_PRIVATE);
            // Run immediately
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(IS_FIRST_RUN, false);
            editor.apply();

            // Set constraint, like network must be available
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            // Schedule the work request here
            PeriodicWorkRequest workRequest =
                    new PeriodicWorkRequest.Builder(BackgroundWorker.class, Math.max(15, Integer.parseInt(appContext.getString(R.string.app_locationTimer_worker))), TimeUnit.MINUTES)
                            .setConstraints(constraints)
                            .build();

            // Enqueue the work request with unique name
            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    "backgroundWork",
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                    workRequest
            );
        } else {
            // Handle actions
            switch (intentExtras.get(ACTION).toString()) {
                case DECLINE_CALL:
                    // Decline an incoming call and close the notification
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(Integer.parseInt(intentExtras.get(NOTIFICATION_ID).toString()));
                    break;
            }
        }
    }
}
