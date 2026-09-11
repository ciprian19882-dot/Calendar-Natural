package com.naturalcalendar.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NaturalCalendarWidget extends AppWidgetProvider {
  private static final double SYN = 29.530588853;
  private static final long DAY = 86400000L;
  private static final long EPOCH = 947182440000L; // 2000-01-06 18:14 UTC

  // Same stored astronomical New Moon civil dates used by the in-app calendar.
  private static final String[] EXACT_NEW = {
      "1982-02-23","1982-03-25","1982-04-23","1982-05-23","1982-06-21","1982-07-21","1982-08-19","1982-09-18",
      "2026-03-19","2026-04-17","2026-05-16","2026-06-15","2026-07-14","2026-08-12","2026-09-11","2026-10-10","2026-11-09","2026-12-09",
      "2027-01-07","2027-02-06","2027-03-08","2027-04-07","2027-05-06","2027-06-04","2027-07-04","2027-08-02","2027-08-31","2027-09-30","2027-10-29","2027-11-28","2027-12-27",
      "2028-01-26","2028-02-25","2028-03-26"
  };

  @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids){
    for(int id: ids) updateWidget(context, manager, id);
  }

  @Override public void onEnabled(Context context){
    super.onEnabled(context);
  }

  private static Date localMidnight(Date d){
    Calendar c = Calendar.getInstance();
    c.setTime(d);
    c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
    return c.getTime();
  }

  private static List<Date> exactNewDates(){
    List<Date> out = new ArrayList<>();
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    f.setLenient(false);
    for(String s: EXACT_NEW){ try { out.add(f.parse(s)); } catch(ParseException ignored){} }
    Collections.sort(out);
    return out;
  }

  private static Date approximatePreviousNew(Date d){
    Calendar c = Calendar.getInstance(); c.setTime(d);
    long utcDay = Date.UTC(c.get(Calendar.YEAR) - 1900, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
    long k = (long)Math.floor((utcDay - EPOCH) / (SYN * DAY));
    return localMidnight(new Date(EPOCH + Math.round(k * SYN * DAY)));
  }

  private static Date previousNew(Date d){
    Date target = localMidnight(d), prev = null;
    for(Date x: exactNewDates()){
      Date lx = localMidnight(x);
      if(!lx.after(target)) prev = lx; else break;
    }
    if(prev != null && target.getTime() - prev.getTime() < 40L * DAY) return prev;
    return approximatePreviousNew(target);
  }

  private static Date naturalYearStart(Date d){
    Calendar c = Calendar.getInstance(); c.setTime(d);
    int y = c.get(Calendar.YEAR);
    Calendar spring = Calendar.getInstance(); spring.clear(); spring.set(y, Calendar.MARCH, 20, 0, 0, 0);
    if(localMidnight(d).before(spring.getTime())){ y--; spring.clear(); spring.set(y, Calendar.MARCH, 20, 0, 0, 0); }
    return spring.getTime();
  }

  private static void updateWidget(Context context, AppWidgetManager manager, int id){
    Date now = new Date();
    Date today = localMidnight(now);
    Date prevNew = previousNew(today);
    Date monthOne = previousNew(naturalYearStart(today));

    int month = (int)Math.round((prevNew.getTime() - monthOne.getTime()) / (SYN * DAY)) + 1;
    if(month < 1) month = 1;
    int day = (int)TimeUnit.MILLISECONDS.toDays(today.getTime() - prevNew.getTime()) + 1;
    double age = (today.getTime() - prevNew.getTime()) / (double)DAY;

    String phase, symbol;
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
