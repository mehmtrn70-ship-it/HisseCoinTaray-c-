package com.hissecointarayici.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Callable;

public class MainActivity extends Activity {
    private LinearLayout list;
    private TextView status, marketBadge;
    private final ExecutorService pool = Executors.newFixedThreadPool(8);
    private final ExecutorService refreshPool = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler();
    private final ArrayList<Scan> latestScans = new ArrayList<>();
    private volatile boolean refreshingTopSignals = false;
    private static final long AUTO_REFRESH_MS = 60000L;
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override public void run() {
            if (!latestScans.isEmpty()) refreshTopSignals();
            mainHandler.postDelayed(this, AUTO_REFRESH_MS);
        }
    };
    private static final int RETRIES = 2;
    private static final int NETWORK_TIMEOUT_MS = 15000;

    private static final String[] BIST100 = ("ASELS,TUPRS,BIMAS,THYAO,AKBNK,EREGL,YKBNK,KCHOL,ISCTR,SAHOL,TCELL,ASTOR,GARAN,CCOLA,ENKAI,SISE,TAVHL,FROTO,MGROS,SASA,TRALT,EKGYO,AEFES,MPARK,TOASO,KRDMD,PGSUS,GUBRF,HALKB,ENERY,ENJSA,TURSG,PETKM,TTKOM,GLYHO,AGHOL,TRMET,MAVI,TKFEN,VAKBN,TRGYO,OYAKC,AHGAZ,BRSAN,AKSEN,TABGD,DOHOL,ANSGR,SOKM,RGYAS,AYGAZ,ALARK,CIMSA,RYSAS,ULKER,AKSA,ISMEN,DOAS,CVKMD,OTKAR,GLRMK,TSKB,ARCLK,HEKTS,TTRAK,ISDMR,TRENJ,ALBRK,ECILC,CANTE,CWENE,ANHYT,TCKRC,BTCIM,AKFYE,SNGYO,GRSEL,ODAS,BRYAT,EGEEN,ECZYT,BERA,FENER,KCAER,ALTNY,EGGUB,LMKDC,PAHOL,ENTRA,ZOREN,OBAMS,GWIND,KARSN,KORDS,EUREN,BINHO,VESTL,KATMR,AKCNS,ALFAS").split(",");
    private static final String[] BIST_EXTRA = ("AKENR,AKFGY,AKSGY,AKYHO,ATATP,ATLAS,AVHOL,AVOD,AYDEM,AYEN,BAKAB,BANVT,BASGZ,BAYRK,BEGYO,BIGCH,BIOEN,BJKAS,BLCYT,BMSCH,BNTAS,BORLS,BRISA,BRKO,BRKSN,BRKVY,BRLSM,BRSAN,BSOKE,BUCIM,BULGS,BURCE,BURVA,CEMAS,CEMTS,CEOEM,COSMO,CRDFA,CUSAN,DAGI,DAGHL,DAPGM,DARDL,DCTTR,DERHL,DERIM,DESA,DESPC,DEVA,DITAS,DMSAS,DOBUR,DURDO,DYOBY,EDATA,EDIP,EFORC,EGPRO,EGSER,EKIZ,ELITE,EMKEL,EMNIS,ENDAE,ENSRI,ERBOS,ERCB,ERSU,ESCAR,ESCOM,ESEN,ETILR,EUPWR,EYGYO,FADE,FONET,FORMT,FORTE,FRIGO, GEDIK,GENTS,GEREL,GLBMD,GLCVY,GOODY,GRNYO,GSDDE,GSDHO,GTYGR,GUNDG,HATEK,HATSN,HDFGS,HEDEF,HKTM,HRKET,IDGYO,IEYHO,IHAAS,IHEVA,IHGZT,IHYAY,IMASM,INDES,INFO,INGRM,INTEM,INVEO,ISBIR,ISBTR,ISFIN,ISKPL,ISKUR,ISSEN,IZENR,IZFAS,IZINV,IZMDC,JANTS,KAPLM,KAREL,KARTN,KATMR,KAYSE,KENT,KERVN,KFEIN,KIMMR,KLGYO,KLKIM,KLMSN,KLNMA,KLRHO,KLSER,KLSYN,KMPUR,KNFRT,KONKA,KONTR,KONYA,KOPOL,KOTON,KRDMA,KRDMB,KRGYO,KRVGD,KSTUR,KUTPO,KUVVA,KZBGY,LIDER,LILAK,LKMNH,LOGO,LUKSK,MAALT,MACKO,MAKIM,MANAS,MARKA,MARTI,MATAS,MEDTR,MEGAP,MEGMT,MERCN,METRO,METUR,MHRGY,MIPAZ,MMCAS,MNDRS,MNDTR,MOBTL,MRGYO,MRSHL,MSGYO,MTRKS,MZHLD,NATEN,NETAS,NIBAS,NTGAZ,NUHCM,OBAMS,OBASE,ONCSM,ORGE,ORMA,OSMEN,OSTIM,OTTO,OYLUM,OZATD,OZGYO,OZKGY,OZRDN,OZSUB,OZYSR,PAGYO,PAMEL,PAPIL,PARSN,PASEU,PCILT,PEKGY,PENGD,PENTA,PETUN,PINSU,PKART,PKENT,PLTUR,PNLSN,PNSUT,POLHO,POLTK,PRDGS,PRKAB,PRKME,PRZMA,PSDTC,QNBFK,QNBTR,RALYH,RAYSG,REEDR,REKOR,RGYAS,RODRG,RTALB,RUBNS,RUZYE,RYGYO,SAMAT,SANEL,SANFM,SANKO,SARKY,SAYAS,SDTTR,SEGMN,SEKFK,SEKUR,SELEC,SELGD,SELVA,SERNT,SEYKM,SILVR,SKBNK,SKTAS,SMART,SMRTG,SMRVA,SNGYO,SNICA,SNPAM,SODSN,SONME,SRVGY,SUNTK,SURGY,SUWEN,TARKM,TATEN,TATGD,TAVHL,TBORG,TDGYO,TEKTU,TERA,TETMT,TGSAS,TKNSA,TLMAN,TMSN,TNZTP,TRCAS,TRILC,TSGYO,TSKB,TUCLK,TUKAS,TUREX,UFUK,ULAS,ULUFA,ULUSE,ULUUN,UNLU,USAK,VAKFA,VAKFN,VAKKO,VERTU,VERUS,VESBE,VKFYO,VKGYO,VKING,VRGYO,YAPRK,YATAS,YAYLA,YEOTK,YESIL,YGGYO,YIGIT,YKSLN,YUNSA,ZEDUR,ZRGYO").replace(" GEDIK","GEDIK").split(",");
    private static final String[] BIST150 = Arrays.copyOf(mergeUnique(BIST100, BIST_EXTRA), 150);
    private static final String[] COIN_FALLBACK = {"BTCUSDT","ETHUSDT","BNBUSDT","SOLUSDT","XRPUSDT","DOGEUSDT","ADAUSDT","AVAXUSDT"};
    private int stockMode=150; // 30 / 50 / 100 / 150
    private int coinMode=0;  // 10 / 50 / 100 / 0=tümü
    private String universeLabel="BIST 150 + Coin Tümü";

    private static String[] mergeUnique(String[] first, String[] second) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        set.addAll(Arrays.asList(first));
        set.addAll(Arrays.asList(second));
        return set.toArray(new String[0]);
    }

    private String[] stockSymbols(int count) {
        String[] source = count > 100 ? BIST150 : BIST100;
        return Arrays.copyOf(source, Math.min(count, source.length));
    }

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
        Button universeBtn = makeButton("EVREN");
        buttons.addView(stocksBtn);
        buttons.addView(coinsBtn);
        buttons.addView(scanBtn);
        buttons.addView(refresh);
        buttons.addView(universeBtn);
        root.addView(buttons);

        status = new TextView(this);
        status.setText("Varsayılan tarama: BIST 150 + Coin Tümü");
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
        universeBtn.setOnClickListener(v -> chooseUniverse());

        scroll.addView(root);
        setContentView(scroll);
    }

    private void chooseUniverse() {
        String[] items={
                "BIST 30 + Coin İlk 10",
                "BIST 50 + Coin İlk 50",
                "BIST 100 + Coin İlk 100",
                "BIST 150 + Coin Tümü"
        };
        new android.app.AlertDialog.Builder(this)
                .setTitle("🔎 Tarama Evreni")
                .setItems(items,(d,which)->{
                    if(which==0){stockMode=30;coinMode=10;universeLabel="BIST 30 + Coin İlk 10";}
                    else if(which==1){stockMode=50;coinMode=50;universeLabel="BIST 50 + Coin İlk 50";}
                    else if(which==2){stockMode=100;coinMode=100;universeLabel="BIST 100 + Coin İlk 100";}
                    else {stockMode=150;coinMode=0;universeLabel="BIST 150 + Coin Tümü";}
                    status.setText("Seçildi: "+universeLabel);
                    marketBadge.setText("●  EVREN HAZIR");
                    setRoundedBackground(marketBadge, Color.rgb(40,125,85), 18);
                }).show();
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
        mainHandler.removeCallbacks(autoRefreshRunnable);
        latestScans.clear();
        list.removeAllViews();
        status.setText("BIST "+stockMode+" hisseleri taranıyor...");
        marketBadge.setText("●  BIST "+stockMode+" TARAMASI");
        setRoundedBackground(marketBadge, Color.rgb(40,90,155), 18);
        pool.execute(() -> {
            String[] syms = stockSymbols(stockMode);
            ArrayList<ScanResult> results = parallelScan(syms, false, "BIST");
            ArrayList<Quote> data = new ArrayList<>();
            int success=0;
            for(ScanResult r:results) if(r.quote!=null){ data.add(r.quote); success++; }
            final int ok=success;
            runOnUiThread(() -> showQuotes(data, "BIST "+stockMode, ok, syms.length));
        });
    }

    private void loadCoins() {
        mainHandler.removeCallbacks(autoRefreshRunnable);
        latestScans.clear();
        list.removeAllViews();
        status.setText("Binance evreni hazırlanıyor...");
        marketBadge.setText("●  KRİPTO EVRENİ");
        setRoundedBackground(marketBadge, Color.rgb(115,78,155), 18);
        pool.execute(() -> {
            try {
                String[] syms=discoverCoins(coinMode);
                ArrayList<ScanResult> results = parallelScan(syms, true, "Kripto");
                ArrayList<Quote> data=new ArrayList<>();
                int success=0;
                for(ScanResult r:results) if(r.quote!=null){ data.add(r.quote); success++; }
                final int ok=success;
                runOnUiThread(() -> showQuotes(data, "KRİPTO", ok, syms.length));
            } catch(Exception e) {
                String[] fallback=COIN_FALLBACK;
                ArrayList<ScanResult> results=parallelScan(fallback,true,"Kripto");
                ArrayList<Quote> data=new ArrayList<>();
                for(ScanResult r:results) if(r.quote!=null) data.add(r.quote);
                runOnUiThread(() -> showQuotes(data, "KRİPTO", data.size(), fallback.length));
            }
        });
    }

    private void loadScanner() {
        mainHandler.removeCallbacks(autoRefreshRunnable);
        latestScans.clear();
        list.removeAllViews();
        status.setText("Tüm seçili piyasa evreni taranıyor...");
        marketBadge.setText("●  AKILLI GENİŞ TARAMA");
        setRoundedBackground(marketBadge, Color.rgb(205,130,35), 18);
        pool.execute(() -> {
            String[] stocks=stockSymbols(stockMode);
            ArrayList<ScanResult> stockResults=parallelScan(stocks,false,"Hisseler");

            String[] coins;
            try { coins=discoverCoins(coinMode); }
            catch(Exception e) { coins=COIN_FALLBACK; }
            ArrayList<ScanResult> coinResults=parallelScan(coins,true,"Kripto");

            ArrayList<Scan> scans=new ArrayList<>();
            int attempted=stocks.length+coins.length;
            int failed=0;
            for(ScanResult r:stockResults) {
                if(r.quote!=null) scans.add(new Scan(r.quote,score(r.quote))); else failed++;
            }
            for(ScanResult r:coinResults) {
                if(r.quote!=null) scans.add(new Scan(r.quote,score(r.quote))); else failed++;
            }
            scans.sort((a,b)->Double.compare(b.score,a.score));
            final int success=scans.size(), fail=failed, total=attempted;
            runOnUiThread(() -> showScans(scans, success, fail, total));
        });
    }

    private ArrayList<ScanResult> parallelScan(String[] symbols, boolean coin, String label) {
        ArrayList<ScanResult> results=new ArrayList<>();
        CompletionService<ScanResult> cs=new ExecutorCompletionService<>(pool);
        for(String symbol:symbols) {
            cs.submit(() -> scanOne(symbol, coin));
        }
        for(int i=0;i<symbols.length;i++) {
            try {
                ScanResult r=cs.take().get();
                results.add(r);
            } catch(Exception e) {
                results.add(new ScanResult(null, ""));
            }
            final int done=i+1, total=symbols.length;
            runOnUiThread(() -> status.setText(label+" taranıyor • "+done+"/"+total+" • "+universeLabel));
        }
        return results;
    }

    private ScanResult scanOne(String symbol, boolean coin) {
        Exception last=null;
        for(int attempt=1;attempt<=RETRIES;attempt++) {
            try {
                Quote q=coin ? fetchBinance(symbol) : fetchYahoo(symbol+".IS",symbol);
                return new ScanResult(q,symbol);
            } catch(Exception e) {
                last=e;
                if(attempt<RETRIES) {
                    try { Thread.sleep(350L*attempt); } catch(InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return new ScanResult(null,symbol);
    }

    private void showQuotes(ArrayList<Quote> data, String type, int success, int attempted) {
        list.removeAllViews();
        for (Quote q:data) {
            list.addView(quoteCard(q, type));
        }
        status.setText(type + " verileri güncellendi • " + success + "/" + attempted + " başarılı");
        marketBadge.setText("●  VERİLER GÜNCEL");
        setRoundedBackground(marketBadge, Color.rgb(40,125,85), 18);
    }

    private void showScans(ArrayList<Scan> scans, int success, int failed, int attempted) {
        latestScans.clear();
        latestScans.addAll(scans);
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

        status.setText("Tarama tamamlandı • " + success + "/" + attempted + " başarılı • " + failed + " başarısız • " + universeLabel);
        marketBadge.setText("●  OTOMATİK YENİLEME AÇIK");
        setRoundedBackground(marketBadge, Color.rgb(40,125,85), 18);
        mainHandler.removeCallbacks(autoRefreshRunnable);
        mainHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_MS);
    }

    private void refreshTopSignals() {
        if (refreshingTopSignals || latestScans.isEmpty()) return;
        refreshingTopSignals = true;
        final ArrayList<Scan> snapshot = new ArrayList<>();
        for (int i=0; i<latestScans.size() && i<18; i++) snapshot.add(latestScans.get(i));
        refreshPool.execute(() -> {
            try {
                CompletionService<ScanResult> cs = new ExecutorCompletionService<>(pool);
                for (Scan old : snapshot) {
                    final boolean coin = old.q.coin;
                    final String symbol = old.q.name + (coin ? "USDT" : "");
                    cs.submit(() -> {
                        ScanResult raw = scanOne(symbol, coin);
                        return new ScanResult(raw.quote, symbol);
                    });
                }
                HashMap<String, Scan> updated = new HashMap<>();
                for (int i=0; i<snapshot.size(); i++) {
                    try {
                        ScanResult r = cs.take().get();
                        if (r.quote != null) updated.put(r.symbol, new Scan(r.quote, score(r.quote)));
                    } catch (Exception ignored) { }
                }
                runOnUiThread(() -> {
                    if (!updated.isEmpty()) {
                        ArrayList<Scan> merged = new ArrayList<>(latestScans);
                        for (int i=0; i<merged.size() && i<18; i++) {
                            Scan old = merged.get(i);
                            String key = old.q.name + (old.q.coin ? "USDT" : "");
                            Scan fresh = updated.get(key);
                            if (fresh != null) merged.set(i, fresh);
                        }
                        merged.sort((a,b)->Double.compare(b.score,a.score));
                        showScans(merged, merged.size(), 0, merged.size());
                        String time = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        status.setText("İlk 18 güçlü sinyal yenilendi • " + time + " • " + updated.size() + "/" + snapshot.size() + " başarılı • " + universeLabel);
                        marketBadge.setText("●  FİYATLAR OTOMATİK GÜNCELLENDİ");
                        setRoundedBackground(marketBadge, Color.rgb(40,125,85), 18);
                    } else {
                        String time = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        status.setText("Otomatik yenileme başarısız • " + time + " • Bağlantıyı kontrol edin");
                        marketBadge.setText("●  VERİ YENİLENEMEDİ");
                        setRoundedBackground(marketBadge, Color.rgb(180,90,45), 18);
                    }
                    refreshingTopSignals = false;
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    refreshingTopSignals = false;
                    status.setText("Otomatik yenileme başarısız • İnternet bağlantısını kontrol edin");
                    marketBadge.setText("●  VERİ YENİLENEMEDİ");
                    setRoundedBackground(marketBadge, Color.rgb(180,90,45), 18);
                });
            }
        });
    }

    @Override protected void onDestroy() {
        mainHandler.removeCallbacks(autoRefreshRunnable);
        refreshPool.shutdownNow();
        pool.shutdownNow();
        super.onDestroy();
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
                "    RSI: " + String.format(Locale.US,"%.1f",s.q.rsi) +
                "    MACD: " + String.format(Locale.US,"%.3f",s.q.macd));
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
                "Momentum: " + momentum + "\n" +
                "MACD: " + String.format(Locale.US,"%.4f",s.q.macd) + "\n" +
                "Hacim gücü: " + String.format(Locale.US,"%.2fx",s.q.volumeRatio) + "\n\n" +
                "Neden bu skor?\n" + reasonText(s.q) +
                "\n\n" + planText(s.q) +
                "\n\nNot: Bu analiz teknik göstergelere dayanır ve yatırım tavsiyesi değildir.";

        // Koruma: eski/bozuk metinlerden gelen literal \\n ifadelerini gerçek satır sonuna çevir.
        msg = msg.replace("\\n", "\n");

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
        ArrayList<Double> highs=toList(r.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).optJSONArray("high"));
        ArrayList<Double> lows=toList(r.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).optJSONArray("low"));
        ArrayList<Double> vols=toList(r.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).optJSONArray("volume"));
        double a=atr(highs,lows,c,14), sup=recentLow(lows,20), res=recentHigh(highs,20);
        return new Quote(name,format(price),price,change,rsi(c),sma(c,20),macd(c),volumeRatio(vols),a,sup,res,false);
    }

    private String[] discoverCoins(int limit) throws Exception {
        JSONObject ex=getJson("https://api.binance.com/api/v3/exchangeInfo");
        JSONArray syms=ex.getJSONArray("symbols");
        HashSet<String> allowed=new HashSet<>();
        for(int i=0;i<syms.length();i++){
            JSONObject s=syms.getJSONObject(i);
            if("TRADING".equals(s.optString("status")) && "USDT".equals(s.optString("quoteAsset")))
                allowed.add(s.optString("symbol"));
        }
        JSONArray tickers=getJsonArray("https://api.binance.com/api/v3/ticker/24hr");
        ArrayList<String[]> ranked=new ArrayList<>();
        for(int i=0;i<tickers.length();i++){
            JSONObject t=tickers.getJSONObject(i);
            String s=t.optString("symbol");
            if(!allowed.contains(s)) continue;
            double vol=t.optDouble("quoteVolume",0);
            ranked.add(new String[]{s,Double.toString(vol)});
        }
        ranked.sort((a,b)->Double.compare(Double.parseDouble(b[1]),Double.parseDouble(a[1])));
        int take=limit<=0?ranked.size():Math.min(limit,ranked.size());
        String[] out=new String[take];
        for(int i=0;i<take;i++)out[i]=ranked.get(i)[0];
        return out;
    }

    private Quote fetchBinance(String symbol) throws Exception {
        JSONObject t=getJson("https://api.binance.com/api/v3/ticker/24hr?symbol="+symbol);
        double price=t.getDouble("lastPrice"), change=t.getDouble("priceChangePercent");
        JSONArray k=getJsonArray("https://api.binance.com/api/v3/klines?symbol="+symbol+"&interval=1d&limit=60");
        ArrayList<Double> c=new ArrayList<>(), highs=new ArrayList<>(), lows=new ArrayList<>(), vols=new ArrayList<>();
        for(int i=0;i<k.length();i++){ JSONArray row=k.getJSONArray(i); highs.add(row.getDouble(2)); lows.add(row.getDouble(3)); c.add(row.getDouble(4)); vols.add(row.getDouble(5)); }
        double a=atr(highs,lows,c,14), sup=recentLow(lows,20), res=recentHigh(highs,20);
        return new Quote(symbol.replace("USDT",""),format(price),price,change,rsi(c),sma(c,20),macd(c),volumeRatio(vols),a,sup,res,true);
    }

    private String num(double x){ return x>0 ? String.format(Locale.US,"%.4f",x) : "-"; }

    private String planText(Quote q) {
        if(q.price<=0) return "Planlama için yeterli fiyat verisi yok.";
        double atr=q.atr>0?q.atr:q.price*0.02;
        double support=q.support>0?q.support:q.price-atr;
        double resistance=q.resistance>0?q.resistance:q.price+atr;
        if(support>=q.price) support=q.price-atr;
        if(resistance<=q.price) resistance=q.price+atr;

        double entryLow=Math.max(support, q.price-0.60*atr);
        double entryHigh=Math.min(q.price, support+0.35*atr);
        if(entryHigh<entryLow) { entryLow=Math.max(0,q.price-0.50*atr); entryHigh=q.price; }

        double stop=Math.max(0,support-0.50*atr);
        double entry=(entryLow+entryHigh)/2.0;
        double risk=Math.max(0.00000001,entry-stop);
        double target1=Math.max(resistance,entry+risk);
        double target2=Math.max(target1,entry+2*risk);
        double target3=Math.max(target2,entry+3*risk);
        double breakout=resistance+0.10*atr;
        double rr=(target1-entry)/risk;

        String condition;
        if(q.price<=entryHigh) condition="Fiyat alım bölgesine yakın.";
        else if(q.price<breakout) condition="Fiyat alım bölgesinin üzerinde; geri çekilme beklemek daha kontrollü.";
        else condition="Direnç aşılmış; kırılımın kalıcılığı ayrıca izlenmeli.";

        return "ALIM–SATIM PLANLAYICI\n\n"+
                "Alım bölgesi: "+num(entryLow)+" – "+num(entryHigh)+"\n"+
                "Kırılım seviyesi: "+num(breakout)+"\n"+
                "Hedef 1: "+num(target1)+"\n"+
                "Hedef 2: "+num(target2)+"\n"+
                "Hedef 3: "+num(target3)+"\n"+
                "Zarar-kes: "+num(stop)+"\n"+
                "Risk/Getiri (H1): "+String.format(Locale.US,"%.2f",rr)+"R\n\n"+
                "Destek: "+num(support)+"\n"+
                "Direnç: "+num(resistance)+"\n"+
                "ATR14: "+num(atr)+"\n\n"+
                "Durum: "+condition+"\n\n"+
                "Hesaplama; destek/direnç, ATR14 ve mevcut fiyat kullanılarak dinamik yapılır.\n"+
                "Bu bölüm teknik senaryodur, yatırım tavsiyesi değildir.";
    }

    private double score(Quote q) {
        // Dengeli puanlama: tek bir gösterge skoru tavana taşımasın.
        double s=50;
        if(q.change>8) s+=12; else if(q.change>3) s+=9; else if(q.change>0) s+=5;
        else if(q.change<-8) s-=12; else if(q.change<-3) s-=9; else if(q.change<0) s-=5;
        if(q.rsi>=52 && q.rsi<=65) s+=8;
        else if(q.rsi>72) s-=10;
        else if(q.rsi>=68) s-=4;
        else if(q.rsi<30) s+=3;
        else if(q.rsi<42) s-=3;
        if(q.sma>0 && q.price>q.sma) s+=8;
        else if(q.sma>0) s-=8;
        double macdPct=q.price>0 ? (q.macd/q.price)*100.0 : 0;
        if(macdPct>0.25) s+=6;
        else if(macdPct>0.05) s+=3;
        else if(macdPct< -0.25) s-=6;
        else if(macdPct< -0.05) s-=3;
        if(q.volumeRatio>=2.0) s+=6;
        else if(q.volumeRatio>=1.3) s+=4;
        else if(q.volumeRatio<0.5) s-=5;
        if(q.resistance>0 && q.price>q.resistance) s+=4;
        return Math.max(10,Math.min(95,s));
    }

    private JSONObject getJson(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(NETWORK_TIMEOUT_MS); c.setReadTimeout(NETWORK_TIMEOUT_MS);
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

    private double atr(ArrayList<Double> highs, ArrayList<Double> lows, ArrayList<Double> closes, int n) {
        if(highs.size()<2 || lows.size()<2 || closes.size()<2) return 0;
        int start=Math.max(1,closes.size()-n);
        double sum=0; int count=0;
        for(int i=start;i<closes.size() && i<highs.size() && i<lows.size();i++){
            double prev=closes.get(i-1);
            double tr=Math.max(highs.get(i)-lows.get(i),Math.max(Math.abs(highs.get(i)-prev),Math.abs(lows.get(i)-prev)));
            if(tr>0){sum+=tr;count++;}
        }
        return count>0?sum/count:0;
    }

    private double recentLow(ArrayList<Double> a,int n){
        if(a.size()<2) return a.isEmpty()?0:a.get(0);
        int end=a.size()-1;
        int start=Math.max(0,end-n);
        double v=Double.MAX_VALUE;
        for(int i=start;i<end;i++) if(a.get(i)>0 && a.get(i)<v) v=a.get(i);
        return v==Double.MAX_VALUE?0:v;
    }

    private double recentHigh(ArrayList<Double> a,int n){
        if(a.size()<2) return a.isEmpty()?0:a.get(0);
        int end=a.size()-1;
        int start=Math.max(0,end-n);
        double v=0;
        for(int i=start;i<end;i++) if(a.get(i)>v) v=a.get(i);
        return v;
    }

    private double ema(ArrayList<Double> a,int n) {
        if(a.isEmpty()) return 0;
        double e=a.get(0), k=2.0/(n+1);
        for(int i=1;i<a.size();i++) e=a.get(i)*k+e*(1-k);
        return e;
    }

    private double macd(ArrayList<Double> a) {
        if(a.size()<26) return 0;
        return ema(a,12)-ema(a,26);
    }

    private double volumeRatio(ArrayList<Double> v) {
        if(v.size()<21) return 1.0;
        double avg=0;
        for(int i=v.size()-21;i<v.size()-1;i++) avg+=v.get(i);
        avg/=20.0;
        return avg>0?v.get(v.size()-1)/avg:1.0;
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
        String name,priceText; boolean coin; double price,change,rsi,sma,macd,volumeRatio,atr,support,resistance;
        Quote(String n,String p,double pr,double ch,double r,double s){name=n;priceText=p;price=pr;change=ch;rsi=r;sma=s;macd=0;volumeRatio=1;atr=0;support=0;resistance=0;coin=false;}
        Quote(String n,String p,double pr,double ch,double r,double s,double m,double vr,double a,double sup,double res,boolean isCoin){name=n;priceText=p;price=pr;change=ch;rsi=r;sma=s;macd=m;volumeRatio=vr;atr=a;support=sup;resistance=res;coin=isCoin;}
    }

    static class Scan {
        Quote q; double score;
        Scan(Quote q,double s){this.q=q;score=s;}
    }

    static class ScanResult {
        Quote quote; String symbol;
        ScanResult(Quote q,String s){quote=q;symbol=s;}
    }
}
