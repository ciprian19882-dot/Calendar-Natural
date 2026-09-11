package com.naturalcalendar.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NaturalEventReceiver extends BroadcastReceiver {
  private static final String CHANNEL_ID = "natural_events";

  @Override public void onReceive(Context context, Intent intent){
    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    Map<String,String[]> events = new HashMap<>();
    events.put("2026-09-11", new String[]{"New Moon tonight", "A new lunar cycle begins."});
    events.put("2026-09-23", new String[]{"Autumn Equinox", "A new astronomical season begins today."});
    events.put("2026-09-26", new String[]{"Full Moon is here", "The Moon reaches full illumination tonight."});
    events.put("2026-12-21", new String[]{"Winter Solstice", "The astronomical winter begins today."});
    events.put("2027-03-20", new String[]{"Spring Equinox", "The astronomical spring begins today."});
    events.put("2027-06-21", new String[]{"Summer Solstice", "The astronomical summer begins today."});
    events.put("2027-09-23", new String[]{"Autumn Equinox", "The astronomical autumn begins today."});
    events.put("2027-12-22", new String[]{"Winter Solstice", "The astronomical winter begins today."});

    String[] event = events.get(today);
    if(event == null) return;

    NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    if(Build.VERSION.SDK_INT >= 26){
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Natural events", NotificationManager.IMPORTANCE_DEFAULT);
      channel.setDescription("Moon phases, equinoxes and solstices");
      nm.createNotificationChannel(channel);
    }

    Intent open = new Intent(context, MainActivity.class);
    PendingIntent pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder = Build.VERSION.SDK_INT >= 26
        ? new Notification.Builder(context, CHANNEL_ID)
        : new Notification.Builder(context);
    builder.setSmallIcon(android.R.drawable.ic_menu_today)
        .setContentTitle(event[0])
        .setContentText(event[1])
        .setStyle(new Notification.BigTextStyle().bigText(event[1]))
        .setAutoCancel(true)
        .setContentIntent(pi);
    nm.notify(today.hashCode(), builder.build());
  }
}
