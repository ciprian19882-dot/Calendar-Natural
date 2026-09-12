package com.naturalcalendar.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
  private static final int REQ_LOCATION=1001, REQ_NOTIFICATIONS=1002;
  private WebView web; private GeolocationPermissions.Callback geoCallback; private String geoOrigin;
  private final SimpleDateFormat isoFmt=new SimpleDateFormat("yyyy-MM-dd", Locale.US);

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    web=new WebView(this); setContentView(web);
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setGeolocationEnabled(true);
    web.addJavascriptInterface(new NaturalBridge(), "NaturalBridge");
    web.setWebViewClient(new WebViewClient(){
      @Override public void onPageFinished(WebView view,String url){
        super.onPageFinished(view,url);
        String js="(function(){function p(){document.querySelectorAll('.eventIcon').forEach(function(e){var t=e.textContent.trim();if(t==='●')e.textContent='🌑';else if(t==='○')e.textContent='🌕';else if(t==='◐')e.textContent='🌓';else if(t==='◑')e.textContent='🌗';});document.querySelectorAll('.legend span').forEach(function(e){e.textContent=e.textContent.replace(/^● /,'🌑 ').replace(/^○ /,'🌕 ').replace(/^◐ /,'🌓 ').replace(/^◑ /,'🌗 ');});}p();new MutationObserver(p).observe(document.body,{childList:true,subtree:true});})();";
        view.evaluateJavascript(js,null);
      }
    });
    web.setWebChromeClient(new WebChromeClient(){
      @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) callback.invoke(origin,true,false);
        else { geoOrigin=origin; geoCallback=callback; requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION); }
      }
    });
    web.loadUrl("file:///android_asset/index.html");
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);
    scheduleDailyNotificationCheck(); refreshWidgets();
  }

  private Date parseIso(String s){ try { synchronized(isoFmt){ return isoFmt.parse(s); } } catch(ParseException e){ return new Date(); } }
  private String iso(Date d){ synchronized(isoFmt){ return isoFmt.format(d); } }
  private static long daysBetween(Date a,Date b){ return Math.round((AstronomyEngine.localMidnight(b).getTime()-AstronomyEngine.localMidnight(a).getTime())/(double)AstronomyEngine.DAY_MS); }
  private static String moonSymbolForName(String name){
    if("New Moon".equals(name)) return "🌑";
    if("Waxing Crescent".equals(name)) return "🌒";
    if("First Quarter".equals(name)) return "🌓";
    if("Waxing Gibbous".equals(name)) return "🌔";
    if("Full Moon".equals(name)) return "🌕";
    if("Waning Gibbous".equals(name)) return "🌖";
    if("Last Quarter".equals(name)) return "🌗";
    if("Waning Crescent".equals(name)) return "🌘";
    return "🌙";
  }

  private class NaturalBridge {
    @JavascriptInterface public String version(){ return "4.1"; }
    @JavascriptInterface public String getInfo(String dateIso){
      try{
        Date d=parseIso(dateIso); AstronomyEngine.Info i=AstronomyEngine.info(d); JSONObject o=new JSONObject();
        o.put("date",iso(i.date)); o.put("naturalMonth",i.naturalMonth); o.put("naturalDay",i.naturalDay); o.put("naturalYearStart",i.naturalYearStart);
        o.put("season",i.season); o.put("phase",i.phaseName); o.put("phaseSymbol",moonSymbolForName(i.phaseName)); o.put("moonAge",i.moonAgeDays); o.put("illumination",i.illumination);
        o.put("previousNew",iso(i.previousNew)); o.put("nextNew",iso(i.nextNew));
        o.put("nextFullInstant",i.nextFull.getTime()); o.put("nextPhaseInstant",i.nextPrimaryPhase.getTime()); o.put("nextPhaseName",i.nextPrimaryPhaseName); o.put("nextPhaseSymbol",moonSymbolForName(i.nextPrimaryPhaseName));
        o.put("nextSeasonInstant",i.nextSeason.getTime()); o.put("nextSeasonName",i.nextSeasonName); o.put("nextSeasonSymbol",i.nextSeasonSymbol);
        return o.toString();
      }catch(Exception e){ return "{\"error\":\"Unable to calculate date\"}"; }
    }

    @JavascriptInterface public String getLunarMonth(String anchorIso,int shift){
      try{
        Date anchor=parseIso(anchorIso); long k=AstronomyEngine.previousNewLunation(anchor)+shift;
        Date start=AstronomyEngine.localMidnight(AstronomyEngine.moonPhaseInstant(k,0)); Date end=AstronomyEngine.localMidnight(AstronomyEngine.moonPhaseInstant(k+1,0));
        Date representative=new Date(start.getTime()+Math.max(1,(end.getTime()-start.getTime())/2)); AstronomyEngine.Info ri=AstronomyEngine.info(representative);
        JSONObject o=new JSONObject(); o.put("start",iso(start)); o.put("end",iso(end)); o.put("month",ri.naturalMonth); o.put("naturalYearStart",ri.naturalYearStart);
        JSONArray days=new JSONArray();
        for(Date cur=start;cur.before(end);cur=new Date(cur.getTime()+AstronomyEngine.DAY_MS)){
          AstronomyEngine.Info inf=AstronomyEngine.info(cur); JSONObject x=new JSONObject(); x.put("date",iso(cur)); x.put("day",(int)daysBetween(start,cur)+1); x.put("season",inf.season);
          List<AstronomyEngine.Event> es=AstronomyEngine.eventsOnCivilDate(cur); JSONArray ev=new JSONArray(); for(AstronomyEngine.Event e:es){ JSONObject q=new JSONObject(); q.put("name",e.name); q.put("symbol",e.type.equals("season")?e.symbol:moonSymbolForName(e.name)); q.put("type",e.type); q.put("instant",e.instant.getTime()); ev.put(q); }
          x.put("events",ev); days.put(x);
        }
        o.put("days",days); return o.toString();
      }catch(Exception e){ return "{\"error\":\"Unable to build lunar month\"}"; }
    }

    @JavascriptInterface public String convertNatural(int year,int month,int day){
      try{ Date d=AstronomyEngine.naturalToGregorian(year,month,day); if(d==null) return "{\"error\":\"That day does not exist in this lunar month\"}"; JSONObject o=new JSONObject(); o.put("date",iso(d)); return o.toString(); }
      catch(Exception e){ return "{\"error\":\"Unable to convert\"}"; }
    }

    @JavascriptInterface public String eventsForYear(int year){
      try{ JSONArray a=new JSONArray(); String[] n={"Spring Equinox","Summer Solstice","Autumn Equinox","Winter Solstice"}; String[] s={"🌱","☀","🍂","❄"}; for(int q=0;q<4;q++){ JSONObject o=new JSONObject(); Date d=AstronomyEngine.seasonInstant(year,q); o.put("name",n[q]); o.put("symbol",s[q]); o.put("instant",d.getTime()); a.put(o);} return a.toString(); }
      catch(Exception e){ return "[]"; }
    }
  }

  private void scheduleDailyNotificationCheck(){
    AlarmManager alarm=(AlarmManager)getSystemService(Context.ALARM_SERVICE); Intent i=new Intent(this,NaturalEventReceiver.class);
    PendingIntent pi=PendingIntent.getBroadcast(this,3107,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Calendar c=Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY,9); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); if(c.getTimeInMillis()<=System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR,1);
    alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);
  }

  private void refreshWidgets(){
    AppWidgetManager m=AppWidgetManager.getInstance(this); ComponentName c=new ComponentName(this,NaturalCalendarWidget.class); int[] ids=m.getAppWidgetIds(c); Intent i=new Intent(this,NaturalCalendarWidget.class); i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE); i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids); sendBroadcast(i);
  }

  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){ super.onRequestPermissionsResult(requestCode,permissions,grantResults); if(requestCode==REQ_LOCATION&&geoCallback!=null){ boolean allowed=false; for(int r:grantResults) if(r==PackageManager.PERMISSION_GRANTED) allowed=true; geoCallback.invoke(geoOrigin,allowed,false); geoCallback=null; geoOrigin=null; } }
  @Override public void onBackPressed(){ if(web!=null&&web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
