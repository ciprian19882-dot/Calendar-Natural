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
import java.util.List;
import java.util.Locale;

public class NaturalEventReceiver extends BroadcastReceiver {
  private static final String CHANNEL_ID="natural_events";

  @Override public void onReceive(Context context,Intent intent){
    List<AstronomyEngine.Event> events=AstronomyEngine.eventsOnCivilDate(new Date());
    if(events.isEmpty()) return;

    NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    if(Build.VERSION.SDK_INT>=26){
      NotificationChannel ch=new NotificationChannel(CHANNEL_ID,"Natural events",NotificationManager.IMPORTANCE_DEFAULT);
      ch.setDescription("Moon phases, equinoxes and solstices");
      nm.createNotificationChannel(ch);
    }

    Intent open=new Intent(context,MainActivity.class);
    PendingIntent pi=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    SimpleDateFormat time=new SimpleDateFormat("HH:mm",Locale.getDefault());
    int n=0;

    for(AstronomyEngine.Event e:events){
      String title=symbolFor(e)+"  "+e.name;
      String body=messageFor(e)+" · "+time.format(e.instant);
      Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL_ID):new Notification.Builder(context);
      b.setSmallIcon(android.R.drawable.ic_menu_today)
       .setContentTitle(title)
       .setContentText(body)
       .setStyle(new Notification.BigTextStyle().bigText(body))
       .setAutoCancel(true)
       .setContentIntent(pi);
      nm.notify((AstronomyEngine.iso(new Date())+e.type).hashCode()+n++,b.build());
    }
  }

  private static String symbolFor(AstronomyEngine.Event e){
    String n=e.name;
    if("New Moon".equals(n)) return "🌑";
    if("First Quarter".equals(n)) return "🌓";
    if("Full Moon".equals(n)) return "🌕";
    if("Last Quarter".equals(n)) return "🌗";
    return e.symbol;
  }

  private static String messageFor(AstronomyEngine.Event e){
    String n=e.name.toLowerCase(Locale.US);
    if(n.contains("new moon")) return "A new lunar cycle begins today";
    if(n.contains("full moon")) return "The Moon reaches full illumination today";
    if(n.contains("first quarter")) return "The Moon reaches First Quarter today";
    if(n.contains("last quarter")) return "The Moon reaches Last Quarter today";
    if(n.contains("spring equinox")) return "Astronomical spring begins today";
    if(n.contains("summer solstice")) return "Astronomical summer begins today";
    if(n.contains("autumn equinox")) return "Astronomical autumn begins today";
    if(n.contains("winter solstice")) return "Astronomical winter begins today";
    return "A natural astronomical milestone occurs today";
  }
}
