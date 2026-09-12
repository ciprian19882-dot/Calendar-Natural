package com.naturalcalendar.app;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NaturalCalendarWidget extends AppWidgetProvider {
  @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids){
    for(int id:ids) updateWidget(context,manager,id);
  }

  @Override public void onReceive(Context context, Intent intent){
    super.onReceive(context,intent);
    String a=intent.getAction();
    if(Intent.ACTION_DATE_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a) ||
       Intent.ACTION_TIME_CHANGED.equals(a) || Intent.ACTION_TIME_TICK.equals(a)){
      AppWidgetManager m=AppWidgetManager.getInstance(context);
      int[] ids=m.getAppWidgetIds(new android.content.ComponentName(context,NaturalCalendarWidget.class));
      for(int id:ids) updateWidget(context,m,id);
    }
  }

  private static final class MoonLive {
    String name, symbol;
    int illuminationPct;
  }

  private static MoonLive moonLive(Date now){
    long k=AstronomyEngine.previousNewLunation(now);
    Date prev=AstronomyEngine.moonPhaseInstant(k,0.0);
    if(prev.after(now)){ k--; prev=AstronomyEngine.moonPhaseInstant(k,0.0); }
    Date next=AstronomyEngine.moonPhaseInstant(k+1,0.0);
    while(!next.after(now)){
      k++; prev=next; next=AstronomyEngine.moonPhaseInstant(k+1,0.0);
    }
    double f=(now.getTime()-prev.getTime())/(double)(next.getTime()-prev.getTime());
    f=Math.max(0.0,Math.min(0.999999,f));
    double illum=(1.0-Math.cos(2.0*Math.PI*f))/2.0;
    MoonLive m=new MoonLive();
    m.illuminationPct=(int)Math.round(illum*100.0);
    if(f<0.0625){ m.name="New Moon"; m.symbol="●"; }
    else if(f<0.1875){ m.name="Waxing Crescent"; m.symbol="◔"; }
    else if(f<0.3125){ m.name="First Quarter"; m.symbol="◐"; }
    else if(f<0.4375){ m.name="Waxing Gibbous"; m.symbol="◕"; }
    else if(f<0.5625){ m.name="Full Moon"; m.symbol="○"; }
    else if(f<0.6875){ m.name="Waning Gibbous"; m.symbol="◕"; }
    else if(f<0.8125){ m.name="Last Quarter"; m.symbol="◑"; }
    else { m.name="Waning Crescent"; m.symbol="◔"; }
    return m;
  }

  private static Location lastLocation(Context context){
    if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED &&
       context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) return null;
    try{
      LocationManager lm=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
      Location best=null;
      List<String> providers=lm.getProviders(true);
      for(String p:providers){
        Location x=lm.getLastKnownLocation(p);
        if(x!=null && (best==null || x.getTime()>best.getTime())) best=x;
      }
      return best;
    }catch(Exception ignored){ return null; }
  }

  private static double norm360(double x){ x%=360.0; return x<0?x+360.0:x; }
  private static double sinD(double x){ return Math.sin(Math.toRadians(x)); }
  private static double cosD(double x){ return Math.cos(Math.toRadians(x)); }

  private static double solarAltitude(Date now,double lat,double lon){
    double jd=2440587.5 + now.getTime()/86400000.0;
    double t=(jd-2451545.0)/36525.0;
    double L0=norm360(280.46646+t*(36000.76983+t*0.0003032));
    double M=357.52911+t*(35999.05029-0.0001537*t);
    double e=0.016708634-t*(0.000042037+0.0000001267*t);
    double C=sinD(M)*(1.914602-t*(0.004817+0.000014*t)) + sinD(2*M)*(0.019993-0.000101*t) + sinD(3*M)*0.000289;
    double trueLong=L0+C;
    double omega=125.04-1934.136*t;
    double lambda=trueLong-0.00569-0.00478*sinD(omega);
    double sec=21.448-t*(46.815+t*(0.00059-t*0.001813));
    double meanObliq=23.0+(26.0+sec/60.0)/60.0;
    double obliq=meanObliq+0.00256*cosD(omega);
    double decl=Math.toDegrees(Math.asin(sinD(obliq)*sinD(lambda)));
    double y=Math.tan(Math.toRadians(obliq/2.0)); y*=y;
    double eqTime=4.0*Math.toDegrees(y*Math.sin(2*Math.toRadians(L0)) - 2*e*Math.sin(Math.toRadians(M)) + 4*e*y*Math.sin(Math.toRadians(M))*Math.cos(2*Math.toRadians(L0)) - 0.5*y*y*Math.sin(4*Math.toRadians(L0)) - 1.25*e*e*Math.sin(2*Math.toRadians(M)));
    java.util.Calendar c=java.util.Calendar.getInstance(); c.setTime(now);
    double minutes=c.get(java.util.Calendar.HOUR_OF_DAY)*60.0+c.get(java.util.Calendar.MINUTE)+c.get(java.util.Calendar.SECOND)/60.0;
    double tzHours=TimeZone.getDefault().getOffset(now.getTime())/3600000.0;
    double tst=(minutes+eqTime+4.0*lon-60.0*tzHours)%1440.0; if(tst<0)tst+=1440.0;
    double ha=tst/4.0-180.0; if(ha<-180)ha+=360.0;
    double cosZen=sinD(lat)*sinD(decl)+cosD(lat)*cosD(decl)*cosD(ha);
    cosZen=Math.max(-1.0,Math.min(1.0,cosZen));
    return 90.0-Math.toDegrees(Math.acos(cosZen));
  }

  private static String sunStatus(Context context,Date now){
    Location loc=lastLocation(context);
    if(loc==null) return "☀  Sun · open app for location";
    double alt=solarAltitude(now,loc.getLatitude(),loc.getLongitude());
    java.util.Calendar c=java.util.Calendar.getInstance(); c.setTime(now);
    boolean beforeNoon=c.get(java.util.Calendar.HOUR_OF_DAY)<12;
    String state, icon;
    if(alt<-18){ state="Night"; icon="✦"; }
    else if(alt<-6){ state=beforeNoon?"Astronomical Dawn":"Astronomical Dusk"; icon=beforeNoon?"◒":"◓"; }
    else if(alt<0){ state=beforeNoon?"Dawn":"Dusk"; icon=beforeNoon?"🌅":"🌇"; }
    else if(alt<10){ state=beforeNoon?"Sunrise light":"Sunset light"; icon=beforeNoon?"🌅":"🌇"; }
    else { state="Daylight"; icon="☀"; }
    return icon+"  "+state+" · "+Math.round(alt)+"°";
  }

  private static void updateWidget(Context context,AppWidgetManager manager,int id){
    Date now=new Date();
    AstronomyEngine.Info i=AstronomyEngine.info(now);
    MoonLive moon=moonLive(now);
    RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.natural_calendar_widget);
    v.setTextViewText(R.id.widgetNaturalDate,"Month "+i.naturalMonth+" · Day "+i.naturalDay);
    v.setTextViewText(R.id.widgetMoonStatus,moon.symbol+"  "+moon.name+" · "+moon.illuminationPct+"%");
    v.setTextViewText(R.id.widgetSunStatus,sunStatus(context,now));
    v.setTextViewText(R.id.widgetCivilDate,new SimpleDateFormat("EEE, d MMM · HH:mm",Locale.ENGLISH).format(now));
    Intent open=new Intent(context,MainActivity.class);
    PendingIntent pi=PendingIntent.getActivity(context,7,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    v.setOnClickPendingIntent(R.id.widgetRoot,pi);
    manager.updateAppWidget(id,v);
  }
}
