package tracepal.app;

import android.Manifest;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BackgroundWorker extends Worker {
    private static final String PREFERENCE = "backgroundWorker";
    private static final String IS_FIRST_RUN = "isFirstRun";
    private static final String LOCATION_TOKEN = "locationToken";

    String TAG = "BackgroundWorker";
    FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

    public BackgroundWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Get application context, different from the system context
        Context appContext = getApplicationContext();

        // Get SharedPreferences
        SharedPreferences prefs = appContext.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(IS_FIRST_RUN, true);
        String locationToken = prefs.getString(LOCATION_TOKEN, "");
        Log.d(TAG, "locationToken: " + locationToken);

        // Check the token exists
        if (locationToken.isEmpty()) {
            return Result.failure();
        }

        // Check if this is the first run
        if (isFirstRun) {
            // If this is the first run, set the flag and skip execution
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(IS_FIRST_RUN, false);
            editor.apply();

            return Result.success();
        }

        // Android 10 (API level 29) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Only check for background location permission
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                // No background permission, get out silently
                return Result.failure();
        }
        // Android 6 (API level 23) to Android 9 (API level 28)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Only check for fine or coarse location permission
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                // No background permission, get out silently
                return Result.failure();
        }

        try {
            // Request a one-off location update, might be null if Android is optimising location access
            Location location = Tasks.await(mFusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null));
            if(location == null) {
                // Attempt getting last known location instead, which is always cached
                location = Tasks.await(mFusedLocationClient.getLastLocation());
            }

            // Check location exists
            if (location != null) {
                OkHttpClient client = new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)  // Enable retry on connection failure
                        .build();

                // Get URL API
                String urlAPI = appContext.getResources().getString(R.string.app_url_api);

                // Set up the RequestParams
                FormBody.Builder formBuilder = new FormBody.Builder()
                        .add("webhooks", "locate")
                        .add("token", locationToken)
                        .add("lat", String.valueOf(location.getLatitude()))
                        .add("long", String.valueOf(location.getLongitude()))
                        .add("accuracy", String.valueOf(location.getAccuracy()));

                // Build POST request
                Request request = new Request.Builder()
                        .url(urlAPI) // Use the base URL for POST
                        .post(formBuilder.build()) // Attach the request body
                        .build();

                // Send sync request
                Response response = null;
                response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    Log.d(TAG, "WebHook success");
                } else {
                    Log.d(TAG, "WebHook Error Code: " + response.code());
                    return Result.failure();
                }
            } else {
                return Result.failure();
            }
        } catch (IOException e) {
            return Result.failure();
        } catch (ExecutionException e) {
            return Result.failure();
        } catch (InterruptedException e) {
            return Result.failure();
        }

        // Indicate whether the work finished successfully
        return Result.success();
    }
}
