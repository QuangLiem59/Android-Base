package com.satek.soft;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public Context appContext;

    @SuppressLint("StaticFieldLeak")
    public static WebView browser;
    public RelativeLayout layoutWelcome;
    public RelativeLayout layoutNoNetwork;

    private ValueCallback<Uri[]> mFilePathCallback;
    private boolean browserLoaded = false;
    private String clickNotification = "";
    ConnectivityManager connectivityManager;
    BetterActivityResult<Intent, ActivityResult> activityForResult;
    final String DOMAIN = "https://point.satek.vn";

    private void requestPer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.POST_NOTIFICATIONS,
            }, 1);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_NETWORK_STATE
            }, 1);
        }
    }

    BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            boolean isNetwork = netInfo != null && netInfo.isConnectedOrConnecting();

            if (Objects.equals(intent.getAction(), "android.net.conn.CONNECTIVITY_CHANGE")) {
                if (isNetwork) {
                    runOnUiThread(() -> {
                        layoutNoNetwork.setVisibility(View.GONE);

                        if (!browserLoaded) {
                            createBrowser();
                        }
                    });
                } else {
                    runOnUiThread(() -> layoutNoNetwork.setVisibility(View.VISIBLE));
                }
            }
        }
    };

    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            runOnUiThread(() -> {
                layoutNoNetwork.setVisibility(View.GONE);
                if (!browserLoaded) {
                    createBrowser();
                }
            });
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            runOnUiThread(() -> layoutNoNetwork.setVisibility(View.VISIBLE));
        }
    };

    BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), MyPushNotificationService.ACTION_BROADCAST)) {
                String token = intent.getStringExtra(MyPushNotificationService.EXTRA_TOKEN);

                if (token != null) {
                    sendTokenToServer(token);
                }
            }
        }
    };

    private void registerWatchNetwork() {
        if (Build.VERSION.SDK_INT < 29) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(networkReceiver, intentFilter);
        } else {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    private void unregisterWatchNetwork() {
        if (Build.VERSION.SDK_INT < 29) {
            unregisterReceiver(networkReceiver);
        } else {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            Uri soundDefault = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            NotificationChannel channel = new NotificationChannel("default", getString(R.string.nc_title), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getString(R.string.nc_description));
            channel.setShowBadge(true);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(soundDefault, audioAttributes);

            notificationManager.createNotificationChannel(channel);
        }
    }

    public void runScript(String script) {
        if (!browserLoaded) {
            return;
        }

        final String code = "if ('_app' in window) {" + script + "}";
        browser.post(() -> browser.evaluateJavascript(code, value -> {}));
    }

    public void sendTokenToServer(final String token) {
        runScript("_app.updateFCMToken('" + token + "')");
    }

    public void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(appContext, getString(R.string.not_receive_fcm_token), Toast.LENGTH_SHORT).show();
                return;
            }

            String token = task.getResult();
            Log.d("Token", token);
            sendTokenToServer(token);
        });
    }

    public void subscribeTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic);
    }

    public void unsubscribeTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
    }

    public void callPhone(String url) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
        startActivity(intent);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void createBrowser() {
        browserLoaded = false;

        browser.getSettings().setLoadsImagesAutomatically(true);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        browser.getSettings().setAllowFileAccess(true);
        browser.getSettings().setMediaPlaybackRequiresUserGesture(false);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setGeolocationEnabled(true);
        browser.getSettings().setUseWideViewPort(true);
        browser.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        browser.addJavascriptInterface(new AppJSInterface(appContext), "Android");
        browser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {// >= 24
                String url = request.getUrl().toString();
                if (url.startsWith("tel:")) {
                    callPhone(url);
                    return true;
                } else if (!url.startsWith("http:") && !url.startsWith("https:")) {
                    Toast.makeText(appContext, getString(R.string.error_scheme_not_accept), Toast.LENGTH_SHORT).show();
                    return true;
                } else if (!url.startsWith(DOMAIN)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intent);
                    return true;
                }

                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (layoutWelcome.getVisibility() == View.VISIBLE) {
                    layoutWelcome.animate().alpha(0).setDuration(200).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            layoutWelcome.setVisibility(View.GONE);
                        }
                    });
                }

                if (clickNotification != null && !clickNotification.isEmpty()) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        runScript(clickNotification);
                        clickNotification = "";
                    }, 500);
                }

                browserLoaded = true;
            }
        });

        browser.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }

                mFilePathCallback = filePathCallback;

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");

                activityForResult.launch(chooserIntent, result -> {
                    Intent data = result.getData();
                    Uri[] results = null;
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (data != null) {
                            String dataString = data.getDataString();
                            if (dataString != null) {
                                results = new Uri[]{Uri.parse(dataString)};
                            }
                        }
                    }

                    mFilePathCallback.onReceiveValue(results);
                    mFilePathCallback = null;
                });

                return true;
            }
        });

        browser.loadUrl(DOMAIN);
    }

    private void processNotification(Bundle data) {
        String click = data.getString("click");
        if (click != null && !click.isEmpty()) {
            if (browserLoaded) {
                runScript(click);
            } else {
                clickNotification = click;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = this;
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activityForResult = BetterActivityResult.registerActivityForResult(this);

        browser = findViewById(R.id.browser);
        layoutWelcome = findViewById(R.id.layoutWelcome);
        layoutNoNetwork = findViewById(R.id.layoutNoNetwork);

        requestPer();
        createNotificationChannel();

        Bundle data = getIntent().getExtras();
        if (data != null) {
            processNotification(data);
        }

        getFCMToken();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle data = intent.getExtras();
        if (data != null) {
            processNotification(data);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter i = new IntentFilter();
        i.addAction(MyPushNotificationService.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, i);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerWatchNetwork();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterWatchNetwork();
    }

    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
    }

    @Override
    public void onBackPressed() {
        if (browser.canGoBack()) {
            browser.goBack();
        } else {
            super.onBackPressed();
        }
    }
}