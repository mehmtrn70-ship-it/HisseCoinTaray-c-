package com.hissecointarayici.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class EarlyOpportunityActivity extends Activity {
    private LinearLayout results;
    private TextView status, historyStatus;
    private final ExecutorService workers = Executors.newFixedThreadPool(6);
    private volatile boolean running = false;
    private static final String PREFS = "early_opportunity_v2";
    private static final String HISTORY = "signals";
    private static final long DUPLICATE_WINDOW = 12L * 60L * 60L * 1000L;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,18,18,18);
        root.setBackgroundColor(Color.rgb(247,248,250));
        TextView title = new TextView(this);
        title.setText("ERKEN FIRSAT AVCISI");
        title.setTextSize(23); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        title.setTextColor(Color.rgb(25,35,50)); title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("Binance USDT • 15 dk + 1 saat • İlk 300 yüksek hacimli parite");
        info.setTextColor(Color.DKGRAY); info.setPadding(0,8,0,8);
        root.addView(info);
        historyStatus = new TextView(this);
        historyStatus.setTextColor(Color.rgb(35,95,75));
        historyStatus.setPadding(0,4,0,8);
        root.addView(historyStatus);
        updateHistoryLabel();
        Button scan = new Button(this); scan.setText("ADAYLARI TARA");
        root.addView(scan);
        status = new TextView(this); status.setText("Tarama başlatılmaya hazır.");
        status.setPadding(4,12,4,12); root.addView(status);
        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        scan.setOnClickListener(v -> startScan());
        startScan();
    }

    private void startScan() {
        if(running) return;
        running=true; results.removeAllViews();
        status.setText("Piyasa evreni ve geçmiş sinyal kayıtları alınıyor...");
        workers.execute(() -> {
            try {
                JSONArray tickers = getArray("https://api.binance.com/api/v3/ticker/24hr");
                HashMap<String,Double> livePrices = new HashMap<>();
                ArrayList<JSONObject> ranked = new ArrayList<>();
                for(int i=0;i<tickers.length();i++) {
                    JSONObject t=tickers.getJSONObject(i);
                    String symbol=t.optString("symbol");
                    double change=t.optDouble("priceChangePercent",0);
                    if(symbol.endsWith("USDT")) livePrices.put(symbol,t.optDouble("lastPrice",0));
                    double quoteVol=t.optDouble("quoteVolume",0);
                    String base = symbol.endsWith("USDT") ? symbol.substring(0, symbol.length()-4) : "";
                    if(symbol.endsWith("USDT") && !isStableBase(base) && !isMetalBase(base)
                        && !symbol.contains("UP") && !symbol.contains("DOWN")
                        && !symbol.contains("BULL") && !symbol.contains("BEAR")
                        && quoteVol>0 && change<=10.0 && change>=-20.0) {
                        ranked.add(t);
                    }
                }
                updateTrackedRecords(livePrices);
                ranked.sort((a,b)->Double.compare(b.optDouble("quoteVolume",0),a.optDouble("quoteVolume",0)));
                int count=Math.min(300,ranked.size());
                ArrayList<Future<Candidate>> futures=new ArrayList<>();
                for(int i=0;i<count;i++) {
                    final JSONObject t=ranked.get(i);
                    futures.add(workers.submit(() -> analyze(t)));
                }
                ArrayList<Candidate> found=new ArrayList<>();
                int failed=0;
                for(Future<Candidate> f:futures) {
                    try { Candidate c=f.get(40,TimeUnit.SECONDS); if(c!=null) found.add(c); }
                    catch(Exception e) { failed++; }
                }
                found.sort((a,b)->Double.compare(b.score,a.score));
                saveNewSignals(found);
                final int failures=failed, scanned=count;
                runOnUiThread(() -> {
                    status.setText("İncelenen: "+scanned+" • Analiz hatası: "+failures+" • Aday: "+found.size()
                        +"\nGünlük değişimi -%20 ile +%10 arasındaki pariteler incelendi; yatay hareket edenler de dahil. Skor tahmin garantisi değildir.");
                    int shown=Math.min(15,found.size());
                    if(shown==0) addMessage("Şu an filtreleri geçen aday bulunamadı. Daha sonra tekrar dene.");
                    for(int i=0;i<shown;i++) addCandidate(found.get(i),i+1);
                    updateHistoryLabel();
                    running=false;
                });
            } catch(Exception e) {
                runOnUiThread(() -> { status.setText("Veri alınamadı: "+safe(e.getMessage())); running=false; });
            }
        });
    }

    private boolean isStableBase(String b) {
        return Arrays.asList("USDT","USDC","BUSD","FDUSD","TUSD","USDP","DAI","USDE",
            "USDS","USDD","USTC","FRAX","PYUSD","EUR","EURI","AEUR","RLUSD","USD1",
            "USDJ","USDX","GUSD","LUSD","SUSD","USDG","RWUSD").contains(b);
    }
    private boolean isMetalBase(String b) { return Arrays.asList("XAUT","PAXG").contains(b); }

    private Candidate analyze(JSONObject ticker) throws Exception {
        String symbol=ticker.getString("symbol");
        JSONArray k15=getArray("https://api.binance.com/api/v3/klines?symbol="+symbol+"&interval=15m&limit=100");
        JSONArray k1h=getArray("https://api.binance.com/api/v3/klines?symbol="+symbol+"&interval=1h&limit=60");
        Series a=parse(k15), b=parse(k1h);
        if(a.close.size()<45 || b.close.size()<30) return null;
        int n=a.close.size();
        double last=a.close.get(n-1);
        double rsi15=rsi(a.close), rsi1h=rsi(b.close);
        double vol15=ratio(a.volume), vol1h=ratio(b.volume);
        double low=recentLow(a.low,40);
        double supportDistance=low>0?(last/low-1)*100:999;
        boolean nearSupport=supportDistance>=0 && supportDistance<=2.5;
        boolean hammer=isHammer(a,n-1);
        boolean engulf=isBullishEngulfing(a,n-1);
        boolean morning=isMorningStar(a,n-1);
        boolean divergence=positiveRsiDivergence(a.close);
        double resistance=highest(a.high,Math.max(0,n-21),n-1);
        double resistanceDistance=resistance>0?(resistance/last-1)*100:999;
        double momentumRecovery=(rsi15>=42 && rsi15<=62 && rsi15>rsi(a.close.subList(0,n-5)))?1:0;

        double score=0;
        if(nearSupport) score+=20;
        if(hammer) score+=14;
        if(engulf) score+=17;
        if(morning) score+=16;
        if(divergence) score+=18;
        if(vol15>=1.25) score+=Math.min(10,(vol15-1)*5);
        if(vol1h>=1.15) score+=Math.min(5,(vol1h-1)*3);
        if(momentumRecovery>0) score+=5;
        if(resistanceDistance>=0 && resistanceDistance<=1.5) score+=5;
        if(rsi15>75 || rsi1h>75) score-=8;
        score=Math.max(0,Math.min(100,score));

        StringBuilder reasons=new StringBuilder();
        if(nearSupport) reasons.append("Desteğe yakın • ");
        if(hammer) reasons.append("Çekiç • ");
        if(engulf) reasons.append("Boğa yutan • ");
        if(morning) reasons.append("Sabah yıldızı • ");
        if(divergence) reasons.append("Pozitif RSI uyumsuzluğu • ");
        if(vol15>=1.25) reasons.append("15dk hacim artışı • ");
        if(vol1h>=1.15) reasons.append("1s hacim artışı • ");
        if(momentumRecovery>0) reasons.append("Momentum toparlanıyor • ");
        if(reasons.length()==0) reasons.append("Belirgin dönüş teyidi yok; yalnızca izleme adayı");
        String category=score>=65?"ALIM BÖLGESİ ADAYI":score>=40?"İZLEME LİSTESİ":"ZAYIF ADAY";
        return new Candidate(symbol,last,ticker.optDouble("priceChangePercent",0),vol15,vol1h,
            rsi15,rsi1h,supportDistance,resistanceDistance,score,category,reasons.toString());
    }

    private Series parse(JSONArray rows) throws Exception {
        Series s=new Series();
        // Binance'in son mumunu (henüz kapanmamış olabilir) analiz dışı bırak.
        int end=Math.max(0,rows.length()-1);
        for(int i=0;i<end;i++) {
            JSONArray r=rows.getJSONArray(i);
            s.open.add(r.getDouble(1)); s.high.add(r.getDouble(2)); s.low.add(r.getDouble(3));
            s.close.add(r.getDouble(4)); s.volume.add(r.getDouble(5));
        }
        return s;
    }
    private JSONArray getArray(String address) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent","HisseCoinTarayici/2.0");
        try {
            int code=c.getResponseCode();
            InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
            BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));
            StringBuilder sb=new StringBuilder(); String line;
            while((line=br.readLine())!=null) sb.append(line);
            br.close();
            if(code<200||code>=300) throw new IOException("HTTP "+code);
            return new JSONArray(sb.toString());
        } finally { c.disconnect(); }
    }
    private double ratio(ArrayList<Double> v) {
        int n=v.size(); if(n<22)return 1;
        double avg=0; for(int i=n-21;i<n-1;i++)avg+=v.get(i);
        avg/=20; return avg>0?v.get(n-1)/avg:1;
    }
    private double rsi(List<Double> c) {
        int n=c.size(); if(n<15)return 50;
        double g=0,l=0; for(int i=n-14;i<n;i++){double d=c.get(i)-c.get(i-1);if(d>0)g+=d;else l-=d;}
        if(l==0)return 100; double rs=g/l; return 100-100/(1+rs);
    }
    private boolean isHammer(Series s,int i) {
        double o=s.open.get(i),c=s.close.get(i),h=s.high.get(i),l=s.low.get(i);
        double body=Math.max(Math.abs(c-o),Math.max(c,h)*0.00001);
        double lower=Math.min(o,c)-l, upper=h-Math.max(o,c);
        return lower>=body*2.0 && upper<=body*0.8 && c>=l+(h-l)*0.55;
    }
    private boolean isBullishEngulfing(Series s,int i) {
        if(i<1)return false;
        double po=s.open.get(i-1),pc=s.close.get(i-1),o=s.open.get(i),c=s.close.get(i);
        return pc<po && c>o && o<=pc && c>=po;
    }
    private boolean isMorningStar(Series s,int i) {
        if(i<2)return false;
        double o1=s.open.get(i-2),c1=s.close.get(i-2),o2=s.open.get(i-1),c2=s.close.get(i-1);
        double o3=s.open.get(i),c3=s.close.get(i);
        double body1=Math.abs(o1-c1),body2=Math.abs(o2-c2);
        return c1<o1 && body2<body1*0.45 && c3>o3 && c3>(o1+c1)/2;
    }
    private boolean positiveRsiDivergence(ArrayList<Double> c) {
        int n=c.size(); if(n<40)return false;
        int split=n-18;
        double oldLow=Double.MAX_VALUE,newLow=Double.MAX_VALUE;
        int oldIdx=-1,newIdx=-1;
        for(int i=n-36;i<split;i++) if(c.get(i)<oldLow){oldLow=c.get(i);oldIdx=i;}
        for(int i=split;i<n-2;i++) if(c.get(i)<newLow){newLow=c.get(i);newIdx=i;}
        if(oldIdx<0||newIdx<0||newLow>=oldLow*0.998)return false;
        double oldRsi=rsi(c.subList(0,oldIdx+1)),newRsi=rsi(c.subList(0,newIdx+1));
        return newRsi>=oldRsi+3.0;
    }
    private double highest(ArrayList<Double> a,int from,int to) {
        double h=0; for(int i=Math.max(0,from);i<Math.max(0,to);i++)h=Math.max(h,a.get(i)); return h;
    }
    private double recentLow(ArrayList<Double> a,int lookback) {
        int end=a.size()-1,start=Math.max(0,end-lookback); double low=Double.MAX_VALUE;
        for(int i=start;i<end;i++) if(a.get(i)>0)low=Math.min(low,a.get(i));
        return low==Double.MAX_VALUE?0:low;
    }
    private void saveNewSignals(List<Candidate> found) {
        try {
            SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
            JSONArray old=new JSONArray(p.getString(HISTORY,"[]"));
            long now=System.currentTimeMillis();
            JSONArray out=new JSONArray();
            for(int i=0;i<old.length();i++) out.put(old.getJSONObject(i));
            for(Candidate c:found) {
                if(c.score<40) continue;
                boolean recent=false;
                for(int i=0;i<out.length();i++) {
                    JSONObject r=out.getJSONObject(i);
                    if(c.symbol.equals(r.optString("symbol")) && now-r.optLong("time")<DUPLICATE_WINDOW) {recent=true;break;}
                }
                if(!recent) {
                    JSONObject r=new JSONObject();
                    r.put("symbol",c.symbol); r.put("price",c.price); r.put("time",now);
                    r.put("score",c.score); r.put("category",c.category);
                    out.put(r);
                }
            }
            JSONArray trimmed=new JSONArray();
            int start=Math.max(0,out.length()-300);
            for(int i=start;i<out.length();i++) trimmed.put(out.getJSONObject(i));
            p.edit().putString(HISTORY,trimmed.toString()).apply();
        } catch(Exception ignored) {}
    }
    private void updateTrackedRecords(Map<String,Double> live) {
        try {
            SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
            JSONArray old=new JSONArray(p.getString(HISTORY,"[]"));
            long now=System.currentTimeMillis();
            for(int i=0;i<old.length();i++) {
                JSONObject r=old.getJSONObject(i);
                Double price=live.get(r.optString("symbol"));
                if(price==null || price<=0) continue;
                long age=now-r.optLong("time");
                double base=r.optDouble("price",0);
                if(base<=0) continue;
                if(age>=86400000L && !r.has("p1")) r.put("p1",(price/base-1)*100);
                if(age>=259200000L && !r.has("p3")) r.put("p3",(price/base-1)*100);
                if(age>=604800000L && !r.has("p7")) r.put("p7",(price/base-1)*100);
            }
            p.edit().putString(HISTORY,old.toString()).apply();
        } catch(Exception ignored) {}
    }
    private void updateHistoryLabel() {
        try {
            JSONArray a=new JSONArray(getSharedPreferences(PREFS,MODE_PRIVATE).getString(HISTORY,"[]"));
            int one=0,three=0,seven=0;
            for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);if(r.has("p1"))one++;if(r.has("p3"))three++;if(r.has("p7"))seven++;}
            historyStatus.setText("Kayıtlı erken sinyal: "+a.length()+" • Ölçülen 1g/3g/7g: "+one+"/"+three+"/"+seven);
        } catch(Exception e) { historyStatus.setText("Sinyal ölçüm kayıtları hazırlanıyor."); }
    }
    private String safe(String s){return s==null?"Bilinmeyen hata":s;}
    private void addMessage(String msg){TextView t=new TextView(this);t.setText(msg);t.setPadding(12,18,12,18);results.addView(t);}
    private void addCandidate(Candidate c,int rank) {
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(14,12,14,12);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(18);card.setBackground(bg);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,10);results.addView(card,cp);
        TextView head=new TextView(this);
        head.setText(rank+". "+c.symbol+"   •   "+String.format(Locale.US,"%.0f/100",c.score));
        head.setTextSize(18);head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);head.setTextColor(Color.rgb(25,65,105));card.addView(head);
        TextView category=new TextView(this);category.setText(c.category);category.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        category.setTextColor(c.score>=65?Color.rgb(20,125,75):Color.rgb(175,105,25));card.addView(category);
        TextView detail=new TextView(this);
        detail.setText("Fiyat: "+String.format(Locale.US,"%.8f",c.price)+" USDT"
            +"\n24s değişim: "+String.format(Locale.US,"%+.2f%%",c.dayChange)
            +"\nHacim oranı: 15dk "+String.format(Locale.US,"%.2fx",c.vol15)+" • 1s "+String.format(Locale.US,"%.2fx",c.vol1h)
            +"\nRSI: 15dk "+String.format(Locale.US,"%.1f",c.rsi15)+" • 1s "+String.format(Locale.US,"%.1f",c.rsi1h)
            +"\nSon dip desteğine mesafe: "+String.format(Locale.US,"%.2f%%",c.supportDistance)
            +"\nYakın dirence mesafe: "+String.format(Locale.US,"%.2f%%",c.resistanceDistance)
            +"\nNedenler: "+c.reasons);
        detail.setTextColor(Color.rgb(65,72,82));detail.setPadding(0,6,0,0);card.addView(detail);
    }
    @Override protected void onDestroy(){workers.shutdownNow();super.onDestroy();}

    static class Series {
        ArrayList<Double> open=new ArrayList<>(),high=new ArrayList<>(),low=new ArrayList<>(),close=new ArrayList<>(),volume=new ArrayList<>();
    }
    static class Candidate {
        String symbol,category,reasons;
        double price,dayChange,vol15,vol1h,rsi15,rsi1h,supportDistance,resistanceDistance,score;
        Candidate(String s,double p,double d,double v15,double v1,double r15,double r1,double sup,double res,double sc,String cat,String why){
            symbol=s;price=p;dayChange=d;vol15=v15;vol1h=v1;rsi15=r15;rsi1h=r1;supportDistance=sup;resistanceDistance=res;score=sc;category=cat;reasons=why;
        }
    }
}
