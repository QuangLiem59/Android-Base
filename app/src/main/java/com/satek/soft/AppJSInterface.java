package com.satek.soft;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class AppJSInterface {
    Context context;

    public AppJSInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void callPhone(String phone) {
        String url = "tel:" + phone;
        ((MainActivity)context).callPhone(url);
    }

    @JavascriptInterface
    public void getToken() {
        ((MainActivity)context).getFCMToken();
    }

    @JavascriptInterface
    public void subscribeTopic(String topic) {
        ((MainActivity)context).subscribeTopic(topic);
    }

    @JavascriptInterface
    public void unsubscribeTopic(String topic) {
        ((MainActivity)context).unsubscribeTopic(topic);
    }
}