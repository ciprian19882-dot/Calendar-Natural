package com.naturalcalendar.app;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class AstronomyEngine {
    public static final long DAY_MS = 86400000L;
    public static final double SYNODIC_MONTH = 29.530588853;
    private static final double NEW_MOON_EPOCH_JD = 2451550.09765;

    private AstronomyEngine() {}

    public static final class Event {
        public final String type, name, symbol;
        public final Date instant;
        public Event(String type, String name, String symbol, Date instant) {
            this.type=type; this.name=name; this.symbol=symbol; this.instant=instant;
        }
    }

    public static final class Info {
        public Date date, previousNew, nextNew, nextFull, nextPrimaryPhase, nextSeason;
        public String nextPrimaryPhaseName, nextPrimaryPhaseSymbol, nextSeasonName, nextSeasonSymbol;
        public int naturalMonth, naturalDay, naturalYearStart;
        public String season, phaseName, phaseSymbol;
        public double moonAgeDays, illumination;
    }

    private static double sinDeg(double d){ return Math.sin(Math.toRadians(d)); }
    private static double cosDeg(double d){ return Math.cos(Math.toRadians(d)); }
    private static double mod360(double d){ d%=360.0; return d<0?d+360.0:d; }

    public static Date localMidnight(Date d){
        Calendar c=Calendar.getInstance(); c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        return c.getTime();
    }

    public static String iso(Date d){ return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d); }
    private static double toJulian(Date d){ return 2440587.5 + d.getTime()/86400000.0; }
    private static Date fromJulian(double jd){ return new Date(Math.round((jd-2440587.5)*86400000.0)); }

    private static double deltaTSeconds(double year){
        if(year < 1920){ double t=year-1900; return -2.79+1.494119*t-0.0598939*t*t+0.0061966*t*t*t-0.000197*t*t*t*t; }
        if(year < 1941){ double t=year-1920; return 21.20+0.84493*t-0.076100*t*t+0.0020936*t*t*t; }
        if(year < 1961){ double t=year-1950; return 29.07+0.407*t-t*t/233.0+t*t*t/2547.0; }
        if(year < 1986){ double t=year-1975; return 45.45+1.067*t-t*t/260.0-t*t*t/718.0; }
        if(year < 2005){ double t=year-2000; return 63.86+0.3345*t-0.060374*t*t+0.0017275*t*t*t+0.000651814*t*t*t*t+0.00002373599*t*t*t*t*t; }
        if(year < 2050){ double t=year-2000; return 62.92+0.32217*t+0.005589*t*t; }
        double u=(year-1820)/100.0; return -20+32*u*u-0.5628*(2150-year);
    }

    private static double phaseJde(double k, double phase){
        double kp=k+phase;
        double T=kp/1236.85, T2=T*T, T3=T2*T, T4=T3*T;
        double E=1-0.002516*T-0.0000074*T2;
        double M=mod360(2.5534+29.10535670*kp-0.0000014*T2-0.00000011*T3);
        double Mp=mod360(201.5643+385.81693528*kp+0.0107582*T2+0.00001238*T3-0.000000058*T4);
        double F=mod360(160.7108+390.67050284*kp-0.0016118*T2-0.00000227*T3+0.000000011*T4);
        double O=mod360(124.7746-1.56375588*kp+0.0020672*T2+0.00000215*T3);
        double jde=NEW_MOON_EPOCH_JD+SYNODIC_MONTH*kp+0.0001337*T2-0.000000150*T3+0.00000000073*T4;
        double c;
        if(phase==0.0 || phase==0.5){
            boolean full=phase==0.5;
            c=(full?-0.40614:-0.40720)*sinDeg(Mp)+(full?0.17302:0.17241)*E*sinDeg(M)
              +(full?0.01614:0.01608)*sinDeg(2*Mp)+(full?0.01043:0.01039)*sinDeg(2*F)
              +(full?0.00734:0.00739)*E*sinDeg(Mp-M)-0.00515*E*sinDeg(Mp+M)
              +0.00209*E*E*sinDeg(2*M)-0.00111*sinDeg(Mp-2*F)-0.00057*sinDeg(Mp+2*F)
              +0.00056*E*sinDeg(2*Mp+M)-0.00042*sinDeg(3*Mp)+0.00042*E*sinDeg(M+2*F)
              +0.00038*E*sinDeg(M-2*F)-0.00024*E*sinDeg(2*Mp-M)-0.00017*sinDeg(O)
              -0.00007*sinDeg(Mp+2*M)+0.00004*sinDeg(2*Mp-2*F)+0.00004*sinDeg(3*M)
              +0.00003*sinDeg(Mp+M-2*F)+0.00003*sinDeg(2*Mp+2*F)-0.00003*sinDeg(Mp+M+2*F)
              +0.00003*sinDeg(Mp-M+2*F)-0.00002*sinDeg(Mp-M-2*F)-0.00002*sinDeg(3*Mp+M)+0.00002*sinDeg(4*Mp);
        } else {
            c=-0.62801*sinDeg(Mp)+0.17172*E*sinDeg(M)-0.01183*E*sinDeg(Mp+M)+0.00862*sinDeg(2*Mp)
              +0.00804*sinDeg(2*F)+0.00454*E*sinDeg(Mp-M)+0.00204*E*E*sinDeg(2*M)-0.00180*sinDeg(Mp-2*F)
              -0.00070*sinDeg(Mp+2*F)-0.00040*sinDeg(3*Mp)-0.00034*E*sinDeg(2*Mp-M)+0.00032*E*sinDeg(M+2*F)
              +0.00032*E*sinDeg(M-2*F)-0.00028*E*E*sinDeg(Mp+2*M)+0.00027*E*sinDeg(2*Mp+M)-0.00017*sinDeg(O)
              -0.00005*sinDeg(Mp-M-2*F)+0.00004*sinDeg(2*Mp+2*F)-0.00004*sinDeg(Mp+M+2*F)
              +0.00004*sinDeg(Mp-2*M)+0.00003*sinDeg(Mp+M-2*F)+0.00003*sinDeg(3*Mp+M)
              +0.00002*sinDeg(2*Mp-2*F)+0.00002*sinDeg(Mp-M+2*F)-0.00002*sinDeg(3*Mp-M);
            double W=0.00306-0.00038*E*cosDeg(M)+0.00026*cosDeg(Mp)-0.00002*cosDeg(Mp-M)+0.00002*cosDeg(Mp+M)+0.00002*cosDeg(2*F);
            c += phase==0.25 ? W : -W;
        }
        double A1=299.77+0.107408*kp-0.009173*T2, A2=251.88+0.016321*kp, A3=251.83+26.651886*kp;
        double A4=349.42+36.412478*kp, A5=84.66+18.206239*kp, A6=141.74+53.303771*kp;
        double A7=207.14+2.453732*kp, A8=154.84+7.306860*kp, A9=34.52+27.261239*kp;
        double A10=207.19+0.121824*kp, A11=291.34+1.844379*kp, A12=161.72+24.198154*kp;
        double A13=239.56+25.513099*kp, A14=331.55+3.592518*kp;
        c+=0.000325*sinDeg(A1)+0.000165*sinDeg(A2)+0.000164*sinDeg(A3)+0.000126*sinDeg(A4)+0.000110*sinDeg(A5)
          +0.000062*sinDeg(A6)+0.000060*sinDeg(A7)+0.000056*sinDeg(A8)+0.000047*sinDeg(A9)+0.000042*sinDeg(A10)
          +0.000040*sinDeg(A11)+0.000037*sinDeg(A12)+0.000035*sinDeg(A13)+0.000023*sinDeg(A14);
        double approxYear=2000+kp/12.3685;
        return jde+c-deltaTSeconds(approxYear)/86400.0;
    }

    public static Date moonPhaseInstant(long lunation, double phase){ return fromJulian(phaseJde(lunation,phase)); }
    private static long approximateLunation(Date d){ return Math.round((toJulian(d)-NEW_MOON_EPOCH_JD)/SYNODIC_MONTH); }

    public static long previousNewLunation(Date d){
        Date target=localMidnight(d); long k=approximateLunation(target);
        while(localMidnight(moonPhaseInstant(k,0)).after(target)) k--;
        while(!localMidnight(moonPhaseInstant(k+1,0)).after(target)) k++;
        return k;
    }
    public static Date previousNew(Date d){ return localMidnight(moonPhaseInstant(previousNewLunation(d),0)); }
    public static Date nextNew(Date d){ return localMidnight(moonPhaseInstant(previousNewLunation(d)+1,0)); }

    private static final double[][] EQ_TERMS={
      {485,324.96,1934.136},{203,337.23,32964.467},{199,342.08,20.186},{182,27.85,445267.112},{156,73.14,45036.886},{136,171.52,22518.443},
      {77,222.54,65928.934},{74,296.72,3034.906},{70,243.58,9037.513},{58,119.81,33718.147},{52,297.17,150.678},{50,21.02,2281.226},
      {45,247.54,29929.562},{44,325.15,31555.956},{29,60.93,4443.417},{18,155.12,67555.328},{17,288.79,4562.452},{16,198.04,62894.029},
      {14,199.76,31436.921},{12,95.39,14577.848},{12,287.11,31931.756},{12,320.81,34777.259},{9,227.73,1222.114},{8,15.45,16859.074}
    };

    public static Date seasonInstant(int year, int which){
        double Y=(year-2000)/1000.0, Y2=Y*Y, Y3=Y2*Y, Y4=Y3*Y;
        double jde0;
        if(which==0) jde0=2451623.80984+365242.37404*Y+0.05169*Y2-0.00411*Y3-0.00057*Y4;
        else if(which==1) jde0=2451716.56767+365241.62603*Y+0.00325*Y2+0.00888*Y3-0.00030*Y4;
        else if(which==2) jde0=2451810.21715+365242.01767*Y-0.11575*Y2+0.00337*Y3+0.00078*Y4;
        else jde0=2451900.05952+365242.74049*Y-0.06223*Y2-0.00823*Y3+0.00032*Y4;
        double T=(jde0-2451545.0)/36525.0;
        double W=35999.373*T-2.47;
        double dl=1+0.0334*cosDeg(W)+0.0007*cosDeg(2*W);
        double S=0; for(double[] a:EQ_TERMS) S+=a[0]*cosDeg(a[1]+a[2]*T);
        double jde=jde0+0.00001*S/dl;
        return fromJulian(jde-deltaTSeconds(year)/86400.0);
    }

    public static Date seasonCivilDate(int year, int which){ return localMidnight(seasonInstant(year,which)); }

    public static int naturalYearStartYear(Date d){
        Calendar c=Calendar.getInstance(); c.setTime(d); int y=c.get(Calendar.YEAR);
        Date spring=seasonCivilDate(y,0);
        return localMidnight(d).before(spring)?y-1:y;
    }

    public static String season(Date d){
        Date day=localMidnight(d); Calendar c=Calendar.getInstance(); c.setTime(day); int y=c.get(Calendar.YEAR);
        Date sp=seasonCivilDate(y,0), su=seasonCivilDate(y,1), au=seasonCivilDate(y,2), wi=seasonCivilDate(y,3);
        if(day.before(sp)) return "Winter"; if(day.before(su)) return "Spring"; if(day.before(au)) return "Summer"; if(day.before(wi)) return "Autumn"; return "Winter";
    }

    public static Info info(Date d){
        Date day=localMidnight(d); long k=previousNewLunation(day);
        Date p=localMidnight(moonPhaseInstant(k,0)), n=localMidnight(moonPhaseInstant(k+1,0));
        int ny=naturalYearStartYear(day); Date spring=seasonCivilDate(ny,0); long m1=previousNewLunation(spring);
        Info i=new Info(); i.date=day; i.previousNew=p; i.nextNew=n; i.naturalYearStart=ny; i.naturalMonth=(int)(k-m1)+1;
        i.naturalDay=(int)((day.getTime()-p.getTime())/DAY_MS)+1; i.season=season(day);
        double age=(day.getTime()-p.getTime())/(double)DAY_MS; i.moonAgeDays=age;
        i.illumination=(1-Math.cos(2*Math.PI*Math.min(age/SYNODIC_MONTH,1)))/2.0;
        Event nearest=phaseForDay(day,k); i.phaseName=nearest.name; i.phaseSymbol=nearest.symbol;
        Event nextP=nextPrimaryPhaseAfter(d); i.nextPrimaryPhase=nextP.instant; i.nextPrimaryPhaseName=nextP.name; i.nextPrimaryPhaseSymbol=nextP.symbol;
        Event nextS=nextSeasonEventAfter(d); i.nextSeason=nextS.instant; i.nextSeasonName=nextS.name; i.nextSeasonSymbol=nextS.symbol;
        i.nextFull=nextPhaseAfter(d,0.5).instant;
        return i;
    }

    private static Event phaseForDay(Date day,long k){
        Event[] es={event(k,0),event(k,0.25),event(k,0.5),event(k,0.75),event(k+1,0)};
        for(Event e:es) if(localMidnight(e.instant).equals(day)) return e;
        double age=(day.getTime()-localMidnight(moonPhaseInstant(k,0)).getTime())/(double)DAY_MS;
        if(age<7.4) return new Event("intermediate","Waxing Crescent","◔",day);
        if(age<14.8) return new Event("intermediate","Waxing Gibbous","◕",day);
        if(age<22.1) return new Event("intermediate","Waning Gibbous","◕",day);
        return new Event("intermediate","Waning Crescent","◔",day);
    }

    private static Event event(long k,double phase){
        String n,s,t;
        if(phase==0){n="New Moon";s="●";t="new_moon";} else if(phase==0.25){n="First Quarter";s="◐";t="first_quarter";}
        else if(phase==0.5){n="Full Moon";s="○";t="full_moon";} else {n="Last Quarter";s="◑";t="last_quarter";}
        return new Event(t,n,s,moonPhaseInstant(k,phase));
    }

    private static Event nextPhaseAfter(Date d,double phase){
        long base=approximateLunation(d)-1; Event best=null;
        for(long k=base;k<=base+3;k++){ Event e=event(k,phase); if(e.instant.after(d)&&(best==null||e.instant.before(best.instant))) best=e; }
        return best;
    }

    public static Event nextPrimaryPhaseAfter(Date d){
        long base=approximateLunation(d)-1; Event best=null;
        for(long k=base;k<=base+2;k++) for(double p:new double[]{0,0.25,0.5,0.75}){ Event e=event(k,p); if(e.instant.after(d)&&(best==null||e.instant.before(best.instant))) best=e; }
        return best;
    }

    public static Event nextSeasonEventAfter(Date d){
        Calendar c=Calendar.getInstance(); c.setTime(d); int y=c.get(Calendar.YEAR); Event best=null;
        String[] names={"Spring Equinox","Summer Solstice","Autumn Equinox","Winter Solstice"}; String[] syms={"🌱","☀","🍂","❄"};
        for(int yy=y;yy<=y+1;yy++) for(int q=0;q<4;q++){ Date x=seasonInstant(yy,q); if(x.after(d)){ Event e=new Event("season",names[q],syms[q],x); if(best==null||x.before(best.instant)) best=e; } }
        return best;
    }

    public static List<Event> eventsOnCivilDate(Date d){
        Date day=localMidnight(d); List<Event> out=new ArrayList<>(); long k=approximateLunation(day);
        for(long kk=k-1;kk<=k+1;kk++) for(double p:new double[]{0,0.25,0.5,0.75}){ Event e=event(kk,p); if(localMidnight(e.instant).equals(day)) out.add(e); }
        Calendar c=Calendar.getInstance(); c.setTime(day); int y=c.get(Calendar.YEAR);
        String[] n={"Spring Equinox","Summer Solstice","Autumn Equinox","Winter Solstice"}; String[] s={"🌱","☀","🍂","❄"};
        for(int q=0;q<4;q++){ Date x=seasonInstant(y,q); if(localMidnight(x).equals(day)) out.add(new Event("season",n[q],s[q],x)); }
        return out;
    }

    public static Date naturalToGregorian(int naturalYear,int month,int day){
        Date spring=seasonCivilDate(naturalYear,0); long m1=previousNewLunation(spring); long k=m1+month-1;
        Date start=localMidnight(moonPhaseInstant(k,0)), next=localMidnight(moonPhaseInstant(k+1,0));
        Date target=new Date(start.getTime()+(day-1L)*DAY_MS);
        if(day<1 || !target.before(next)) return null;
        return target;
    }
}
