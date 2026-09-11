package com.naturalcalendar.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Calendar;

public class MainActivity extends Activity {
  private static final int REQ_LOCATION = 1001;
  private static final int REQ_NOTIFICATIONS = 1002;
  private WebView web;
  private GeolocationPermissions.Callback geoCallback;
  private String geoOrigin;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    s.setGeolocationEnabled(true);
    web.setWebViewClient(new WebViewClient());
    web.setWebChromeClient(new WebChromeClient(){
      @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
           checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
          callback.invoke(origin, true, false);
        } else {
          geoOrigin = origin;
          geoCallback = callback;
          requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
      }
    });
    web.loadUrl("file:///android_asset/index.html");

    if(Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
    }
    scheduleDailyNotificationCheck();
  }

  private void scheduleDailyNotificationCheck(){
    AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    Intent i = new Intent(this, NaturalEventReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(this, 3107, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Calendar c = Calendar.getInstance();
    c.set(Calendar.HOUR_OF_DAY, 9);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    if(c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1);
    alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
  }

  @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if(requestCode == REQ_LOCATION && geoCallback != null){
      boolean allowed = false;
      for(int r : grantResults) if(r == PackageManager.PERMISSION_GRANTED) allowed = true;
      geoCallback.invoke(geoOrigin, allowed, false);
      geoCallback = null;
      geoOrigin = null;
    }
  }

  @Override public void onBackPressed(){
    if(web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
  }
}
