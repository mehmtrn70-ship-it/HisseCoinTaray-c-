package com.hissecointarayici.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class EarlyOpportunityActivity extends Activity {
    private LinearLayout results;
    private TextView status;
    private final ExecutorService workers = Executors.newFixedThreadPool(6);
    private volatile boolean running = false;

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
        info.setText("Binance USDT • 15 dk + 1 saat • İlk 60 yüksek hacimli parite");
        info.setTextColor(Color.DKGRAY); info.setPadding(0,8,0,12);
        root.addView(info);
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
        status.setText("Binance evreni alınıyor...");
        workers.execute(() -> {
            try {
                JSONArray tickers = getArray("https://api.binance.com/api/v3/ticker/24hr");
                ArrayList<JSONObject> ranked = new ArrayList<>();
                for(int i=0;i<tickers.length();i++) {
                    JSONObject t=tickers.getJSONObject(i);
                    String symbol=t.optString("symbol");
                    double change=t.optDouble("priceChangePercent",0);
                    double quoteVol=t.optDouble("quoteVolume",0);
                    String base = symbol.endsWith("USDT") ? symbol.substring(0, symbol.length()-4) : "";
                    if(symbol.endsWith("USDT") && !isStableBase(base) && !isMetalBase(base)
                        && !symbol.contains("UP") && !symbol.contains("DOWN")
                        && !symbol.contains("BULL") && !symbol.contains("BEAR")
                        && quoteVol>0 && change<=10.0 && Math.abs(change)>=1.0) ranked.add(t);
                }
                ranked.sort((a,b)->Double.compare(b.optDouble("quoteVolume",0),a.optDouble("quoteVolume",0)));
                int count=Math.min(60,ranked.size());
                ArrayList<Future<Candidate>> futures=new ArrayList<>();
                for(int i=0;i<count;i++) {
                    final JSONObject t=ranked.get(i);
                    futures.add(workers.submit(() -> analyze(t)));
                }
                ArrayList<Candidate> found=new ArrayList<>();
                int failed=0;
                for(Future<Candidate> f:futures) {
                    try { Candidate c=f.get(35,TimeUnit.SECONDS); if(c!=null) found.add(c); }
                    catch(Exception e) { failed++; }
                }
                found.sort((a,b)->Double.compare(b.score,a.score));
                final int failures=failed, scanned=count;
                runOnUiThread(() -> {
                    status.setText("İncelenen: "+scanned+" • Analiz hatası: "+failures+" • Aday: "+found.size()
                        +"\n24 saatte %10'dan fazla yükselenler dışarıda bırakıldı. Skor tahmin garantisi değildir.");
                    int shown=Math.min(10,found.size());
                    if(shown==0) addMessage("Şu an filtreleri geçen aday bulunamadı. Daha sonra tekrar dene.");
                    for(int i=0;i<shown;i++) addCandidate(found.get(i),i+1);
                    running=false;
                });
            } catch(Exception e) {
                runOnUiThread(() -> { status.setText("Veri alınamadı: "+safe(e.getMessage())); running=false; });
            }
        });
    }

    // Stablecoins are excluded from directional opportunity rankings.
    private boolean isStableBase(String b) {
        return Arrays.asList("USDT","USDC","BUSD","FDUSD","TUSD","USDP","DAI","USDE",
            "USDS","USDD","USTC","FRAX","PYUSD","EUR","EURI","AEUR","RLUSD","USD1",
            "USDJ","USDX","GUSD","LUSD","SUSD","USDG","RWUSD").contains(b);
    }
    // Gold-backed tokens are not mixed with crypto momentum candidates.
    private boolean isMetalBase(String b) {
        return Arrays.asList("XAUT","PAXG").contains(b);
    }

    private Candidate analyze(JSONObject ticker) throws Exception {
        String symbol=ticker.getString("symbol");
        JSONArray k15=getArray("https://api.binance.com/api/v3/klines?symbol="+symbol+"&interval=15m&limit=100");
        JSONArray k1h=getArray("https://api.binance.com/api/v3/klines?symbol="+symbol+"&interval=1h&limit=60");
        Series a=parse(k15), b=parse(k1h);
        if(a.close.size()<30 || b.close.size()<30) return null;
        double rsi15=rsi(a.close), rsi1h=rsi(b.close);
        double vol15=ratio(a.volume), vol1h=ratio(b.volume);
        double macd15=macdHistogram(a.close), macd1h=macdHistogram(b.close);
        double last=a.close.get(a.close.size()-1);
        double priorHigh=highest(a.high,a.high.size()-21,a.high.size()-1);
        double breakout=priorHigh>0?last/priorHigh:1;
        double volumeScore=clamp((Math.min(vol15,4)-1)/3*25,0,25);
        double breakoutScore=breakout>=1?25:clamp((breakout-0.975)/0.025*25,0,25);
        double momentumScore=0;
        momentumScore+=rsiScore(rsi15)*15;
        momentumScore+=rsiScore(rsi1h)*15;
        double macdScore=(macd15>0?10:0)+(macd1h>0?10:0);
        double score=volumeScore+breakoutScore+momentumScore+macdScore;
        // Do not promote an already overheated short-term move.
        if(rsi15>78 || rsi1h>78) score-=10;
        return new Candidate(symbol,last,ticker.optDouble("priceChangePercent",0),vol15,vol1h,rsi15,rsi1h,breakout,score);
    }

    private Series parse(JSONArray rows) throws Exception {
        Series s=new Series();
        // Binance's final candle may still be forming; use only closed candles.
        int end=Math.max(0,rows.length()-1);
        for(int i=0;i<end;i++) {
            JSONArray r=rows.getJSONArray(i);
            s.high.add(r.getDouble(2)); s.close.add(r.getDouble(4)); s.volume.add(r.getDouble(5));
        }
        return s;
    }
    private JSONArray getArray(String address) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent","HisseCoinTarayici/1.0");
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
    private double rsi(ArrayList<Double> c) {
        int n=c.size(); if(n<15)return 50;
        double g=0,l=0; for(int i=n-14;i<n;i++){double d=c.get(i)-c.get(i-1);if(d>0)g+=d;else l-=d;}
        if(l==0)return 100; double rs=g/l; return 100-100/(1+rs);
    }
    private double rsiScore(double r) {
        if(r>=52&&r<=68)return 1;
        if(r>68&&r<=74)return 0.65;
        if(r>=45&&r<52)return 0.55;
        if(r>74&&r<=78)return 0.25;
        return 0.1;
    }
    private double ema(ArrayList<Double> a,int n) {
        double e=a.get(0),k=2.0/(n+1);
        for(int i=1;i<a.size();i++)e=a.get(i)*k+e*(1-k);
        return e;
    }
    private double macdHistogram(ArrayList<Double> c) {
        if(c.size()<35)return 0;
        ArrayList<Double> m=new ArrayList<>();
        for(int i=25;i<c.size();i++) {
            ArrayList<Double> part=new ArrayList<>(c.subList(0,i+1));
            m.add(ema(part,12)-ema(part,26));
        }
        if(m.size()<10)return 0;
        double signal=ema(m,9);
        return m.get(m.size()-1)-signal;
    }
    private double highest(ArrayList<Double> a,int from,int to) {
        double h=0; for(int i=Math.max(0,from);i<Math.max(0,to);i++)h=Math.max(h,a.get(i)); return h;
    }
    private double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    private String safe(String s){return s==null?"Bilinmeyen hata":s;}
    private void addMessage(String msg){TextView t=new TextView(this);t.setText(msg);t.setPadding(12,18,12,18);results.addView(t);}
    private void addCandidate(Candidate c,int rank) {
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(14,12,14,12);
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(18);card.setBackground(bg);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,10);results.addView(card,cp);
        TextView head=new TextView(this);head.setText(rank+". "+c.symbol+"   •   "+String.format(Locale.US,"%.0f/100",c.score));
        head.setTextSize(18);head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);head.setTextColor(Color.rgb(25,65,105));card.addView(head);
        TextView detail=new TextView(this);
        detail.setText("Fiyat: "+String.format(Locale.US,"%.8f",c.price)+" USDT"
            +"\n24s değişim: "+String.format(Locale.US,"%+.2f%%",c.dayChange)
            +"\nHacim oranı: 15dk "+String.format(Locale.US,"%.2fx",c.vol15)+" • 1s "+String.format(Locale.US,"%.2fx",c.vol1h)
            +"\nRSI: 15dk "+String.format(Locale.US,"%.1f",c.rsi15)+" • 1s "+String.format(Locale.US,"%.1f",c.rsi1h)
            +"\n15dk direnç mesafesi: "+String.format(Locale.US,"%+.2f%%",(c.breakout-1)*100));
        detail.setTextColor(Color.rgb(65,72,82));detail.setPadding(0,6,0,0);card.addView(detail);
    }
    @Override protected void onDestroy(){workers.shutdownNow();super.onDestroy();}

    static class Series { ArrayList<Double> close=new ArrayList<>(),high=new ArrayList<>(),volume=new ArrayList<>(); }
    static class Candidate {
        String symbol; double price,dayChange,vol15,vol1h,rsi15,rsi1h,breakout,score;
        Candidate(String s,double p,double d,double v15,double v1,double r15,double r1,double br,double sc){
            symbol=s;price=p;dayChange=d;vol15=v15;vol1h=v1;rsi15=r15;rsi1h=r1;breakout=br;score=sc;
        }
    }
}