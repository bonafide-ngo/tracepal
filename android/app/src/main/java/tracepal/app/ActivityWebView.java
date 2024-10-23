package tracepal.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.media.MediaScannerConnection;
import android.app.PendingIntent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Random;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.imaginativeworld.oopsnointernet.callbacks.ConnectionCallback;
import org.imaginativeworld.oopsnointernet.dialogs.pendulum.DialogPropertiesPendulum;
import org.imaginativeworld.oopsnointernet.dialogs.pendulum.NoInternetDialogPendulum;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ActivityWebView extends AppCompatActivity {
    // Properties
    Boolean mIsDebug;
    WebView mWebView;
    String mCookies;
    String mFirebaseToken;
    String mLocationToken;
    String mAppDomain;
    String mAppRootUrl;
    String mAppBoardUrl;
    String mGooglePlayServicesUrl = "https://google.com";
    Boolean mIsGooglePlayServices = true;
    PowerManager.WakeLock mWakeLock = null;
    Handler mHandlerLocation = new Handler();
    FusedLocationProviderClient mFusedLocationClient;

    String TAG = "ActivityWebView";

    // Intent
    public static final String INTENT_ACTION = "IntentAction";
    public static final String INTENT_TEXT = "IntentText";
    public static final String INTENT_FILES = "IntentFiles";
    public static final String INTENT_FILE_BASE64 = "IntentFileBase64";
    public static final String INTENT_FILE_NAME = "IntentFileName";
    public static final String INTENT_FILE_SIZE = "IntentFileSize";
    public static final String INTENT_FILE_TYPE = "IntentFileType";
    Map<String, String> mIntentData;
    ArrayList<Object> mIntentFiles;

    // Channels
    public static final String CHANNEL_CALL_ID = "CALL_01";
    public static final String CHANNEL_MESSAGE_ID = "MESSAGE_01";
    public static final String CHANNEL_SYSTEM_ID = "SYSTEM_01";

    // JS functions
    private static final String JS_hookFirebaseToken_Callback = "javascript: frm.firebase.android.hookFirebaseToken_Callback('%s');";
    public static final String JS_firebaseHandler = "javascript: frm.firebase.android.handler('%s');";
    public static final String JS_shareTextHandler = "javascript: frm.share.android.textHandler('%s');";
    public static final String JS_shareFileHandler = "javascript: frm.share.android.fileHandler('%s');";
    public static final String JS_shareFilesHandler = "javascript: frm.share.android.filesHandler('%s');";
    public static final String JS_killSwitchHandler = "javascript: frm.webrtc.android.killSwitch();";

    // Shared resources
    private static final String PREFERENCE = "backgroundWorker";
    private static final String IS_FIRST_RUN = "isFirstRun";
    private static final String LOCATION_TOKEN = "locationToken";

    // Permissions
    private static final int LOCATION = 1;
    private static final int LOCATION_FOREGROUND = 11;
    private static final int LOCATION_BACKGROUND = 111;
    private static final int RECORD_AUDIO = 2;
    private static final int CAMERA = 3;
    private static final int RECORD_AUDIO_CAMERA = 23;
    private static final int FILE_READ = 4;
    private static final int FILE_WRITE = 5;
    private static final int INAPP_UPDATE = 6;
    private static final int READ_PHONE_STATE = 7;

    // Download callback
    private static String mDownloadCallback_Filename;
    private static String mDownloadCallback_DataUrlBase64;
    private static String mDownloadCallback_Message;

    // File selector
    public ValueCallback<Uri[]> mFilePathCallback;

    // Geolocation
    private String mGeolocationOrigin;
    private GeolocationPermissions.Callback mGeolocationCallback;

    // Resource permission
    private PermissionRequest mPermissionRequest;

    // Lang
    private static final String LANG_EN = "en";
    private static final String LANG_IT = "it";

    /**
     * Handle screen changes without reloading the WebView
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Handle the instance for external calls
     */
    private static ActivityWebView sInstance;

    public static ActivityWebView getInstance() {
        return sInstance;
    }

    /**
     * Handle file selector
     *
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case FILE_READ:
                // File chooser can return either dataString or clipData
                if (mFilePathCallback == null)
                    return;

                Uri[] results = null;
                if (resultCode == RESULT_OK && intent != null) {
                    // Get results
                    String dataString = intent.getDataString();
                    ClipData clipData = intent.getClipData();

                    // Check if either dataString or clipData exist
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    } else if (clipData != null) {
                        results = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            results[i] = item.getUri();
                        }
                    }
                }

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
                break;
            case INAPP_UPDATE:
                // In app updates
                if (resultCode == RESULT_OK) {
                    // Clear cache
                    mWebView.clearCache(true);
                } else {
                    // If the update is cancelled or fails, request to start the update again.
                    checkInAppUpdate();
                }
                break;
        }
    }

    /**
     * Background location permission requires that fine location permission is already granted.
     */
    private void checkBackgroundLocationPermission() {
        // Only check for background location permission on Android 10 (API level 29) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request background location permission
                requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_BACKGROUND);
            }
        }
    }

    /**
     * Handle callback on requesting permissions
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Foreground location permission granted
                    // Android 10 (API level 29) and above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Request background location permission separately
                        // Background location permission requires that fine location permission is already granted
                        checkBackgroundLocationPermission();
                    }
                    // Android 6 (API level 23) to Android 9 (API level 28)
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        // Callback to runnable
                        mHandlerLocation.post(trackLocation);
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                        || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    String title;
                    String message;
                    String button;
                    switch (Locale.getDefault().getLanguage()) {
                        case LANG_IT:
                            title = getResources().getString(R.string.lang_it_alert_title_information);
                            message = getResources().getString(R.string.lang_it_alert_message_permission_location);
                            button = getResources().getString(R.string.lang_it_alert_button_continue);
                        case LANG_EN:
                        default:
                            title = getResources().getString(R.string.lang_en_alert_title_information);
                            message = getResources().getString(R.string.lang_en_alert_message_permission_location);
                            button = getResources().getString(R.string.lang_en_alert_button_continue);
                    }
                    showAlert(title, message, button, R.style.AlertInfo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION);
                        }
                    });
                }
                break;
            case LOCATION_FOREGROUND:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    // Callback to web chrome client, on first instance when permission is shown
                    mGeolocationCallback.invoke(mGeolocationOrigin, true, false);
                else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                        || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    String title;
                    String message;
                    String button;
                    switch (Locale.getDefault().getLanguage()) {
                        case LANG_IT:
                            title = getResources().getString(R.string.lang_it_alert_title_information);
                            message = getResources().getString(R.string.lang_it_alert_message_permission_location);
                            button = getResources().getString(R.string.lang_it_alert_button_continue);
                        case LANG_EN:
                        default:
                            title = getResources().getString(R.string.lang_en_alert_title_information);
                            message = getResources().getString(R.string.lang_en_alert_message_permission_location);
                            button = getResources().getString(R.string.lang_en_alert_button_continue);
                    }
                    showAlert(title, message, button, R.style.AlertInfo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_FOREGROUND);
                        }
                    });
                }
                break;
            case LOCATION_BACKGROUND:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    // Callback to runnable
                    mHandlerLocation.post(trackLocation);
                else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    String title;
                    String message;
                    String button;
                    switch (Locale.getDefault().getLanguage()) {
                        case LANG_IT:
                            title = getResources().getString(R.string.lang_it_alert_title_information);
                            message = getResources().getString(R.string.lang_it_alert_message_permission_location);
                            button = getResources().getString(R.string.lang_it_alert_button_continue);
                        case LANG_EN:
                        default:
                            title = getResources().getString(R.string.lang_en_alert_title_information);
                            message = getResources().getString(R.string.lang_en_alert_message_permission_location);
                            button = getResources().getString(R.string.lang_en_alert_button_continue);
                    }
                    showAlert(title, message, button, R.style.AlertInfo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION);
                        }
                    });
                }
                break;
            case RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    mPermissionRequest.grant(mPermissionRequest.getResources());
                else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    String title;
                    String message;
                    String button;
                    switch (Locale.getDefault().getLanguage()) {
                        case LANG_IT:
                            title = getResources().getString(R.string.lang_it_alert_title_information);
                            message = getResources().getString(R.string.lang_it_alert_message_permission_audio);
                            button = getResources().getString(R.string.lang_it_alert_button_continue);
                        case LANG_EN:
                        default:
                            title = getResources().getString(R.string.lang_en_alert_title_information);
                            message = getResources().getString(R.string.lang_en_alert_message_permission_audio);
                            button = getResources().getString(R.string.lang_en_alert_button_continue);
                    }
                    showAlert(title, message, button, R.style.AlertInfo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
                        }
                    });
                } else
                    mPermissionRequest.deny();
                break;
            case CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    mPermissionRequest.grant(mPermissionRequest.getResources());

                else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    String title;
                    String message;
                    String button;
                    switch (Locale.getDefault().getLanguage()) {
                        case LANG_IT:
                            title = getResources().getString(R.string.lang_it_alert_title_information);
                            message = getResources().getString(R.string.lang_it_alert_message_permission_camera);
                            button = getResources().getString(R.string.lang_it_alert_button_continue);
                        case LANG_EN:
                        default:
                            title = getResources().getString(R.string.lang_en_alert_title_information);
                            message = getResources().getString(R.string.lang_en_alert_message_permission_camera);
                            button = getResources().getString(R.string.lang_en_alert_button_continue);
                    }
                    showAlert(title, message, button, R.style.AlertInfo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA);
                        }
                    });
                } else
                    mPermissionRequest.deny();
                break;
            case RECORD_AUDIO_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    mPermissionRequest.grant(mPermissionRequest.getResources());
                else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
                        || shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    String title;
                    String message;
                    String button;
                    switch (Locale.getDefault().getLanguage()) {
                        case LANG_IT:
                            title = getResources().getString(R.string.lang_it_alert_title_information);
                            message = getResources().getString(R.string.lang_it_alert_message_permission_audio_camera);
                            button = getResources().getString(R.string.lang_it_alert_button_continue);
                        case LANG_EN:
                        default:
                            title = getResources().getString(R.string.lang_en_alert_title_information);
                            message = getResources().getString(R.string.lang_en_alert_message_permission_audio_camera);
                            button = getResources().getString(R.string.lang_en_alert_button_continue);
                    }
                    showAlert(title, message, button, R.style.AlertInfo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, RECORD_AUDIO_CAMERA);
                        }
                    });
                } else
                    mPermissionRequest.deny();
                break;
            case READ_PHONE_STATE:
                // Telephone state is requested by the phone, not by the web app, so there's no permission to grant nor deny
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    detectPhoneCall();
                else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {
                    String title;
                    String message;
                    String button;
                    switch (Locale.getDefault().getLanguage()) {
                        case LANG_IT:
                            title = getResources().getString(R.string.lang_it_alert_title_information);
                            message = getResources().getString(R.string.lang_it_alert_message_permission_telephone);
                            button = getResources().getString(R.string.lang_it_alert_button_continue);
                        case LANG_EN:
                        default:
                            title = getResources().getString(R.string.lang_en_alert_title_information);
                            message = getResources().getString(R.string.lang_en_alert_message_permission_telephone);
                            button = getResources().getString(R.string.lang_en_alert_button_continue);
                    }
                    showAlert(title, message, button, R.style.AlertInfo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE);
                        }
                    });
                }
                break;
            case FILE_WRITE:
                downloadCallback();
                break;
        }
    }

    /**
     * Check if play services are supported
     */
    public void checkGooglePlayServices() {
        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int errorCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (errorCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(errorCode)) {
                apiAvailability.makeGooglePlayServicesAvailable(this);
            } else {
                // Save for later because it's an async operation
                mIsGooglePlayServices = false;
                // Redirect to the static error page
                mWebView.loadUrl(mGooglePlayServicesUrl);
            }
        }
    }

    /**
     * Check battery optimisation and ask user to disable it for unrestricted background data usage
     */
    private void checkBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(this.POWER_SERVICE);
        String packageName = getPackageName();

        // Check if the app is already ignoring battery optimizations
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            // Prompt the user to disable battery optimizations for this app
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    /**
     * Handle the back button
     */
    @Override
    public void onBackPressed() {
        // Pop the browser back stack or exit the activity
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Save the App state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    /**
     * Restore the App state
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    /**
     * Handling KeyDown
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Audio manager
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // Check if the key event was the Back button and if there's history
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_VOLUME_UP:
                //Adjust the Volume
                if (audio.isMusicActive())
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                else
                    audio.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                //Adjust the Volume
                if (audio.isMusicActive())
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                else
                    audio.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
        }

        // Bubble up to the default system behavior
        // i.e. exit the activity
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Read an asset into a string
     * This utility is not currently used but it might turn useful in the future
     * https://stackoverflow.com/questions/16110002/read-assets-file-as-string
     *
     * @param assetPath
     * @return
     * @throws IOException
     */
    public String asset2string(String assetPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream is = getAssets().open(assetPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String string;
        while ((string = br.readLine()) != null) {
            sb.append(string);
        }
        br.close();
        return string;
    }

    /** ***************************************************************************************** */

    /**
     * On create event
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set debugging
        mIsDebug = Boolean.parseBoolean(getResources().getString(R.string.app_debug));

        // No Internet Dialog: Pendulum
        NoInternetDialogPendulum.Builder oopsNoInternet = new NoInternetDialogPendulum.Builder(this, getLifecycle());
        createOopsNoInternet(oopsNoInternet);

        // Notification/Activity on locked screen
        // https://stackoverflow.com/questions/35356848/android-how-to-launch-activity-over-lock-screen/55998126#55998126
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null)
                keyguardManager.requestDismissKeyguard(this, null);
        } else
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // Disable screenshot and background thumbnail
        // N.B. This security feature is not required in this app
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // Save instance
        sInstance = this;

        // Set view
        setContentView(R.layout.activity_web_view);

        // Hide top title bar
        getSupportActionBar().hide();

        // Create Firebase Channels
        createChannels();

        // Cancel all Firebase Notifications when app opens
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        // Get WebView
        mWebView = (WebView) findViewById(R.id.webview);
        // Avoid blank white loading screen by enforcing a transparent one
        mWebView.setBackgroundColor(0x00000000);

        // Enable JS
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);

        // HTML5 API flags
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        // Enable Geolocation
        mWebView.getSettings().setGeolocationEnabled(true);

        // Set UserAgent
        String userAgent = mWebView.getSettings().getUserAgentString();
        mWebView.getSettings().setUserAgentString(userAgent + " Android/" + getApplicationContext().getPackageName() + "/" + BuildConfig.VERSION_NAME);

        // Load WebView clients
        mWebView.setWebViewClient(new extendedWebViewClient());
        mWebView.setWebChromeClient(new extendedWebChromeClient());

        // Load the WebAppInterface: Javascript >> Android, Android >> Javascript
        mWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // Enable remote debugging with developer tools
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
            mWebView.setWebContentsDebuggingEnabled(true);

        // Set app params
        mAppDomain = getResources().getString(R.string.app_domain);
        mAppRootUrl = getResources().getString(R.string.app_url_root);
        mAppBoardUrl = getResources().getString(R.string.app_url_board);

        // Load app or automatically recover state only if GooglePlayServices is supported
        if (mIsGooglePlayServices) {
            // Get the last known location
            // https://developer.android.com/develop/sensors-and-location/location/retrieve-current
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // Init Intent data and files
            mIntentData = new HashMap();
            mIntentFiles = new ArrayList();

            // Handle intent
            Intent intent = getIntent();
            String intentAction = intent.getAction();
            String intentType = intent.getType();
            Bundle intentExtras = intent.getExtras();
            Uri intentData = intent.getData();

            // Intent main or notification or call
            if (Intent.ACTION_MAIN.equals(intentAction) && intentExtras != null) {
                //Log.d(TAG, "Main or Notification Intent");

                // Set intent
                mIntentData.put(INTENT_ACTION, intentAction);
                // Handle notification
                for (String key : intentExtras.keySet()) {
                    Object value = intentExtras.get(key);

                    // Add mapped data element
                    mIntentData.put(key, value.toString());
                    //Log.d(TAG, key + ": " + value.toString());
                }
            }
            // Intent share sheet, single share
            else if (Intent.ACTION_SEND.equals(intentAction) && intentType != null) {
                //Log.d(TAG, "Share sheet Intent");

                // Set intent
                mIntentData.put(INTENT_ACTION, intentAction);

                // Handle text to share
                if (intentType.startsWith("text/")) {
                    // Append intent data
                    mIntentData.put(INTENT_TEXT, intent.getStringExtra(Intent.EXTRA_TEXT));
                }
                // Handle file to share
                else {
                    // Init
                    String fileBase64 = "";
                    String fileType = "";
                    String fileName = "";
                    String fileSize = "";

                    // Grab uri
                    Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

                    // Convert URI stream into Base64 string
                    // http://www.java2s.com/example/android/file-input-output/file-uri-to-base64.html
                    try {
                        // Convert Uri to Stream
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        // Get resource details
                        Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
                        // Get columns details for name and size
                        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                        returnCursor.moveToFirst();
                        // Get details
                        fileType = getContentResolver().getType(uri);
                        fileName = returnCursor.getString(nameIndex);
                        fileSize = Long.toString(returnCursor.getLong(sizeIndex));

                        // Init buffer
                        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                        int bufferSize = 1024;
                        byte[] buffer = new byte[bufferSize];
                        // Read buffer
                        int len = 0;
                        while ((len = inputStream.read(buffer)) != -1) {
                            byteBuffer.write(buffer, 0, len);
                        }
                        // Convert buffer to byte array
                        byte[] bytes = byteBuffer.toByteArray();
                        // Convert to base64 data url
                        fileBase64 = "data:" + fileType + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
                    } catch (IOException e) {
                        Log.e(TAG, "IOException", e);
                    }

                    // Append intent data
                    mIntentData.put(INTENT_FILE_NAME, fileName);
                    mIntentData.put(INTENT_FILE_SIZE, fileSize);
                    mIntentData.put(INTENT_FILE_TYPE, fileType);
                    mIntentData.put(INTENT_FILE_BASE64, fileBase64);
                }
            }
            // Intent share sheet, multiple share, files only, cannot be text
            else if (Intent.ACTION_SEND_MULTIPLE.equals(intentAction) && intentType != null) {
                //Log.d(TAG, "Share sheet Intent");

                // Set intent
                mIntentData.put(INTENT_ACTION, intentAction);

                // Get list of URIs
                ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                // Init index
                Integer index = 0;
                if (uris != null)
                    for (Uri uri : uris) {

                        // Init
                        String fileBase64 = "";
                        String fileType = "";
                        String fileName = "";
                        String fileSize = "";

                        // Convert URI stream into Base64 string
                        // http://www.java2s.com/example/android/file-input-output/file-uri-to-base64.html
                        try {
                            // Convert Uri to Stream
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            // Get resource details
                            Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
                            // Get columns details for name and size
                            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                            returnCursor.moveToFirst();
                            // Get details
                            fileType = getContentResolver().getType(uri);
                            fileName = returnCursor.getString(nameIndex);
                            fileSize = Long.toString(returnCursor.getLong(sizeIndex));

                            // Init buffer
                            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                            int bufferSize = 1024;
                            byte[] buffer = new byte[bufferSize];
                            // Read buffer
                            int len = 0;
                            while ((len = inputStream.read(buffer)) != -1) {
                                byteBuffer.write(buffer, 0, len);
                            }
                            // Convert buffer to byte array
                            byte[] bytes = byteBuffer.toByteArray();
                            // Convert to base64 data url
                            fileBase64 = "data:" + fileType + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
                        } catch (IOException e) {
                            Log.e(TAG, "IOException", e);
                        }

                        // Create json object
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put(INTENT_FILE_NAME, fileName);
                            jsonObject.put(INTENT_FILE_SIZE, fileSize);
                            jsonObject.put(INTENT_FILE_TYPE, fileType);
                            jsonObject.put(INTENT_FILE_BASE64, fileBase64);
                        } catch (JSONException e) {
                            Log.e(TAG, "JSONException", e);
                        }

                        // Append intent files
                        mIntentFiles.add(jsonObject);
                    }
            }

            // Begin a new state or restore the previous one
            if (savedInstanceState == null) {
                // Clear cache
                if (mIsDebug)
                    mWebView.clearCache(true);

                if (Intent.ACTION_VIEW.equals(intentAction) && intentData != null) {
                    mWebView.loadUrl(intentData.toString());
                } else if (mIntentData.isEmpty())
                    // Load root URL
                    mWebView.loadUrl(mAppRootUrl);
                else
                    // Load board URL to handle an intent
                    mWebView.loadUrl(mAppBoardUrl);
            } else
                // Restore state
                mWebView.restoreState(savedInstanceState);

            // Check battery optimisation for background worker
            checkBatteryOptimization();
        }

        /**
         * Handle HTTP/s file download
         */
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                // Get filename
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

                // Start request
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                // Start Download
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
            }
        });
    }

    /**
     * On resume event
     */
    @Override
    protected void onResume() {
        super.onResume();

        // In-app updates
        checkInAppUpdate();
    }

    /**
     * Cancel a background work (i.e. mLocationToken changed)
     */
    private void cancelBackgroundWork() {
        // Cancel the existing work (if needed)
        WorkManager.getInstance(this).cancelUniqueWork("backgroundWork");

        // Get SharedPreferences
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        // Reset first run
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_FIRST_RUN, true);
        editor.apply();

    }

    /**
     * PeriodicWorkRequest has a minimum interval of 15 minutes because of battery optimisation
     */
    public void scheduleBackgroundWork() {
        // Get SharedPreferences
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
        // Set first run
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_FIRST_RUN, true);
        editor.apply();

        // Set constraint, like network must be available
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Schedule the work request here
        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(BackgroundWorker.class, Math.max(15, Integer.parseInt(getResources().getString(R.string.app_locationTimer_worker))), TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        // Enqueue the work request with unique name
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "backgroundWork",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                workRequest
        );
    }

    /** ***************************************************************************************** */

    /**
     * WebViewClient subclass loads all hyperlinks in the existing WebView
     */
    public class extendedWebViewClient extends WebViewClient {
        /**
         * Handle loading URLs
         *
         * @param view
         * @param request
         * @return
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            String host = request.getUrl().getHost();

            if (url.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(intent);
                return true;
            } else if (host == null || host.equals(mAppDomain)) {
                // Local URL, do not override; let the WebView loading the page
                return false;
            } else {
                // External URL, launch another Activity (default browser) that handles URLs
                Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                startActivity(intent);
                return true;
            }
        }

        /**
         * Handle activities on page loaded
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            // Handle cookies
            mCookies = CookieManager.getInstance().getCookie(url);

            // Check GooglePlayServices
            checkGooglePlayServices();

            // Handle Intent Data if any and when full page is loaded
            if (mWebView.getProgress() == 100 && !mIntentData.isEmpty()) {
                //Log.d(TAG, "Intent OnPageFinished");

                switch (mIntentData.get(INTENT_ACTION)) {
                    case Intent.ACTION_MAIN:
                        // Call the JS update method
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                JSONObject jParams = new JSONObject(mIntentData);
                                String jScript = String.format(JS_firebaseHandler, jParams.toString());
                                //Log.d(TAG, jScript);
                                mWebView.evaluateJavascript(jScript, null);
                                // Reset Intent data because of back buttom
                                mIntentData = new HashMap();
                            }
                        });
                        break;
                    case Intent.ACTION_SEND:
                        if (mIntentData.get(INTENT_TEXT) != null) {
                            // Call the JS share text handler
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String jScript = String.format(JS_shareTextHandler, mIntentData.get(INTENT_TEXT));
                                    //Log.d(TAG, jScript);
                                    mWebView.evaluateJavascript(jScript, null);
                                    // Reset Intent data because of back buttom
                                    mIntentData = new HashMap();
                                }
                            });
                        } else {
                            // Call the JS share file handler
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    JSONObject jParams = new JSONObject(mIntentData);
                                    String jScript = String.format(JS_shareFileHandler, jParams.toString());
                                    //Log.d(TAG, jScript);
                                    mWebView.evaluateJavascript(jScript, null);
                                    // Reset Intent data because of back buttom
                                    mIntentData = new HashMap();
                                }
                            });
                        }
                        break;
                    case Intent.ACTION_SEND_MULTIPLE:
                        // Call the JS share files handler
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                JSONArray jParams = new JSONArray();
                                for (Object jsonObject : mIntentFiles) {
                                    jParams.put(jsonObject);
                                }
                                String jScript = String.format(JS_shareFilesHandler, jParams.toString());
                                //Log.d(TAG, jScript);
                                mWebView.evaluateJavascript(jScript, null);
                                // Reset Intent data and files because of back buttom
                                mIntentData = new HashMap();
                                mIntentFiles = new ArrayList();
                            }
                        });
                        break;
                }
            }

        }

        /**
         * Handle network error
         * https://stackoverflow.com/questions/6392318/detecting-webview-error-and-show-message
         *
         * @param view
         * @param request
         * @param error
         */
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            // Alert error
            if (mIsDebug) {
                showAlert("Network Error", "(" + error.getErrorCode() + ") " + error.getDescription(), "Retry", R.style.AlertError, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // Reload url
                        view.loadUrl(mAppRootUrl);
                    }
                });
                super.onReceivedError(view, request, error);
                // Override default webpage error
                view.loadUrl("about:blank");
            }
        }
    }

    /** ***************************************************************************************** */

    /**
     * WebChromeClient subclass handles UI-related calls
     * Note: think chrome as in decoration, not the Chrome browser
     */
    public class extendedWebChromeClient extends WebChromeClient {
        /**
         * Handle the request to open the file chooser
         * From SDK 22+
         *
         * @param webView
         * @param filePathCallback
         * @param fileChooserParams
         * @return
         */
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            // make sure there is no existing callback
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = null;
            }

            mFilePathCallback = filePathCallback;

            Intent intent = fileChooserParams.createIntent();
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, FILE_READ);

            return true;
        }

        /**
         * Handle general permission requested by the browser
         *
         * @param request
         */
        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            // Store for async callback
            mPermissionRequest = request;

            // Get permissions
            String[] permissions = request.getResources();

            // Joint media permissions (audio + video)
            if (permissions.length == 2
                    && Arrays.asList(permissions).contains("android.webkit.resource.AUDIO_CAPTURE")
                    && Arrays.asList(permissions).contains("android.webkit.resource.VIDEO_CAPTURE")) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, RECORD_AUDIO_CAMERA);
            } else
                for (String permission : permissions) {
                    switch (permission) {
                        case "android.webkit.resource.AUDIO_CAPTURE":
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
                            break;
                        case "android.webkit.resource.VIDEO_CAPTURE":
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA);
                            break;
                    }
                }
        }

        /**
         * Handle GeoLocation permission requested by the browser
         *
         * @param origin
         * @param callback
         */
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            // Store for async callback
            mGeolocationOrigin = origin;
            mGeolocationCallback = callback;

            // Ask for permission
            if (ContextCompat.checkSelfPermission(ActivityWebView.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(ActivityWebView.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Ask the user for permission
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_FOREGROUND);
            } else {
                // Callback to web chrome client, on following instances when permission is shown no more
                callback.invoke(mGeolocationOrigin, true, false);
            }

        }
    }

    /** ***************************************************************************************** */

    /**
     * Web interface to allow communication between Javascript and Android
     */
    public class WebAppInterface {
        Context mContext;

        /**
         * Instantiate the interface and set the context
         *
         * @param c
         */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /**
         * Hook the Firebase Token (async)
         * N.B. Registered by the javascript page
         */
        @JavascriptInterface
        public void hookFirebaseToken() {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull Task<String> task) {
                            if (task.isSuccessful()) {
                                // Store the new FCM registration token (string)
                                mFirebaseToken = task.getResult();
                                //Log.d(TAG, "Firebase Token: " + mFirebaseToken);

                                // Call the update method to sync server
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String jScript = String.format(JS_hookFirebaseToken_Callback, mFirebaseToken);
                                        //Log.d(TAG, jScript);
                                        mWebView.evaluateJavascript(jScript, null);
                                    }
                                });
                            }

                        }
                    });
        }

        /**
         * Hook to keep the screen on
         * N.B. Registered by the javascript page
         */
        @JavascriptInterface
        public void hookKeepScreenOn() {
            if (mWakeLock == null) {
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
                mWakeLock.acquire();
            }
        }

        /**
         * Hook to release the screen on
         * N.B. Registered by the javascript page
         */
        @JavascriptInterface
        public void hookReleaseScreenOn() {
            if (mWakeLock != null) {

                mWakeLock.release();
                mWakeLock = null;
            }
        }

        /**
         * Hook a download
         * N.B. Registered by the javascript page
         */
        @JavascriptInterface
        public void hookDownload(String filename, String dataUrlBase64, String message) {
            // Store for async call
            mDownloadCallback_Filename = filename;
            mDownloadCallback_DataUrlBase64 = dataUrlBase64;
            mDownloadCallback_Message = message;

            // Request permission for Android 10 (Q) and less (<= SDK 29) only
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, FILE_WRITE);
            } else {
                downloadCallback();
            }
        }

        /**
         * Hook a share
         * N.B. Registered by the javascript page
         *
         * @param title
         * @param url
         */
        @JavascriptInterface
        public void hookShare(String title, String url) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            // Set mimetype
            sendIntent.setType("text/plain");
            // Set the Url to share, og (open-ghaph) will do the rest for showing the logo a, title and description)
            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
            // Set the subject when sharing via email
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);

            // Show the Sharesheet
            startActivity(Intent.createChooser(sendIntent, null));
        }

        /**
         * Hook to detect phone status and enable kill switch
         * N.B. Registered by the javascript page
         */
        @JavascriptInterface
        public void hookDetectPhoneStatus() {
            // Check telephone permission
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE);
            } else
                detectPhoneCall();
        }

        /**
         * Hook to start location tracking
         * N.B. Registered by the javascript page
         * https://web.archive.org/web/20200131001301/http://www.mopri.de/2010/timertask-bad-do-it-the-android-way-use-a-handler/
         */
        @JavascriptInterface
        public void hookStartTracker(String locationToken) {
            //Log.d(TAG, "Location Token: " + locationToken);

            // Do not restart the same tracker
            if (locationToken.isEmpty() || locationToken.equals(mLocationToken))
                return;

            // Store the new location token
            mLocationToken = locationToken;

            // Get SharedPreferences
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
            // Set location token
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(LOCATION_TOKEN, mLocationToken);
            editor.apply();

            // Cancel background worker (just in case)
            cancelBackgroundWork();
            // Start background worker
            scheduleBackgroundWork();
            // Start foreground runnable
            mHandlerLocation.post(trackLocation);
        }

        /**
         * Hook to stop location tracking
         * N.B. Registered by the javascript page
         * https://web.archive.org/web/20200131001301/http://www.mopri.de/2010/timertask-bad-do-it-the-android-way-use-a-handler/
         */
        @JavascriptInterface
        public void hookStopTracker() {
            // Reset the location token
            mLocationToken = "";

            // Get SharedPreferences
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
            // Reset location token
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(LOCATION_TOKEN, mLocationToken);
            editor.apply();

            // Cancel background worker
            cancelBackgroundWork();
            // Stop foreground runnable
            mHandlerLocation.removeCallbacks(trackLocation);
        }
    }

    /**
     * Track a location in foreground
     */
    public Runnable trackLocation = new Runnable() {
        @Override
        public void run() {
            // Android 10 (API level 29) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Check for foreground and background permission
                if (ContextCompat.checkSelfPermission(mWebView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(mWebView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(mWebView.getContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION);
                    return;
                }
            }
            // Android 6 (API level 23) to Android 9 (API level 28)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Check for foreground permission
                if (ContextCompat.checkSelfPermission(mWebView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(mWebView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION);
                    return;
                }
            }

            // Create a LocationRequest object using the new Builder pattern
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                    .setIntervalMillis(Long.parseLong(getResources().getString(R.string.app_locationTimer_runnable)))
                    .setMinUpdateIntervalMillis(Long.parseLong(getResources().getString(R.string.app_locationTimer_runnable)) / 10)
                    .build();

            // Define the location callback to handle location updates
            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null || locationResult.getLocations().isEmpty()) {
                        return;
                    }

                    // Get the first location
                    Location location = locationResult.getLocations().get(0);
                    // Got location. In some rare situations this can be null.
                    if (location != null) {
                        OkHttpClient client = new OkHttpClient.Builder()
                                .retryOnConnectionFailure(true)  // Enable retry on connection failure
                                .build();

                        // Get URL API
                        String urlAPI = getResources().getString(R.string.app_url_api);

                        // Set up the RequestParams
                        FormBody.Builder formBuilder = new FormBody.Builder()
                                .add("webhooks", "locate")
                                .add("token", mLocationToken)
                                .add("lat", String.valueOf(location.getLatitude()))
                                .add("long", String.valueOf(location.getLongitude()))
                                .add("accuracy", String.valueOf(location.getAccuracy()));

                        // Build POST request
                        Request request = new Request.Builder()
                                .url(urlAPI) // Use the base URL for POST
                                .post(formBuilder.build()) // Attach the request body
                                .build();

                        // Send async request
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.d(TAG, "WebHook Error: " + e.getMessage());
                            }
                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    Log.d(TAG, "WebHook success");
                                } else {
                                    Log.d(TAG, "WebHook Error Code: " + response.code());
                                }
                            }
                        });
                    }
                }
            };

            // Start requesting location updates
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            // Remove the handler's postDelayed loop since requestLocationUpdates handles it
            //mHandlerLocation.postDelayed(this, Integer.parseInt(getResources().getString(R.string.app_locationTimer_runnable)));
        }
    };

    /**
     * Download a resource
     */
    public void downloadCallback() {
        // Set local vars from callback
        String filename = mDownloadCallback_Filename;
        String dataUrlBase64 = mDownloadCallback_DataUrlBase64;
        String message = mDownloadCallback_Message;

        // Sanitise filename, replace any space and forslash with an underscore, saving will fail otherwise
        filename = filename.replaceAll("[\\s\\/]", "_");

        // Set the target folder
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        // Probe the original filename
        File file = new File(path, filename);
        try {
            if (!path.exists())
                path.mkdirs();
            if (!file.exists())
                file.createNewFile();
            else {
                // Append timestamp for uniqueness i.e. filename.123456789.ext
                String ext = filename.substring(filename.lastIndexOf("."));
                String name = filename.substring(0, filename.lastIndexOf("."));
                filename = name + "." + (Instant.now().getEpochSecond()) + ext;

                // Switch and create new unique file
                file = new File(path, filename);
                file.createNewFile();
            }

            // data:image/gif;base64,R0lGODlhAQ...
            String base64EncodedString = dataUrlBase64.substring(dataUrlBase64.indexOf(",") + 1);
            byte[] decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT);
            OutputStream os = new FileOutputStream(file);
            os.write(decodedBytes);
            os.close();

            // Tell the media scanner about the new file so that it is immediately available to the user.
            MediaScannerConnection.scanFile(ActivityWebView.this,
                    new String[]{file.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            //Log.d(TAG, "Scanned " + path + ":");
                            //Log.d(TAG, "-> uri=" + uri);
                        }
                    });

            // create system tray notification after download complete and add pending intent
            downloadNotification(filename, message);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }
    }

    /**
     * Add a download notification in the system tray
     *
     * @param messageTitle
     * @param messageBody
     */
    public void downloadNotification(String messageTitle, String messageBody) {
        systemNotification(messageTitle, messageBody, DownloadManager.ACTION_VIEW_DOWNLOADS);
    }

    /**
     * Add a system notification in the system tray
     *
     * @param messageTitle
     * @param messageBody
     * @param action
     */
    public void systemNotification(String messageTitle, String messageBody, String action) {
        // Generate random notification ID
        Random rand = new Random();
        int upperbound = 999999999;
        int notificationId = rand.nextInt(upperbound);

        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Set pending intent
        PendingIntent pendingIntent;
        // https://developer.android.com/guide/components/intents-filters#DeclareMutabilityPendingIntent
        pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Set notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ActivityWebView.CHANNEL_SYSTEM_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(ContextCompat.getColor(this, R.color.bs_primary))
                .setContentText(messageBody)
                .setContentTitle(messageTitle)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Fire notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    /**
     * Create Channels
     * N.B. This is needed to display notifications such as calls, messages and system
     */
    private void createChannels() {
        // Init and set language
        Integer channelCallName = null;
        Integer channelCallDescription = null;
        Integer channelMessageName = null;
        Integer channelMessageDescription = null;
        Integer channelSystemName = null;
        Integer channelSystemDescription = null;
        switch (Locale.getDefault().getLanguage()) {
            case LANG_IT:
                channelCallName = R.string.lang_en_channel_call_name;
                channelCallDescription = R.string.lang_en_channel_call_description;
                channelMessageName = R.string.lang_en_channel_message_name;
                channelMessageDescription = R.string.lang_en_channel_message_description;
                channelSystemName = R.string.lang_en_channel_system_name;
                channelSystemDescription = R.string.lang_en_channel_system_description;
            case LANG_EN:
            default:
                channelCallName = R.string.lang_en_channel_call_name;
                channelCallDescription = R.string.lang_en_channel_call_description;
                channelMessageName = R.string.lang_en_channel_message_name;
                channelMessageDescription = R.string.lang_en_channel_message_description;
                channelSystemName = R.string.lang_en_channel_system_name;
                channelSystemDescription = R.string.lang_en_channel_system_description;
        }
        // Call audio
        AudioAttributes audioCall = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        // Call channel
        NotificationChannelCompat.Builder channelCall = new NotificationChannelCompat.Builder(CHANNEL_CALL_ID, NotificationManager.IMPORTANCE_HIGH)
                .setName(getResources().getString(channelCallName))
                .setDescription(getResources().getString(channelCallDescription))
                .setLightsEnabled(true)
                .setVibrationEnabled(true)
                .setVibrationPattern(new long[]{
                        100, 500, 1500, 500, 1500, 500, 1500, 500, 1500, 500, 3075,
                        500, 1500, 500, 1500, 500, 1500, 500, 1500, 500, 3075,
                        500, 1500, 500, 1500, 500, 1500, 500, 1500, 500, 3075,
                        500, 1500, 500, 1500, 500, 1500, 500, 1500, 500, 3075}) // Match sound pattern
                .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/" + R.raw.ring_receiver), audioCall);
        NotificationManagerCompat.from(getApplicationContext()).createNotificationChannel(channelCall.build());

        // Message audio
        AudioAttributes audioMessage = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        // Message channel
        NotificationChannelCompat.Builder channelMessage = new NotificationChannelCompat.Builder(CHANNEL_MESSAGE_ID, NotificationManager.IMPORTANCE_HIGH)
                .setName(getResources().getString(channelMessageName))
                .setDescription(getResources().getString(channelMessageDescription))
                .setLightsEnabled(true)
                .setVibrationEnabled(true)
                .setVibrationPattern(new long[]{100, 100, 100, 100}) // Match sound pattern
                .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/" + R.raw.beep), audioMessage);
        NotificationManagerCompat.from(getApplicationContext()).createNotificationChannel(channelMessage.build());

        // System audio
        AudioAttributes audioSystem = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        // System channel
        NotificationChannelCompat.Builder channelSystem = new NotificationChannelCompat.Builder(CHANNEL_SYSTEM_ID, NotificationManager.IMPORTANCE_HIGH)
                .setName(getResources().getString(channelMessageName))
                .setDescription(getResources().getString(channelMessageDescription))
                .setLightsEnabled(true)
                .setVibrationEnabled(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioSystem);
        NotificationManagerCompat.from(getApplicationContext()).createNotificationChannel(channelSystem.build());
    }

    /**
     * Handle no internet connection
     * https://github.com/ImaginativeShohag/Oops-No-Internet
     *
     * @param oopsNoInternet
     */
    private void createOopsNoInternet(NoInternetDialogPendulum.Builder oopsNoInternet) {
        DialogPropertiesPendulum oopsNoInternetProps = oopsNoInternet.getDialogProperties();
        // Optional
        oopsNoInternetProps.setConnectionCallback(new ConnectionCallback() {
            @Override
            public void hasActiveConnection(boolean hasActiveConnection) {
                // ...
            }
        });
        switch (Locale.getDefault().getLanguage()) {
            case LANG_IT:
                oopsNoInternetProps.setNoInternetConnectionTitle(getResources().getString(R.string.lang_it_no_internet));
                oopsNoInternetProps.setNoInternetConnectionMessage(getResources().getString(R.string.lang_it_check_internet));
                oopsNoInternetProps.setPleaseTurnOnText(getResources().getString(R.string.lang_it_turn_on));
                oopsNoInternetProps.setPleaseTurnOnText(getResources().getString(R.string.lang_it_turn_off));
                oopsNoInternetProps.setWifiOnButtonText(getResources().getString(R.string.lang_it_wifi));
                oopsNoInternetProps.setOnAirplaneModeMessage(getResources().getString(R.string.lang_it_turn_on_airplane_mode));
                oopsNoInternetProps.setAirplaneModeOffButtonText(getResources().getString(R.string.lang_it_airplane_mode));
                break;
            case LANG_EN:
            default:
                oopsNoInternetProps.setNoInternetConnectionTitle(getResources().getString(R.string.lang_en_no_internet));
                oopsNoInternetProps.setNoInternetConnectionMessage(getResources().getString(R.string.lang_en_check_internet));
                oopsNoInternetProps.setPleaseTurnOnText(getResources().getString(R.string.lang_en_turn_on));
                oopsNoInternetProps.setPleaseTurnOnText(getResources().getString(R.string.lang_en_turn_off));
                oopsNoInternetProps.setWifiOnButtonText(getResources().getString(R.string.lang_en_wifi));
                oopsNoInternetProps.setOnAirplaneModeMessage(getResources().getString(R.string.lang_en_turn_on_airplane_mode));
                oopsNoInternetProps.setAirplaneModeOffButtonText(getResources().getString(R.string.lang_en_airplane_mode));
                break;
        }

        oopsNoInternetProps.setShowInternetOnButtons(true);
        oopsNoInternetProps.setCancelable(false);
        oopsNoInternetProps.setShowAirplaneModeOffButtons(true);
        oopsNoInternet.build();
    }

    /**
     * In-app updates
     * https://developer.android.com/guide/playcore/in-app-updates/kotlin-java#java
     */
    private void checkInAppUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            // This applies an immediate update. To apply a flexible update instead, pass in AppUpdateType.FLEXIBLE
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Request the update.
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                            AppUpdateType.IMMEDIATE,
                            // The current activity making the update request.
                            this,
                            // Include a request code to later monitor this update request.
                            INAPP_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "SendIntentException", e);
                }
            }
            // Handle update in progress
            else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // If an in-app update is already running, resume the update.
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            INAPP_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "SendIntentException", e);
                }
            }
        });
    }

    /**
     * Detect a phone call and handle kill switch
     */
    public void detectPhoneCall() {
        // Detect telephone call
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        // Check if the phone is already on a call
        if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
            // Call the JS kill switch method
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Log.d(TAG, jScript);
                    mWebView.evaluateJavascript(JS_killSwitchHandler, null);
                }
            });
        }
        // Listen for calls
        else {
            telephonyManager.listen(new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    switch (state) {
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            // Call the JS kill switch method
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Log.d(TAG, jScript);
                                    mWebView.evaluateJavascript(JS_killSwitchHandler, null);
                                }
                            });
                            break;
                    }
                    super.onCallStateChanged(state, incomingNumber);
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void showAlert(String title, String message, String button, int style, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(new ContextThemeWrapper(ActivityWebView.this, style))
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(button, onClickListener)
                .create()
                .show();
    }
}