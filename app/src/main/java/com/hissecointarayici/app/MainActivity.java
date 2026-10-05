package com.hissecointarayici.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private LinearLayout list;
    private TextView status;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    private final String[] stocks = {"THYAO.IS","ASELS.IS","TUPRS.IS","AKBNK.IS","GARAN.IS","EREGL.IS","KCHOL.IS","BIMAS.IS","SASA.IS","TCELL.IS"};
    private final String[] stockNames = {"THYAO","ASELS","TUPRS","AKBNK","GARAN","EREGL","KCHOL","BIMAS","SASA","TCELL"};
    private final String[] coins = {"BTCUSDT","ETHUSDT","BNBUSDT","SOLUSDT","XRPUSDT","DOGEUSDT","ADAUSDT","AVAXUSDT"};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        loadAll();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 24, 28, 28);

        TextView title = new TextView(this);
        title.setText("Hisse&Coin Tarayıcı");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView sub = new TextView(this);
        sub.setText("API anahtarı gerekmez • Piyasa verisi internetten alınır");
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        Button stocksBtn = new Button(this); stocksBtn.setText("Hisseler");
        Button coinsBtn = new Button(this); coinsBtn.setText("Coinler");
        Button scanBtn = new Button(this); scanBtn.setText("Tarayıcı");
        Button refresh = new Button(this); refresh.setText("Yenile");
        buttons.addView(stocksBtn); buttons.addView(coinsBtn); buttons.addView(scanBtn); buttons.addView(refresh);
        root.addView(buttons);

        status = new TextView(this);
        status.setText("Veriler yükleniyor...");
        status.setPadding(8, 16, 8, 16);
        root.addView(status);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        stocksBtn.setOnClickListener(v -> loadStocks());
        coinsBtn.setOnClickListener(v -> loadCoins());
        scanBtn.setOnClickListener(v -> loadScanner());
        refresh.setOnClickListener(v -> loadAll());

        scroll.addView(root);
        setContentView(scroll);
    }

    private void loadAll() {
        status.setText("Veriler yükleniyor...");
        loadStocks();
    }

    private void loadStocks() {
        list.removeAllViews();
        status.setText("BIST hisseleri taranıyor...");
        pool.execute(() -> {
            ArrayList<Quote> data = new ArrayList<>();
            for (int i=0;i<stocks.length;i++) {
                try { data.add(fetchYahoo(stocks[i], stockNames[i])); }
                catch (Exception e) { data.add(new Quote(stockNames[i], "Veri alınamadı", 0, 0, 0)); }
            }
            runOnUiThread(() -> showQuotes(data, "BIST"));
        });
    }

    private void loadCoins() {
        list.removeAllViews();
        status.setText("Kripto piyasası taranıyor...");
        pool.execute(() -> {
            ArrayList<Quote> data = new ArrayList<>();
            for (String s: coins) {
                try { data.add(fetchBinance(s)); }
                catch (Exception e) { data.add(new Quote(s.replace("USDT",""), "Veri alınamadı", 0, 0, 0)); }
            }
            runOnUiThread(() -> showQuotes(data, "KRİPTO"));
        });
    }

    private void loadScanner() {
        list.removeAllViews();
        status.setText("Sinyaller hesaplanıyor...");
        pool.execute(() -> {
            ArrayList<Scan> scans = new ArrayList<>();
            for (int i=0;i<stocks.length;i++) {
                try {
                    Quote q=fetchYahoo(stocks[i], stockNames[i]);
                    scans.add(new Scan(q, score(q)));
                } catch(Exception ignored) {}
            }
            for(String s:coins) {
                try {
                    Quote q=fetchBinance(s);
                    scans.add(new Scan(q, score(q)));
                } catch(Exception ignored) {}
            }
            scans.sort((a,b)->Double.compare(b.score,a.score));
            runOnUiThread(() -> showScans(scans));
        });
    }

    private void showQuotes(ArrayList<Quote> data, String type) {
        list.removeAllViews();
        for (Quote q:data) {
            TextView row = row(q.name + "   " + q.priceText + "   " + pct(q.change) +
                    "\nRSI: " + (q.rsi > 0 ? String.format(Locale.US,"%.1f",q.rsi) : "-") +
                    "   SMA20: " + (q.sma > 0 ? String.format(Locale.US,"%.2f",q.sma) : "-"));
            list.addView(row);
        }
        status.setText(type + " verileri güncellendi. " + new Date());
    }

    private void showScans(ArrayList<Scan> scans) {
        list.removeAllViews();
        int n=0;
        for(Scan s:scans) {
            if(n++ >= 18) break;
            String signal = s.score >= 70 ? "GÜÇLÜ" : s.score >= 55 ? "POZİTİF" : s.score <= 35 ? "ZAYIF" : "İZLE";
            TextView row=row(s.q.name + "   " + signal + "   Skor: " + String.format(Locale.US,"%.0f",s.score) +
                    "\nFiyat: " + s.q.priceText + "   24s: " + pct(s.q.change) +
                    "   RSI: " + String.format(Locale.US,"%.1f",s.q.rsi));
            list.addView(row);
        }
        status.setText("Tarama tamamlandı. Sinyaller yatırım tavsiyesi değildir.");
    }

    private TextView row(String text) {
        TextView t=new TextView(this);
        t.setText(text); t.setTextSize(16); t.setPadding(18,18,18,18);
        t.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,6,0,6);
        t.setLayoutParams(p); return t;
    }

    private Quote fetchYahoo(String symbol,String name) throws Exception {
        String u="https://query1.finance.yahoo.com/v8/finance/chart/"+URLEncoder.encode(symbol,"UTF-8")+"?interval=1d&range=3mo";
        JSONObject root=getJson(u);
        JSONObject r=root.getJSONObject("chart").getJSONArray("result").getJSONObject(0);
        JSONObject meta=r.getJSONObject("meta");
        double price=meta.optDouble("regularMarketPrice",Double.NaN);
        JSONArray close=r.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).optJSONArray("close");
        ArrayList<Double> c=toList(close);
        if(Double.isNaN(price) && !c.isEmpty()) price=c.get(c.size()-1);
        double prev=c.size()>1?c.get(c.size()-2):price;
        double change=prev!=0?(price-prev)*100/prev:0;
        return new Quote(name,format(price),price,change,rsi(c),sma(c,20));
    }

    private Quote fetchBinance(String symbol) throws Exception {
        JSONObject t=getJson("https://api.binance.com/api/v3/ticker/24hr?symbol="+symbol);
        double price=t.getDouble("lastPrice"), change=t.getDouble("priceChangePercent");
        JSONArray k=getJsonArray("https://api.binance.com/api/v3/klines?symbol="+symbol+"&interval=1d&limit=60");
        ArrayList<Double> c=new ArrayList<>();
        for(int i=0;i<k.length();i++) c.add(k.getJSONArray(i).getDouble(4));
        return new Quote(symbol.replace("USDT",""),format(price),price,change,rsi(c),sma(c,20));
    }

    private double score(Quote q) {
        double s=50;
        if(q.change>2) s+=15; else if(q.change>0) s+=7; else if(q.change<-3) s-=15; else if(q.change<0) s-=7;
        if(q.rsi>=50 && q.rsi<=68) s+=15; else if(q.rsi>72) s-=10; else if(q.rsi<30) s+=5;
        if(q.sma>0 && q.price>q.sma) s+=15; else if(q.sma>0) s-=10;
        return Math.max(0,Math.min(100,s));
    }

    private JSONObject getJson(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent","Mozilla/5.0 HisseCoinTarayici/2.0");
        int code=c.getResponseCode();
        if(code<200 || code>=300) throw new Exception("HTTP "+code);
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb=new StringBuilder(); String line;
        while((line=br.readLine())!=null) sb.append(line);
        br.close(); c.disconnect();
        return new JSONObject(sb.toString());
    }

    private JSONArray getJsonArray(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent","Mozilla/5.0 HisseCoinTarayici/2.0");
        int code=c.getResponseCode(); if(code<200||code>=300) throw new Exception("HTTP "+code);
        BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb=new StringBuilder(); String line; while((line=br.readLine())!=null) sb.append(line);
        br.close(); c.disconnect(); return new JSONArray(sb.toString());
    }

    private ArrayList<Double> toList(JSONArray a) {
        ArrayList<Double> x=new ArrayList<>(); if(a==null)return x;
        for(int i=0;i<a.length();i++) if(!a.isNull(i)) x.add(a.optDouble(i,Double.NaN));
        x.removeIf(v->Double.isNaN(v));
        return x;
    }
    private double sma(ArrayList<Double> a,int n) {
        if(a.size()<n)return 0; double s=0; for(int i=a.size()-n;i<a.size();i++)s+=a.get(i); return s/n;
    }
    private double rsi(ArrayList<Double> a) {
        if(a.size()<15)return 50; double gain=0,loss=0;
        int start=Math.max(1,a.size()-14);
        for(int i=start;i<a.size();i++){double d=a.get(i)-a.get(i-1); if(d>0)gain+=d;else loss-=d;}
        if(loss==0)return 100; double rs=(gain/14)/(loss/14); return 100-(100/(1+rs));
    }
    private String format(double x){return String.format(Locale.US,"%.4f",x);}
    private String pct(double x){return String.format(Locale.US,"%+.2f%%",x);}

    static class Quote {
        String name,priceText; double price,change,rsi,sma;
        Quote(String n,String p,double pr,double ch,double r,double s){name=n;priceText=p;price=pr;change=ch;rsi=r;sma=s;}
    }
    static class Scan { Quote q; double score; Scan(Quote q,double s){this.q=q;score=s;} }
}
