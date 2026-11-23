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
        EarlyOpportunityValidationScheduler.ensureScheduled(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        final int pagePadding = dp(18);
        root.setPadding(pagePadding,pagePadding,pagePadding,pagePadding);
        root.setBackgroundColor(Color.rgb(247,248,250));
        // Android sistem cubuklarinin icerikle cakismasini onle.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            root.setPadding(pagePadding, pagePadding + topInset,
                    pagePadding, pagePadding + bottomInset);
            return insets;
        });
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
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(true);
        // Alt gezinme cubugu ve son kartin altinda kaydirma payi birak.
        scroll.setPadding(0,0,0,dp(64));
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(0,0,0,dp(48));
        results.setClipToPadding(false);
        results.setClipChildren(false);
        scroll.addView(results);
        root.setClipToPadding(false);
        root.setClipChildren(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
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
                    double tickerPrice=t.optDouble("lastPrice",0);
                    if(symbol.endsWith("USDT") && tickerPrice>0
                            && !Double.isNaN(tickerPrice) && !Double.isInfinite(tickerPrice))
                        livePrices.put(symbol,tickerPrice);
                    double quoteVol=t.optDouble("quoteVolume",0);
                    String base = symbol.endsWith("USDT") ? symbol.substring(0, symbol.length()-4) : "";
                    if(symbol.endsWith("USDT") && !base.isEmpty()
                        && !isStableBase(base) && !isMetalBase(base) && !isLeveragedToken(base)
                        && quoteVol>0 && !Double.isNaN(quoteVol) && !Double.isInfinite(quoteVol)
                        && !Double.isNaN(change) && !Double.isInfinite(change)
                        && change<=10.0 && change>=-20.0) {
                        ranked.add(t);
                    }
                }
                long marketSnapshotAt = System.currentTimeMillis();
                updateTrackedRecords(livePrices, marketSnapshotAt);
                ranked.sort((a,b)->Double.compare(b.optDouble("quoteVolume",0),a.optDouble("quoteVolume",0)));
                int count=Math.min(300,ranked.size());
                ArrayList<Future<Candidate>> futures=new ArrayList<>();
                for(int i=0;i<count;i++) {
                    final JSONObject t=ranked.get(i);
                    futures.add(workers.submit(() -> analyze(t)));
                }
                ArrayList<Candidate> found=new ArrayList<>();
                int failed=0;
                // Use one overall scan deadline. Per-future 40s waits could otherwise
                // add up hundreds of times and leave queued HTTP tasks running after UI completion.
                long scanDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(180);
                for(Future<Candidate> future:futures) {
                    long remaining=scanDeadline-System.nanoTime();
                    if(remaining<=0) {
                        failed++;
                        future.cancel(true);
                        continue;
                    }
                    try {
                        Candidate c=future.get(remaining,TimeUnit.NANOSECONDS);
                        if(c!=null) found.add(c);
                    } catch(TimeoutException e) {
                        failed++;
                        future.cancel(true);
                    } catch(InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failed++;
                        break;
                    } catch(Exception e) {
                        failed++;
                    }
                }
                for(Future<Candidate> future:futures) {
                    if(!future.isDone()) future.cancel(true);
                }
                found.sort((a,b)->Double.compare(b.score,a.score));
                recordAndMeasureConfirmedSignals(found, livePrices, marketSnapshotAt);
                saveNewSignals(found, livePrices, marketSnapshotAt);
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
    private boolean isLeveragedToken(String base) {
        // Filter Binance leveraged-token suffixes only; substring matching wrongly
        // excluded ordinary assets whose names merely contain UP/DOWN/BULL/BEAR.
        return base.endsWith("UP") || base.endsWith("DOWN")
                || base.endsWith("BULL") || base.endsWith("BEAR");
    }

    private Candidate analyze(JSONObject ticker) throws Exception {
        String symbol=ticker.getString("symbol");
        JSONArray k15=getArray("https://api.binance.com/api/v3/klines?symbol="+symbol+"&interval=15m&limit=100");
        JSONArray k1h=getArray("https://api.binance.com/api/v3/klines?symbol="+symbol+"&interval=1h&limit=60");
        Series a=parse(k15), b=parse(k1h);
        if(a.close.size()<45 || b.close.size()<30) return null;
        int n=a.close.size();
        double last=a.close.get(n-1);
        double displayPrice=ticker.optDouble("lastPrice",last);
        if(displayPrice<=0 || Double.isNaN(displayPrice) || Double.isInfinite(displayPrice))
            displayPrice=last;
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
        double rsi1hPrevious=rsi(b.close.subList(0,b.close.size()-5));
        boolean hourlyRsiRising=rsi1h>=rsi1hPrevious+1.0;
        double last1h=b.close.get(b.close.size()-1);
        double sma20=sma(b.close,20), sma50=sma(b.close,50);

        // Şeffaf 100 puan: saatlik momentum 20, hacim uyumu 20, seviye 20,
        // trend 15, dönüş teyidi 15, risk/ödül 10.
        int hourlyPoints = rsi1h>=55 && hourlyRsiRising ? 20
                : rsi1h>=50 && hourlyRsiRising ? 17
                : rsi1h>=50 ? 13
                : rsi1h>=45 && hourlyRsiRising ? 10
                : rsi1h>=40 ? 5 : 0;

        int volumePoints = vol15>=1.5 && vol1h>=1.5 ? 20
                : vol15>=1.2 && vol1h>=1.2 ? 16
                : vol1h>=1.2 ? 12
                : vol15>=1.5 ? 7
                : vol15>=1.2 ? 4 : 0;

        int levelPoints = supportDistance>=0 && supportDistance<=1.0 ? 10
                : nearSupport ? 6 : 0;
        if(resistanceDistance>=2.0) levelPoints+=10;
        else if(resistanceDistance>=1.0) levelPoints+=7;
        else if(resistanceDistance>=0.5) levelPoints+=4;
        else if(resistanceDistance<0 && vol1h>=1.2 && rsi1h>=50) levelPoints+=6;
        else if(resistanceDistance<0) levelPoints+=2;
        levelPoints=Math.min(20,levelPoints);

        int trendPoints = last1h>sma20 && sma20>sma50 ? 15
                : last1h>sma20 ? 10
                : sma20>sma50 ? 8 : 0;

        int patternPoints = (divergence?8:0) + ((hammer||engulf||morning)?7:0);
        boolean breakoutCandidate=resistanceDistance<0 && vol1h>=1.2 && rsi1h>=50;
        double rewardRisk = supportDistance>0 ? resistanceDistance/supportDistance : 0;
        int riskRewardPoints = resistanceDistance<0
                ? (breakoutCandidate?8:0)
                : rewardRisk>=2.0?10:rewardRisk>=1.5?7:rewardRisk>=1.0?4:0;

        double score=hourlyPoints+volumePoints+levelPoints+trendPoints+patternPoints+riskRewardPoints;
        score=Math.max(0,Math.min(100,score));
        boolean confirmed = rsi1h>=50 && rsi15>=50 && hourlyRsiRising
                && vol1h>=1.2 && (last1h>sma20 || breakoutCandidate);

        StringBuilder reasons=new StringBuilder();
        if(nearSupport) reasons.append("Desteğe yakın • ");
        if(hammer) reasons.append("Çekiç • ");
        if(engulf) reasons.append("Boğa yutan • ");
        if(morning) reasons.append("Sabah yıldızı • ");
        if(divergence) reasons.append("Pozitif RSI uyumsuzluğu • ");
        if(vol15>=1.2) reasons.append("15dk hacim artışı • ");
        if(vol1h>=1.2) reasons.append("1s hacim artışı • ");
        if(hourlyRsiRising) reasons.append("Saatlik RSI yükseliyor • ");
        if(!confirmed) reasons.append("Alım teyidi bekleniyor • ");
        reasons.append("\nPuan katkısı: RSI "+hourlyPoints+"/20 • Hacim "+volumePoints+"/20 • Seviye "+levelPoints+"/20")
                .append(" • Trend "+trendPoints+"/15 • Dönüş "+patternPoints+"/15 • Risk/ödül "+riskRewardPoints+"/10");
        String category=confirmed?"ALIM TEYİDİ • KOŞULLAR SAĞLANDI"
                :score>=60?"ERKEN FIRSAT • TEYİT BEKLİYOR"
                :score>=40?"İZLEME LİSTESİ":"ZAYIF ADAY";
        return new Candidate(symbol,displayPrice,ticker.optDouble("priceChangePercent",0),vol15,vol1h,
            rsi15,rsi1h,supportDistance,resistanceDistance,score,category,confirmed,reasons.toString());
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
    private double sma(List<Double> values,int period) {
        if(values.size()<period)return 0;
        double total=0;
        for(int i=values.size()-period;i<values.size();i++)total+=values.get(i);
        return total/period;
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
    private void saveNewSignals(List<Candidate> found, Map<String,Double> live, long snapshotAt) {
        try {
            SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
            JSONArray old=new JSONArray(p.getString(HISTORY,"[]"));
            long now=snapshotAt;
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
                    Double entryPrice=live.get(c.symbol);
                    if(entryPrice==null || entryPrice<=0 || Double.isNaN(entryPrice) || Double.isInfinite(entryPrice))
                        continue;
                    JSONObject r=new JSONObject();
                    r.put("symbol",c.symbol); r.put("price",entryPrice); r.put("time",snapshotAt);
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
    private void updateTrackedRecords(Map<String,Double> live, long snapshotAt) {
        try {
            SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
            JSONArray old=new JSONArray(p.getString(HISTORY,"[]"));
            long now=snapshotAt;
            int one=p.getInt("measured_1d_total",0);
            int three=p.getInt("measured_3d_total",0);
            int seven=p.getInt("measured_7d_total",0);
            boolean initialized=p.getBoolean("measurement_totals_initialized",false);
            // İlk açılışta eldeki ölçümleri sayaçlara aktar; sonraki kayıt silinmeleri toplamı azaltmaz.
            if(!initialized) {
                for(int i=0;i<old.length();i++) {
                    JSONObject r=old.optJSONObject(i);
                    if(r==null)continue;
                    if(r.has("p1"))one++;
                    if(r.has("p3"))three++;
                    if(r.has("p7"))seven++;
                }
                initialized=true;
            }
            for(int i=0;i<old.length();i++) {
                JSONObject r=old.getJSONObject(i);
                Double price=live.get(r.optString("symbol"));
                if(price==null || price<=0) continue;
                long age=now-r.optLong("time");
                double base=r.optDouble("price",0);
                if(base<=0) continue;
                if(age>=86400000L && !r.has("p1")) {
                    r.put("p1",(price/base-1)*100); r.put("p1At",now); one++;
                }
                if(age>=259200000L && !r.has("p3")) {
                    r.put("p3",(price/base-1)*100); r.put("p3At",now); three++;
                }
                if(age>=604800000L && !r.has("p7")) {
                    r.put("p7",(price/base-1)*100); r.put("p7At",now); seven++;
                }
            }
            p.edit().putString(HISTORY,old.toString())
                    .putInt("measured_1d_total",one).putInt("measured_3d_total",three)
                    .putInt("measured_7d_total",seven)
                    .putBoolean("measurement_totals_initialized",initialized).commit();
        } catch(Exception ignored) {}
    }
    // Yeni doğrulama kayıtları eski sinyal geçmişinden tamamen ayrı tutulur.
    private static final String VALIDATION_HISTORY = "validated_signals_v1";
    private static final long[] VALIDATION_TARGETS = {
        60L*60L*1000L, 4L*60L*60L*1000L, 24L*60L*60L*1000L,
        3L*24L*60L*60L*1000L, 7L*24L*60L*60L*1000L
    };
    private static final String[] VALIDATION_KEYS = {"m1h","m4h","m24h","m3d","m7d"};
    private static final String[] VALIDATION_LABELS = {"1s","4s","24s","3g","7g"};
    private static final long[] VALIDATION_GRACE = {
        20L*60L*1000L, 30L*60L*1000L, 2L*60L*60L*1000L,
        6L*60L*60L*1000L, 12L*60L*60L*1000L
    };

    private void recordAndMeasureConfirmedSignals(List<Candidate> found,
            Map<String,Double> live, long snapshotAt) {
        synchronized (EarlyOpportunityActivity.class) {
            try {
                SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
                JSONArray records=new JSONArray(p.getString(VALIDATION_HISTORY,"[]"));
                for(int i=0;i<records.length();i++) {
                    JSONObject r=records.optJSONObject(i);
                    if(r==null) continue;
                    Double current=live.get(r.optString("symbol"));
                    if(current==null || current<=0 || Double.isNaN(current) || Double.isInfinite(current)) continue;
                    long entryAt=r.optLong("entryAt",0L);
                    double entry=r.optDouble("entryPrice",0);
                    if(entryAt<=0 || entry<=0) continue;
                    long age=snapshotAt-entryAt;
                    if(age<0) continue;
                    for(int j=0;j<VALIDATION_TARGETS.length;j++) {
                        String key=VALIDATION_KEYS[j];
                        if(r.has(key)) continue;
                        // Re-open old missed checkpoints; the actual elapsed time is retained.
                        r.remove(key+"_missed");
                        r.remove(key+"_missedAt");
                        if(age>=VALIDATION_TARGETS[j]) {
                            JSONObject m=new JSONObject();
                            m.put("price",current);
                            m.put("returnPct",(current/entry-1.0)*100.0);
                            m.put("measuredAt",snapshotAt);
                            m.put("elapsedMs",age);
                            m.put("targetMs",VALIDATION_TARGETS[j]);
                            m.put("delayMs",Math.max(0L,age-VALIDATION_TARGETS[j]));
                            m.put("late",age>VALIDATION_TARGETS[j]+VALIDATION_GRACE[j]);
                            m.put("source","Binance ticker lastPrice; app capture time");
                            r.put(key,m);
                        }
                    }
                }
                for(Candidate candidate:found) {
                    if(!candidate.confirmed) continue;
                    Double entryPrice=live.get(candidate.symbol);
                    if(entryPrice==null || entryPrice<=0) continue;
                    boolean duplicate=false;
                    for(int i=records.length()-1;i>=0;i--) {
                        JSONObject prior=records.optJSONObject(i);
                        if(prior==null || !candidate.symbol.equals(prior.optString("symbol"))) continue;
                        long priorAt=prior.optLong("entryAt",0L);
                        if(snapshotAt>=priorAt && snapshotAt-priorAt<DUPLICATE_WINDOW) {
                            duplicate=true; break;
                        }
                    }
                    if(duplicate) continue;
                    JSONObject r=new JSONObject();
                    r.put("schema","signal-validation-v1");
                    r.put("symbol",candidate.symbol);
                    r.put("entryPrice",entryPrice);
                    r.put("entryAt",snapshotAt);
                    r.put("score",candidate.score);
                    r.put("rsi15",candidate.rsi15);
                    r.put("rsi1h",candidate.rsi1h);
                    r.put("vol15",candidate.vol15);
                    r.put("vol1h",candidate.vol1h);
                    r.put("supportDistance",candidate.supportDistance);
                    r.put("resistanceDistance",candidate.resistanceDistance);
                    r.put("confirmation",true);
                    r.put("entryPriceSource","Binance 24hr ticker lastPrice");
                    records.put(r);
                }
                JSONArray kept=new JSONArray();
                int keepStart=Math.max(0,records.length()-1000);
                for(int i=keepStart;i<records.length();i++) kept.put(records.getJSONObject(i));
                p.edit().putString(VALIDATION_HISTORY,kept.toString()).commit();
            } catch(Exception ignored) { }
        }
    }

    private void updateHistoryLabel() {
        try {
            SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
            JSONArray legacy=new JSONArray(p.getString(HISTORY,"[]"));
            JSONArray validated=new JSONArray(p.getString(VALIDATION_HISTORY,"[]"));
            int[] measured=new int[VALIDATION_KEYS.length];
            int[] onTime=new int[VALIDATION_KEYS.length];
            int[] lateByHorizon=new int[VALIDATION_KEYS.length];
            int[] pending=new int[VALIDATION_KEYS.length];
            int[] missed=new int[VALIDATION_KEYS.length];
            int lateMeasurements=0;
            for(int i=0;i<validated.length();i++) {
                JSONObject r=validated.optJSONObject(i);
                if(r==null) continue;
                for(int j=0;j<VALIDATION_KEYS.length;j++) {
                    JSONObject checkpoint=r.optJSONObject(VALIDATION_KEYS[j]);
                    if(checkpoint!=null) {
                        measured[j]++;
                        if(checkpoint.optBoolean("late",false)) {
                            lateMeasurements++;
                            lateByHorizon[j]++;
                        } else {
                            onTime[j]++;
                        }
                    } else if(r.optBoolean(VALIDATION_KEYS[j]+"_missed",false)) {
                        missed[j]++;
                    } else {
                        pending[j]++;
                    }
                }
            }
            StringBuilder counts=new StringBuilder();
            StringBuilder onTimeCounts=new StringBuilder();
            StringBuilder lateCounts=new StringBuilder();
            StringBuilder pendingCounts=new StringBuilder();
            StringBuilder missedCounts=new StringBuilder();
            for(int j=0;j<measured.length;j++) {
                if(j>0) {
                    counts.append("/");
                    onTimeCounts.append("/");
                    lateCounts.append("/");
                    pendingCounts.append("/");
                    missedCounts.append("/");
                }
                counts.append(measured[j]);
                onTimeCounts.append(onTime[j]);
                lateCounts.append(lateByHorizon[j]);
                pendingCounts.append(pending[j]);
                missedCounts.append(missed[j]);
            }
            long bgAt=p.getLong("validation_bg_last_success",0L);
            String bgText=bgAt>0
                ? new java.text.SimpleDateFormat("dd.MM HH:mm",Locale.getDefault()).format(new java.util.Date(bgAt))
                : "henüz çalışmadı";
            int bgUpdated=p.getInt("validation_bg_last_updated",0);
            int bgFailed=p.getInt("validation_bg_last_failed",0);
            String bgError=p.getString("validation_bg_last_error","");
            historyStatus.setText("Eski kayıt: "+legacy.length()
                +" • Yeni teyitli sinyal: "+validated.length()
                +"\nÖlçülen 1s/4s/24s/3g/7g: "+counts
                +"\nZamanında: "+onTimeCounts+" • Gecikmeli: "+lateCounts+" (toplam "+lateMeasurements+")"
                +"\nBekleyen: "+pendingCounts+" • Kaçırılan: "+missedCounts
                +"\nArka plan son kontrol: "+bgText+" • Yeni ölçüm: "+bgUpdated+" • Fiyat hatası: "+bgFailed
                +(bgError.isEmpty()?"":"\nDurum: "+bgError));
        } catch(Exception e) { historyStatus.setText("Sinyal ölçüm kayıtları hazırlanıyor."); }
    }
    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
        category.setTextColor(c.confirmed?Color.rgb(20,125,75):Color.rgb(175,105,25));card.addView(category);
        TextView detail=new TextView(this);
        detail.setText("Fiyat: "+String.format(Locale.US,"%.8f",c.price)+" USDT"
            +"\n24s değişim: "+String.format(Locale.US,"%+.2f%%",c.dayChange)
            +"\nHacim oranı: 15dk "+String.format(Locale.US,"%.2fx",c.vol15)+" • 1s "+String.format(Locale.US,"%.2fx",c.vol1h)
            +"\nRSI: 15dk "+String.format(Locale.US,"%.1f",c.rsi15)+" • 1s "+String.format(Locale.US,"%.1f",c.rsi1h)
            +"\nSon dip desteğine mesafe: "+String.format(Locale.US,"%.2f%%",c.supportDistance)
            +"\nYakın dirence mesafe: "+String.format(Locale.US,"%.2f%%",c.resistanceDistance)
            +"\nAlım teyidi: "+(c.confirmed?"EVET":"BEKLENİYOR")
            +"\nNedenler ve puan dağılımı: "+c.reasons);
        detail.setTextColor(Color.rgb(65,72,82));detail.setPadding(0,6,0,0);card.addView(detail);
    }
    @Override protected void onDestroy(){workers.shutdownNow();super.onDestroy();}

    static class Series {
        ArrayList<Double> open=new ArrayList<>(),high=new ArrayList<>(),low=new ArrayList<>(),close=new ArrayList<>(),volume=new ArrayList<>();
    }
    static class Candidate {
        String symbol,category,reasons;
        double price,dayChange,vol15,vol1h,rsi15,rsi1h,supportDistance,resistanceDistance,score;
        boolean confirmed;
        Candidate(String s,double p,double d,double v15,double v1,double r15,double r1,double sup,double res,double sc,String cat,boolean isConfirmed,String why){
            symbol=s;price=p;dayChange=d;vol15=v15;vol1h=v1;rsi15=r15;rsi1h=r1;supportDistance=sup;resistanceDistance=res;score=sc;category=cat;confirmed=isConfirmed;reasons=why;
        }
    }
}
