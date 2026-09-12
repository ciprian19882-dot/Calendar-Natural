package com.naturalcalendar.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NaturalCalendarWidget extends AppWidgetProvider {
  @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){ for(int id:ids) updateWidget(context,manager,id); }
  @Override public void onReceive(Context context,Intent intent){ super.onReceive(context,intent); String a=intent.getAction(); if(Intent.ACTION_DATE_CHANGED.equals(a)||Intent.ACTION_TIMEZONE_CHANGED.equals(a)||Intent.ACTION_TIME_CHANGED.equals(a)){ AppWidgetManager m=AppWidgetManager.getInstance(context); int[] ids=m.getAppWidgetIds(new android.content.ComponentName(context,NaturalCalendarWidget.class)); for(int id:ids) updateWidget(context,m,id); } }

  private static void updateWidget(Context context,AppWidgetManager manager,int id){
    Date now=new Date(); AstronomyEngine.Info i=AstronomyEngine.info(now);
    RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.natural_calendar_widget);
    v.setTextViewText(R.id.widgetNaturalDate,"Month "+i.naturalMonth+" · Day "+i.naturalDay);
    v.setTextViewText(R.id.widgetPhase,i.phaseSymbol+"  "+i.phaseName);
    v.setTextViewText(R.id.widgetCivilDate,new SimpleDateFormat("EEE, d MMM",Locale.ENGLISH).format(now));
    Intent open=new Intent(context,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(context,7,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); v.setOnClickPendingIntent(R.id.widgetRoot,pi);
    manager.updateAppWidget(id,v);
  }
}
