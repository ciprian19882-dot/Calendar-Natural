package com.naturalcalendar.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NaturalCalendarWidget extends AppWidgetProvider {
  private static final double SYN = 29.530588853;
  private static final long EPOCH = 947182440000L; // 2000-01-06 18:14 UTC

  @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids){
    for(int id: ids) updateWidget(context, manager, id);
  }

  private static void updateWidget(Context context, AppWidgetManager manager, int id){
    Date now = new Date();
    Calendar c = Calendar.getInstance();
    c.setTime(now);
    int year = c.get(Calendar.YEAR);
    Calendar spring = Calendar.getInstance();
    spring.clear(); spring.set(year, Calendar.MARCH, 20, 0, 0, 0);
    if(c.before(spring)){ year--; spring.clear(); spring.set(year, Calendar.MARCH, 20, 0, 0, 0); }

    double daysSinceEpoch = (now.getTime() - EPOCH) / 86400000.0;
    double lunationsNow = Math.floor(daysSinceEpoch / SYN);
    long prevNewMs = EPOCH + Math.round(lunationsNow * SYN * 86400000.0);
    if(prevNewMs > now.getTime()) prevNewMs -= Math.round(SYN * 86400000.0);

    double springDays = (spring.getTimeInMillis() - EPOCH) / 86400000.0;
    double springLunation = Math.floor(springDays / SYN);
    long monthOneMs = EPOCH + Math.round(springLunation * SYN * 86400000.0);
    int month = (int)Math.round((prevNewMs - monthOneMs) / (SYN * 86400000.0)) + 1;
    if(month < 1) month = 1;
    int day = (int)TimeUnit.MILLISECONDS.toDays(now.getTime() - prevNewMs) + 1;
    double age = (now.getTime() - prevNewMs) / 86400000.0;
    String phase;
    String symbol;
    if(age < 2){ phase="New Moon"; symbol="●"; }
    else if(age < 8){ phase="Waxing Crescent"; symbol="◔"; }
    else if(age < 10){ phase="First Quarter"; symbol="◐"; }
    else if(age < 15){ phase="Waxing Gibbous"; symbol="◕"; }
    else if(age < 17){ phase="Full Moon"; symbol="○"; }
    else if(age < 23){ phase="Waning Gibbous"; symbol="◕"; }
    else if(age < 25){ phase="Last Quarter"; symbol="◑"; }
    else { phase="Waning Crescent"; symbol="◔"; }

    RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.natural_calendar_widget);
    v.setTextViewText(R.id.widgetNaturalDate, "Month " + month + " · Day " + day);
    v.setTextViewText(R.id.widgetPhase, symbol + "  " + phase);
    v.setTextViewText(R.id.widgetCivilDate, new SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(now));

    Intent open = new Intent(context, MainActivity.class);
    PendingIntent pi = PendingIntent.getActivity(context, 7, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    v.setOnClickPendingIntent(R.id.widgetRoot, pi);
    manager.updateAppWidget(id, v);
  }
}
