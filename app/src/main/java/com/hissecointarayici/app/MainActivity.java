package com.hissecointarayici.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private TextView status, marketBadge;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    private final String[] stocks = {"THYAO.IS","ASELS.IS","TUPRS.IS","AKBNK.IS","GARAN.IS","EREGL.IS","KCHOL.IS","BIMAS.IS","SASA.IS","TCELL"};
    private final String[] stockNames = {"THYAO","ASELS","TUPRS","AKBNK","GARAN","EREGL","KCHOL","BIMAS","SASA","TCELL"};
    private final String[] coins = {"BTCUSDT","ETHUSDT","BNBUSDT","SOLUSDT","XRPUSDT","DOGEUSDT","ADAUSDT","AVAXUSDT"};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        loadAll();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 18, 18, 28);
        root.setBackgroundColor(Color.rgb(247,248,250));

        TextView title = new TextView(this);
        title.setText("Hisse&Coin Tarayıcı");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(30,35,45));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView sub = new TextView(this);
        sub.setText("Akıllı piyasa taraması • API anahtarı gerekmez");
        sub.setTextSize(13);
        sub.setTextColor(Color.rgb(95,100,110));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 2, 0, 10);
        root.addView(sub);

        marketBadge = new TextView(this);
        marketBadge.setText("●  PİYASA ANALİZ EDİLİYOR");
        marketBadge.setTextSize(12);
        marketBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        marketBadge.setGravity(Gravity.CENTER);
        marketBadge.setTextColor(Color.WHITE);
        marketBadge.setPadding(12, 10, 12, 10);
        setRoundedBackground(marketBadge, Color.rgb(75,82,95), 18);
        root.addView(marketBadge, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        buttons.setPadding(0, 10, 0, 8);

        Button stocksBtn = makeButton("HİSSELER");
        Button coinsBtn = makeButton("COİNLER");
        Button scanBtn = makeButton("TARAMA");
        Button refresh = makeButton("YENİLE");
        buttons.addView(stocksBtn);
        buttons.addView(coinsBtn);
        buttons.addView(scanBtn);
        buttons.addView(refresh);
        root.addView(buttons);

        status = new TextView(this);
        status.setText("Veriler yükleniyor...");
        status.setTextSize(14);
        status.setTextColor(Color.rgb(75,80,90));
        status.setPadding(8, 10, 8, 10);
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

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setPadding(5, 0, 5, 0);
        return b;
    }

    private void loadAll() {
        status.setText("Hisseler ve coinler taranıyor...");
        marketBadge.setText("●  CANLI VERİLER ALINIYOR");
        setRoundedBackground(marketBadge, Color.rgb(40,125,85), 18);
        loadScanner();
    }

    private void loadStocks() {
        list.removeAllViews();
        status.setText("BIST hisseleri taranıyor...");
        marketBadge.setText("●  BIST TARAMASI");
        setRoundedBackground(marketBadge, Color.rgb(40,90,155), 18);
        pool.execute(() -> {
            ArrayList<Quote> data = new ArrayList<>();
            for (int i=0;i<stocks.length;i++) {
                try { data.add(fetchYahoo(stocks[i], stockNames[i])); }
                catch (Exception e) { data.add(new Quote(stockNames[i], "Veri yok", 0, 0, 50, 0)); }
            }
            runOnUiThread(() -> showQuotes(data, "BIST"));
        });
    }

    private void loadCoins() {
        list.removeAllViews();
        status.setText("Kripto piyasası taranıyor...");
        marketBadge.setText("●  KRİPTO TARAMASI");
        setRoundedBackground(marketBadge, Color.rgb(115,78,155), 18);
        pool.execute(() -> {
            ArrayList<Quote> data = new ArrayList<>();
            for (String s: coins) {
                try { data.add(fetchBinance(s)); }
                catch (Exception e) { data.add(new Quote(s.replace("USDT",""), "Veri yok", 0, 0, 50, 0)); }
            }
            runOnUiThread(() -> showQuotes(data, "KRİPTO"));
        });
    }

    private void loadScanner() {
        list.removeAllViews();
        status.setText("Sinyaller ve teknik göstergeler hesaplanıyor...");
        marketBadge.setText("●  AKILLI TARAMA ÇALIŞIYOR");
        setRoundedBackground(marketBadge, Color.rgb(205,130,35), 18);
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
            list.addView(quoteCard(q, type));
        }
        status.setText(type + " verileri güncellendi • " + data.size() + " varlık");
        marketBadge.setText("●  VERİLER GÜNCEL");
        setRoundedBackground(marketBadge, Color.rgb(40,125,85), 18);
    }

    private void showScans(ArrayList<Scan> scans) {
        list.removeAllViews();

        TextView heading = new TextView(this);
        heading.setText("🔥 GÜNÜN EN GÜÇLÜ SİNYALLERİ");
        heading.setTextSize(18);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(Color.rgb(35,40,50));
        heading.setPadding(4, 12, 4, 8);
        list.addView(heading);

        int n=0;
        for(Scan s:scans) {
            if(n++ >= 18) break;
            list.addView(scanCard(s, n));
        }

        status.setText("Tarama tamamlandı • " + scans.size() + " varlık analiz edildi");
        marketBadge.setText("●  TARAMA TAMAMLANDI");
        setRoundedBackground(marketBadge, Color.rgb(40,125,85), 18);
    }

    private View scanCard(Scan s, int rank) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 14, 16, 14);
        setRoundedBackground(card, Color.WHITE, 16);

        TextView top = new TextView(this);
        String signal = signal(s.score);
        top.setText((rank <= 3 ? "★ " : "") + s.q.name + "   " + signal + "   Skor: " +
                String.format(Locale.US,"%.0f",s.score));
        top.setTextSize(18);
        top.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.setTextColor(signalColor(s.score));
        card.addView(top);

        TextView details = new TextView(this);
        details.setText("Fiyat: " + s.q.priceText + "    24s: " + pct(s.q.change) +
                "    RSI: " + String.format(Locale.US,"%.1f",s.q.rsi));
        details.setTextSize(14);
        details.setTextColor(Color.rgb(80,85,95));
        details.setPadding(0, 6, 0, 4);
        card.addView(details);

        TextView why = new TextView(this);
        why.setText("✓ " + reasonText(s.q));
        why.setTextSize(13);
        why.setTextColor(Color.rgb(65,70,80));
        card.addView(why);

        TextView action = new TextView(this);
        action.setText("DETAYLI ANALİZ  ›");
        action.setTextSize(12);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setGravity(Gravity.RIGHT);
        action.setTextColor(Color.rgb(45,100,175));
        action.setPadding(0, 8, 0, 0);
        card.addView(action);

        card.setOnClickListener(v -> showAnalysis(s));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0, 6, 0, 6);
        card.setLayoutParams(p);
        return card;
    }

    private View quoteCard(Quote q, String type) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 14, 16, 14);
        setRoundedBackground(card, Color.WHITE, 16);

        TextView a = new TextView(this);
        a.setText(q.name + "    " + pct(q.change));
        a.setTextSize(18);
        a.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        a.setTextColor(q.change >= 0 ? Color.rgb(35,125,75) : Color.rgb(190,65,65));
        card.addView(a);

        TextView b = new TextView(this);
        b.setText("Fiyat: " + q.priceText + "    RSI: " +
                String.format(Locale.US,"%.1f",q.rsi) + "    SMA20: " +
                (q.sma > 0 ? String.format(Locale.US,"%.2f",q.sma) : "-"));
        b.setTextSize(14);
        b.setTextColor(Color.rgb(80,85,95));
        b.setPadding(0, 6, 0, 0);
        card.addView(b);

        card.setOnClickListener(v -> showAnalysis(new Scan(q, score(q))));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0, 6, 0, 6);
        card.setLayoutParams(p);
        return card;
    }

    private void showAnalysis(Scan s) {
        String trend = s.q.sma > 0 && s.q.price > s.q.sma ? "Yukarı trend" :
                s.q.sma > 0 ? "SMA20 altında" : "Trend verisi sınırlı";
        String momentum = s.q.change > 0 ? "Pozitif" : s.q.change < 0 ? "Negatif" : "Nötr";
        String rsiText = s.q.rsi >= 50 && s.q.rsi <= 68 ? "Olumlu bölge" :
                s.q.rsi > 72 ? "Aşırı alım riski" :
                s.q.rsi < 30 ? "Aşırı satım bölgesi" : "Nötr bölge";

        String msg = s.q.name + "\n\n" +
                "Skor: " + String.format(Locale.US,"%.0f/100",s.score) + "\n" +
                "Sinyal: " + signal(s.score) + "\n\n" +
                "Fiyat: " + s.q.priceText + "\n" +
                "24 saat: " + pct(s.q.change) + "\n" +
                "RSI: " + String.format(Locale.US,"%.1f",s.q.rsi) + " — " + rsiText + "\n" +
                "SMA20: " + (s.q.sma > 0 ? String.format(Locale.US,"%.4f",s.q.sma) : "-") + "\n" +
                "Trend: " + trend + "\n" +
                "Momentum: " + momentum + "\n\n" +
                "Neden bu skor?\n" + reasonText(s.q) +
                "\n\nNot: Bu analiz teknik göstergelere dayanır ve yatırım tavsiyesi değildir.";

        new android.app.AlertDialog.Builder(this)
                .setTitle("📊 " + s.q.name + " Analizi")
                .setMessage(msg)
                .setPositiveButton("KAPAT", null)
                .show();
    }

    private String reasonText(Quote q) {
        ArrayList<String> r = new ArrayList<>();
        if(q.change > 2) r.add("24s momentum güçlü");
        else if(q.change > 0) r.add("24s değişim pozitif");
        else if(q.change < -2) r.add("24s düşüş baskısı");
        else r.add("24s hareket sınırlı");

        if(q.rsi >= 50 && q.rsi <= 68) r.add("RSI dengeli");
        else if(q.rsi > 72) r.add("RSI yüksek");
        else if(q.rsi < 30) r.add("RSI düşük");
        else r.add("RSI nötr");

        if(q.sma > 0 && q.price > q.sma) r.add("fiyat SMA20 üzerinde");
        else if(q.sma > 0) r.add("fiyat SMA20 altında");

        return String.join(" • ", r);
    }

    private String signal(double score) {
        return score >= 70 ? "GÜÇLÜ" : score >= 55 ? "POZİTİF" : score <= 35 ? "ZAYIF" : "İZLE";
    }

    private int signalColor(double score) {
        if(score >= 70) return Color.rgb(30,125,75);
        if(score >= 55) return Color.rgb(45,105,175);
        if(score <= 35) return Color.rgb(190,65,65);
        return Color.rgb(120,100,45);
    }

    private void setRoundedBackground(View v, int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        v.setBackground(g);
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
        c.setRequestProperty("User-Agent","Mozilla/5.0 HisseCoinTarayici/3.0");
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
        c.setRequestProperty("User-Agent","Mozilla/5.0 HisseCoinTarayici/3.0");
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

    static class Scan {
        Quote q; double score;
        Scan(Quote q,double s){this.q=q;score=s;}
    }
}
