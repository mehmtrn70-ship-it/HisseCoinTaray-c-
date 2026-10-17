package com.hissecointarayici.app;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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
    private EditText searchInput;
    private Spinner assetFilter, scoreFilter;
    private boolean darkTheme = false;
    private LinearLayout rootLayout;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1901;
    private static final String SIGNAL_CHANNEL_ID = "strong_signals";
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
        createSignalNotificationChannel();
        buildUi();
        loadAll();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(true);
        scroll.setPadding(0, 0, 0, dp(24));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                int bottomInset = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
                        : insets.getSystemWindowInsetBottom();
                view.setPadding(0, 0, 0, Math.max(dp(24), bottomInset + dp(20)));
                return insets;
            });
        }

        LinearLayout root = new LinearLayout(this);
        rootLayout = root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(20));
        root.setBackgroundColor(themeBg());

        TextView title = new TextView(this);
        title.setText("Hisse&Coin Tarayıcı");
        title.setTextSize(28);
        title.setTextColor(themeText());
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
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setWeightSum(4);
        for (Button topButton : new Button[]{stocksBtn, coinsBtn, scanBtn, refresh}) {
            LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(0, dp(48), 1);
            topParams.setMargins(2, 0, 2, 0);
            buttons.addView(topButton, topParams);
        }
        root.addView(buttons);
        LinearLayout universeRow = new LinearLayout(this);
        universeRow.setPadding(0, 0, 0, 8);
        universeBtn.setText("EVREN • Hisse ve Coin Kapsamı");
        LinearLayout.LayoutParams universeParams = new LinearLayout.LayoutParams(-1, dp(48));
        universeParams.setMargins(2, 0, 2, 0);
        universeRow.addView(universeBtn, universeParams);
        root.addView(universeRow);

        LinearLayout toolsRow = new LinearLayout(this);
        toolsRow.setGravity(Gravity.CENTER);
        toolsRow.setPadding(0, 0, 0, 8);
        Button historyBtn = makeButton("SİNYAL GEÇMİŞİ");
        Button portfolioBtn = makeButton("SANAL PORTFÖY");
        Button alertsBtn = makeButton("SİNYAL BİLDİRİMİ");
        toolsRow.addView(historyBtn, new LinearLayout.LayoutParams(0, -2, 1));
        toolsRow.addView(portfolioBtn, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(toolsRow);
        LinearLayout alertRow = new LinearLayout(this);
        alertRow.setGravity(Gravity.CENTER);
        alertRow.addView(alertsBtn, new LinearLayout.LayoutParams(-1, -2));
        root.addView(alertRow);

        LinearLayout personalRow = new LinearLayout(this);
        personalRow.setGravity(Gravity.CENTER);
        Button watchBtn = makeButton("★ TAKİP LİSTESİ");
        Button priceAlertBtn = makeButton("🔔 FİYAT ALARMI");
        Button themeBtn = makeButton("◐ TEMA");
        personalRow.addView(watchBtn, new LinearLayout.LayoutParams(0, -2, 1));
        personalRow.addView(priceAlertBtn, new LinearLayout.LayoutParams(0, -2, 1));
        personalRow.addView(themeBtn, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(personalRow);
        watchBtn.setOnClickListener(v -> showWatchlist());
        priceAlertBtn.setOnClickListener(v -> showPriceAlerts());
        themeBtn.setOnClickListener(v -> toggleTheme());

        historyBtn.setOnClickListener(v -> showSignalHistory());
        alertsBtn.setOnClickListener(v -> showNotificationSettings());
        portfolioBtn.setOnClickListener(v -> refreshPaperPricesThenShow());

        status = new TextView(this);
        status.setText("Varsayılan tarama: BIST 150 + Coin Tümü");
        status.setTextSize(14);
        status.setTextColor(Color.rgb(75,80,90));
        status.setPadding(8, 10, 8, 10);
        root.addView(status);

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.VERTICAL);
        filterRow.setPadding(0, 0, 0, 8);
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setHint("Hisse veya coin ara (ör. ASELS, BTC)");
        searchInput.setPadding(12, 8, 12, 8);
        filterRow.addView(searchInput, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout selectors = new LinearLayout(this);
        selectors.setGravity(Gravity.CENTER_VERTICAL);
        assetFilter = new Spinner(this);
        ArrayAdapter<String> assetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Tümü", "Sadece hisseler", "Sadece coinler"});
        assetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assetFilter.setAdapter(assetAdapter);
        scoreFilter = new Spinner(this);
        ArrayAdapter<String> scoreAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Tüm skorlar", "Skor 70+", "Skor 80+", "Skor 90+"});
        scoreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scoreFilter.setAdapter(scoreAdapter);
        selectors.addView(assetFilter, new LinearLayout.LayoutParams(0, -2, 1));
        selectors.addView(scoreFilter, new LinearLayout.LayoutParams(0, -2, 1));
        filterRow.addView(selectors);
        root.addView(filterRow);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderFilteredScans(); }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });
        android.widget.AdapterView.OnItemSelectedListener filterListener = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { renderFilteredScans(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        };
        assetFilter.setOnItemSelectedListener(filterListener);
        scoreFilter.setOnItemSelectedListener(filterListener);

        stocksBtn.setOnClickListener(v -> loadStocks());
        coinsBtn.setOnClickListener(v -> loadCoins());
        scanBtn.setOnClickListener(v -> loadScanner());
        refresh.setOnClickListener(v -> loadAll());
        universeBtn.setOnClickListener(v -> chooseUniverse());

        scroll.addView(root);
        setContentView(scroll);
        darkTheme = getSharedPreferences("ui_settings", MODE_PRIVATE).getBoolean("dark_theme", false);
        applyTheme();
    }



    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private int themeBg() { return darkTheme ? Color.rgb(16,20,28) : Color.rgb(247,248,250); }
    private int themeCard() { return darkTheme ? Color.rgb(35,43,57) : Color.WHITE; }
    private int themeText() { return darkTheme ? Color.rgb(250,252,255) : Color.rgb(30,35,45); }
    private int themeMuted() { return darkTheme ? Color.rgb(218,225,236) : Color.rgb(80,85,95); }
    private void toggleTheme() {
        darkTheme = !darkTheme;
        getSharedPreferences("ui_settings", MODE_PRIVATE).edit().putBoolean("dark_theme", darkTheme).apply();
        applyTheme(); renderFilteredScans();
        Toast.makeText(this, darkTheme ? "Koyu tema açık" : "Açık tema açık", Toast.LENGTH_SHORT).show();
    }
    private void applyTheme() {
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(themeBg());
            recolorTree(rootLayout);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getWindow().setNavigationBarColor(themeBg());
                getWindow().getDecorView().setSystemUiVisibility(darkTheme ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
        }
    }
    private void recolorTree(View view) {
        if (view instanceof TextView) {
            TextView t = (TextView)view;
            if (t == marketBadge) return;
            if (t instanceof Button) {
                t.setTextColor(darkTheme ? Color.rgb(20,25,34) : Color.rgb(25,30,38));
                t.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    darkTheme ? Color.rgb(220,226,235) : Color.rgb(224,228,234)));
            } else if (t instanceof EditText) {
                t.setTextColor(themeText());
                t.setHintTextColor(themeMuted());
                t.setBackgroundTintList(android.content.res.ColorStateList.valueOf(themeMuted()));
            } else {
                t.setTextColor(themeText());
            }
        }
        if (view instanceof Spinner) {
            ((Spinner)view).setBackgroundTintList(android.content.res.ColorStateList.valueOf(themeMuted()));
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i=0;i<group.getChildCount();i++) recolorTree(group.getChildAt(i));
        }
    }
    private android.content.SharedPreferences watchPrefs() { return getSharedPreferences("watchlist", MODE_PRIVATE); }
    private String watchKey(String name, boolean coin) { return (coin ? "C:" : "S:") + name; }
    private boolean isWatched(String name, boolean coin) {
        Set<String> set = new HashSet<>(watchPrefs().getStringSet("items", new HashSet<>()));
        return set.contains(watchKey(name, coin));
    }
    private void toggleWatch(String name, boolean coin) {
        Set<String> set = new HashSet<>(watchPrefs().getStringSet("items", new HashSet<>()));
        String key = watchKey(name, coin);
        if (set.contains(key)) set.remove(key); else set.add(key);
        watchPrefs().edit().putStringSet("items", set).apply();
        Toast.makeText(this, set.contains(key) ? name + " takip listesine eklendi" : name + " takip listesinden çıkarıldı", Toast.LENGTH_SHORT).show();
    }
    private void showWatchlist() {
        Set<String> items = new HashSet<>(watchPrefs().getStringSet("items", new HashSet<>()));
        ScrollView sc = new ScrollView(this);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(18,12,18,12);
        if (items.isEmpty()) {
            TextView empty = new TextView(this); empty.setText("Takip listen boş. Tarama sonuçlarında ☆ Takibe ekle düğmesini kullan."); empty.setPadding(8,12,8,12); box.addView(empty);
        } else {
            ArrayList<String> sorted = new ArrayList<>(items); Collections.sort(sorted);
            for (String item : sorted) {
                String[] parts = item.split(":",2); if(parts.length<2) continue;
                boolean coin = "C".equals(parts[0]); String name = parts[1];
                LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
                TextView label = new TextView(this); label.setText(name + " • " + currencyFor(coin)); label.setTextSize(16); label.setPadding(6,10,6,10);
                row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
                Button alarm = makeButton("Alarm"); Button remove = makeButton("Kaldır");
                row.addView(alarm); row.addView(remove); box.addView(row);
                alarm.setOnClickListener(v -> {
                    Scan found = findScan(name, coin);
                    if(found != null) showCreatePriceAlert(found.q);
                    else Toast.makeText(this,"Önce tarama yapıp bu varlığın güncel fiyatını al.",Toast.LENGTH_LONG).show();
                });
                remove.setOnClickListener(v -> { toggleWatch(name,coin); showWatchlist(); });
            }
        }
        sc.addView(box);
        new android.app.AlertDialog.Builder(this).setTitle("★ Takip Listesi").setView(sc).setPositiveButton("KAPAT",null).show();
    }
    private Scan findScan(String name, boolean coin) {
        for(Scan scan:latestScans) if(scan.q.coin==coin && scan.q.name.equalsIgnoreCase(name)) return scan;
        return null;
    }
    private android.content.SharedPreferences priceAlertPrefs() { return getSharedPreferences("price_alerts", MODE_PRIVATE); }
    private void showPriceAlerts() {
        JSONArray alerts;
        try { alerts = new JSONArray(priceAlertPrefs().getString("items", "[]")); } catch(Exception e) { alerts = new JSONArray(); }
        ScrollView sc = new ScrollView(this);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(18,12,18,12);
        if(alerts.length()==0) {
            TextView empty=new TextView(this); empty.setText("Henüz fiyat alarmı yok. Tarama kartından Fiyat alarmı oluşturabilirsin."); empty.setPadding(8,12,8,12); box.addView(empty);
        }
        for(int i=0;i<alerts.length();i++) {
            JSONObject a=alerts.optJSONObject(i); if(a==null) continue;
            String name=a.optString("name"), kind=a.optString("kind"), unit=a.optString("currency");
            String target = kind.equals("price") ? ("below".equals(a.optString("direction"))?"Altına düşünce: ":"Üstüne çıkınca: ")+a.optDouble("target",0)+" "+unit : "Değişim: "+a.optDouble("percent",0)+"% (başlangıç "+a.optDouble("base",0)+" "+unit+")";
            TextView line=new TextView(this); line.setText(name+" • "+target+" • "+(a.optBoolean("active",true)?"Aktif": "Kapalı")+(a.optBoolean("triggered",false)?" • Tetiklendi":"")); line.setPadding(4,8,4,8); box.addView(line);
            Button del=makeButton("Alarmı sil"); box.addView(del);
            final int idx=i; del.setOnClickListener(v -> { removePriceAlert(idx); showPriceAlerts(); });
        }
        sc.addView(box);
        new android.app.AlertDialog.Builder(this).setTitle("🔔 Fiyat Alarmları").setView(sc).setPositiveButton("KAPAT",null).setNeutralButton("YENİ ALARM",(d,w)->{
            if(!latestScans.isEmpty()) {
                String[] names=new String[latestScans.size()];
                for(int i=0;i<names.length;i++) names[i]=latestScans.get(i).q.name+" ("+currencyFor(latestScans.get(i).q.coin)+")";
                new android.app.AlertDialog.Builder(this).setTitle("Varlık seç").setItems(names,(dd,which)->showCreatePriceAlert(latestScans.get(which).q)).show();
            } else Toast.makeText(this,"Önce tarama yap.",Toast.LENGTH_SHORT).show();
        }).show();
    }
    private void removePriceAlert(int index) {
        try { JSONArray a=new JSONArray(priceAlertPrefs().getString("items","[]")); if(index>=0&&index<a.length()) a.remove(index); priceAlertPrefs().edit().putString("items",a.toString()).apply(); } catch(Exception ignored) {}
    }
    private void showCreatePriceAlert(Quote q) {
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(20,8,20,4);
        TextView current=new TextView(this); current.setText(q.name+" • Güncel referans: "+q.priceText+" "+currencyFor(q.coin)); form.addView(current);
        Spinner kind=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{"Hedef fiyatın üstüne çıkınca","Hedef fiyatın altına düşünce","Başlangıç fiyatından yüzde değişim"}); adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); kind.setAdapter(adapter); form.addView(kind);
        EditText value=new EditText(this); value.setInputType(8194); value.setHint("Hedef fiyat veya yüzde (ör. 5 veya -5)"); form.addView(value);
        TextView hint=new TextView(this); hint.setText("Yüzde alarmında +5 = %5 yükseliş, -5 = %5 düşüş. Kontrol tarama/yenileme sırasında yapılır."); form.addView(hint);
        new android.app.AlertDialog.Builder(this).setTitle("🔔 "+q.name+" alarmı").setView(form).setPositiveButton("KAYDET",(d,w)->{
            String raw=value.getText().toString().trim().replace(",",".");
            try {
                double v=Double.parseDouble(raw);
                if(v==0 || (kind.getSelectedItemPosition()!=2 && v<=0)) throw new NumberFormatException();
                JSONArray arr=new JSONArray(priceAlertPrefs().getString("items","[]"));
                JSONObject a=new JSONObject(); a.put("name",q.name); a.put("coin",q.coin); a.put("currency",currencyFor(q.coin)); a.put("kind",kind.getSelectedItemPosition()==2?"percent":"price"); a.put("direction",kind.getSelectedItemPosition()==1?"below":"above"); a.put("target",kind.getSelectedItemPosition()!=2?v:0); a.put("percent",kind.getSelectedItemPosition()==2?v:0); a.put("base",q.price); a.put("active",true); a.put("created",System.currentTimeMillis()); arr.put(a);
                priceAlertPrefs().edit().putString("items",arr.toString()).apply();
                Toast.makeText(this,"Alarm kaydedildi. Tarama sırasında kontrol edilecek.",Toast.LENGTH_LONG).show();
            } catch(Exception e) { Toast.makeText(this,"Geçerli bir sayı gir. Hedef fiyat pozitif olmalı; yüzde sıfır olamaz.",Toast.LENGTH_LONG).show(); }
        }).setNegativeButton("İPTAL",null).show();
    }
    private void checkPriceAlerts(ArrayList<Scan> scans) {
        JSONArray alerts;
        try { alerts=new JSONArray(priceAlertPrefs().getString("items","[]")); } catch(Exception e) { return; }
        boolean changed=false;
        for(int i=0;i<alerts.length();i++) {
            JSONObject a=alerts.optJSONObject(i); if(a==null || !a.optBoolean("active",true) || a.optBoolean("triggered",false)) continue;
            Scan match=findScan(a.optString("name"),a.optBoolean("coin",false));
            if(match==null) continue;
            double price=match.q.price; boolean hit=false; String reason="";
            if("price".equals(a.optString("kind"))) {
                double target=a.optDouble("target",0); hit="below".equals(a.optString("direction")) ? price<=target : price>=target;
                reason=("below".equals(a.optString("direction"))?"hedefin altı ":"hedefin üstü ")+target+" "+a.optString("currency");
            } else {
                double base=a.optDouble("base",0), pct=a.optDouble("percent",0);
                if(base>0) { double actual=(price-base)*100.0/base; hit=pct>0?actual>=pct:actual<=pct; reason=String.format(Locale.US,"hedef %+.2f%%, mevcut %+.2f%%",pct,actual); }
            }
            if(hit) {
                sendPriceAlarm(match.q,reason,i);
                try { a.put("triggered",true); a.put("triggeredAt",System.currentTimeMillis()); changed=true; } catch(Exception ignored) {}
            }
        }
        if(changed) priceAlertPrefs().edit().putString("items",alerts.toString()).apply();
    }
    private void sendPriceAlarm(Quote q,String reason,int id) {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,"Fiyat alarmı tetiklendi: "+q.name+" (bildirim izni kapalı)",Toast.LENGTH_LONG).show(); return;
        }
        NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE); if(manager==null)return;
        String body=q.name+" • "+q.priceText+" "+currencyFor(q.coin)+" • "+reason;
        android.app.Notification.Builder builder=Build.VERSION.SDK_INT>=26?new android.app.Notification.Builder(this,SIGNAL_CHANNEL_ID):new android.app.Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Fiyat alarmı").setContentText(body).setStyle(new android.app.Notification.BigTextStyle().bigText(body)).setAutoCancel(true);
        manager.notify(700000+id,builder.build());
    }

    private void showNotificationSettings() {
        android.content.SharedPreferences prefs = getSharedPreferences("signal_alert_settings", MODE_PRIVATE);
        int current = prefs.getInt("threshold", 80);
        String[] options = {"Skor 70 ve üzeri", "Skor 80 ve üzeri", "Skor 90 ve üzeri"};
        int selected = current >= 90 ? 2 : current >= 80 ? 1 : 0;
        new android.app.AlertDialog.Builder(this)
                .setTitle("Güçlü sinyal bildirimleri")
                .setMessage("Bildirimler, uygulama tarama yaparken veya ilk 18 sinyal yenilenirken kontrol edilir. Uygulama tamamen kapalıyken sürekli tarama yapmaz.")
                .setSingleChoiceItems(options, selected, (dialog, which) -> {
                    int threshold = which == 0 ? 70 : which == 1 ? 80 : 90;
                    prefs.edit().putInt("threshold", threshold).putBoolean("enabled", true).apply();
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, NOTIFICATION_PERMISSION_REQUEST);
                    }
                    Toast.makeText(this, "Bildirim eşiği " + threshold + " olarak ayarlandı.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("BİLDİRİMLERİ KAPAT", (dialog, which) -> {
                    prefs.edit().putBoolean("enabled", false).apply();
                    Toast.makeText(this, "Sinyal bildirimleri kapatıldı.", Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("KAPAT", null)
                .show();
    }

    private void createSignalNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(SIGNAL_CHANNEL_ID, "Güçlü sinyaller", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Seçilen skor eşiğine ulaşan hisse ve coin sinyalleri");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void maybeNotifyStrongSignals(ArrayList<Scan> scans) {
        android.content.SharedPreferences prefs = getSharedPreferences("signal_alert_settings", MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false)) return;
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) return;
        int threshold = prefs.getInt("threshold", 80);
        String day = new java.text.SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        int checked = Math.min(18, scans.size());
        for (int i = 0; i < checked; i++) {
            Scan scan = scans.get(i);
            if (scan.score < threshold) continue;
            String key = "sent_" + day + "_" + (scan.q.coin ? "C_" : "S_") + scan.q.name;
            if (prefs.getBoolean(key, false)) continue;
            String title = "Güçlü sinyal: " + scan.q.name;
            String body = "Skor " + String.format(Locale.US, "%.0f", scan.score) + "/100 • " +
                    scan.q.priceText + " " + currencyFor(scan.q.coin) + " • 24s " + pct(scan.q.change);
            android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new android.app.Notification.Builder(this, SIGNAL_CHANNEL_ID)
                    : new android.app.Notification.Builder(this);
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title).setContentText(body).setStyle(new android.app.Notification.BigTextStyle().bigText(body))
                    .setAutoCancel(true);
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pending = PendingIntent.getActivity(this, scan.q.name.hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            builder.setContentIntent(pending);
            manager.notify(Math.abs(scan.q.name.hashCode()), builder.build());
            prefs.edit().putBoolean(key, true).apply();
        }
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
        b.setTextSize(12);
        b.setMinHeight(dp(46));
        b.setAllCaps(false);
        b.setPadding(dp(6), dp(4), dp(6), dp(4));
        b.setTextColor(darkTheme ? Color.rgb(20,25,34) : Color.rgb(25,30,38));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                darkTheme ? Color.rgb(220,226,235) : Color.rgb(224,228,234)));
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
            ArrayList<String> failedSymbols=new ArrayList<>();
            for(ScanResult r:stockResults) {
                if(r.quote!=null) scans.add(new Scan(r.quote,score(r.quote)));
                else { failed++; if(r.symbol!=null && !r.symbol.trim().isEmpty()) failedSymbols.add(r.symbol+" (BIST)"); }
            }
            for(ScanResult r:coinResults) {
                if(r.quote!=null) scans.add(new Scan(r.quote,score(r.quote)));
                else { failed++; if(r.symbol!=null && !r.symbol.trim().isEmpty()) failedSymbols.add(r.symbol+" (Coin)"); }
            }
            scans.sort((a,b)->Double.compare(b.score,a.score));
            recordSignalHistory(scans);
            final int success=scans.size(), fail=failed, total=attempted;
            final String failedInfo=failedSymbols.isEmpty() ? "Sembol belirlenemedi" : String.join(", ", failedSymbols);
            runOnUiThread(() -> showScans(scans, success, fail, total, failedInfo));
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

    private void showScans(ArrayList<Scan> scans, int success, int failed, int attempted, String failedInfo) {
        latestScans.clear();
        latestScans.addAll(scans);
        maybeNotifyStrongSignals(scans);
        checkPriceAlerts(scans);
        renderFilteredScans();
        status.setText("Tarama tamamlandı • " + success + "/" + attempted + " başarılı • " + failed + " başarısız • " + universeLabel
                + (failed > 0 ? "\\nBaşarısız sembol(ler): " + failedInfo : ""));
        marketBadge.setText("●  OTOMATİK YENİLEME AÇIK");
        setRoundedBackground(marketBadge, Color.rgb(40,125,85), 18);
        mainHandler.removeCallbacks(autoRefreshRunnable);
        mainHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_MS);
    }

    private void renderFilteredScans() {
        if (list == null) return;
        list.removeAllViews();
        String query = searchInput == null ? "" : searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        int type = assetFilter == null ? 0 : assetFilter.getSelectedItemPosition();
        int selectedScore = scoreFilter == null ? 0 : scoreFilter.getSelectedItemPosition();
        int minScore = selectedScore == 1 ? 70 : selectedScore == 2 ? 80 : selectedScore == 3 ? 90 : 0;
        ArrayList<Scan> filtered = new ArrayList<>();
        for (Scan s : latestScans) {
            if (type == 1 && s.q.coin) continue;
            if (type == 2 && !s.q.coin) continue;
            if (s.score < minScore) continue;
            if (!query.isEmpty() && !s.q.name.toLowerCase(Locale.ROOT).contains(query)) continue;
            filtered.add(s);
        }
        TextView heading = new TextView(this);
        heading.setText("🔥 GÜNÜN EN GÜÇLÜ SİNYALLERİ • " + filtered.size() + " sonuç");
        heading.setTextSize(18);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(themeText());
        heading.setPadding(4, 12, 4, 8);
        list.addView(heading);
        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Bu filtrelere uygun sonuç yok. Aramayı veya skor eşiğini değiştir.");
            empty.setTextSize(14);
            empty.setPadding(8, 12, 8, 12);
            empty.setTextColor(themeMuted());
            list.addView(empty);
            return;
        }
        int shown = Math.min(18, filtered.size());
        for (int i = 0; i < shown; i++) list.addView(scanCard(filtered.get(i), i + 1));
        if (filtered.size() > shown) {
            TextView more = new TextView(this);
            more.setText("En güçlü " + shown + " sonuç gösteriliyor • filtrelenen toplam: " + filtered.size());
            more.setTextSize(12);
            more.setPadding(8, 8, 8, 12);
            more.setTextColor(themeMuted());
            list.addView(more);
        }
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
                        updatePaperPositionPrices(merged);
                        showScans(merged, merged.size(), 0, merged.size(), "");
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
        setRoundedBackground(card, themeCard(), 16);

        TextView top = new TextView(this);
        String signal = signal(s.score);
        top.setText((rank <= 3 ? "★ " : "") + s.q.name + "   " + signal + "   Skor: " +
                String.format(Locale.US,"%.0f",s.score));
        top.setTextSize(18);
        top.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.setTextColor(signalColor(s.score));
        card.addView(top);

        TextView details = new TextView(this);
        details.setText("Fiyat: " + s.q.priceText + " " + (s.q.coin ? "USDT" : "TL") + "    24s: " + pct(s.q.change) +
                "    RSI: " + String.format(Locale.US,"%.1f",s.q.rsi) +
                "    MACD: " + String.format(Locale.US,"%.3f",s.q.macd));
        details.setTextSize(14);
        details.setTextColor(themeMuted());
        details.setPadding(0, 6, 0, 4);
        card.addView(details);

        TextView why = new TextView(this);
        why.setText("✓ " + reasonText(s.q));
        why.setTextSize(13);
        why.setTextColor(themeMuted());
        card.addView(why);

        TextView action = new TextView(this);
        action.setText("DETAYLI ANALİZ  ›");
        action.setTextSize(12);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setGravity(Gravity.RIGHT);
        action.setTextColor(Color.rgb(45,100,175));
        action.setPadding(0, 8, 0, 0);
        card.addView(action);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button watch = makeButton(isWatched(s.q.name, s.q.coin) ? "★ Takipte" : "☆ Takibe ekle");
        Button alarm = makeButton("Fiyat alarmı");
        actions.addView(watch); actions.addView(alarm); card.addView(actions);
        watch.setOnClickListener(v -> { toggleWatch(s.q.name, s.q.coin); watch.setText(isWatched(s.q.name, s.q.coin) ? "★ Takipte" : "☆ Takibe ekle"); });
        alarm.setOnClickListener(v -> showCreatePriceAlert(s.q));
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
        b.setText("Fiyat: " + q.priceText + " " + currencyFor(q.coin) + "    RSI: " +
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
                "Fiyat: " + s.q.priceText + " " + currencyFor(s.q.coin) + "\n" +
                "24 saat: " + pct(s.q.change) + "\n" +
                "RSI: " + String.format(Locale.US,"%.1f",s.q.rsi) + " — " + rsiText + "\n" +
                "SMA20: " + (s.q.sma > 0 ? formatPortfolioPrice(s.q.sma, s.q.coin) + " " + currencyFor(s.q.coin) : "-") + "\n" +
                "Trend: " + trend + "\n" +
                "Momentum: " + momentum + "\n" +
                "MACD: " + String.format(Locale.US,"%.4f",s.q.macd) + "\n" +
                "Hacim gücü: " + String.format(Locale.US,"%.2fx",s.q.volumeRatio) + "\n\n" +
                "Neden bu skor?\n" + reasonText(s.q) +
                "\n\n" + planText(s.q) +
                "\n\nNot: Bu analiz teknik göstergelere dayanır ve yatırım tavsiyesi değildir.";

        // Koruma: eski/bozuk metinlerden gelen literal \n ifadelerini gerçek satır sonuna çevir.
        msg = msg.replace("\\n", "\n").replace("\\r", "\n");

        new android.app.AlertDialog.Builder(this)
                .setTitle("📊 " + s.q.name + " Analizi")
                .setMessage(msg)
                .setPositiveButton("SANAL ALIM", (dialog, which) -> showPaperBuyDialog(s))
                .setNegativeButton("KAPAT", null)
                .show();
    }

    private android.content.SharedPreferences paperPrefs() {
        return getSharedPreferences("hisse_coin_paper_v1", MODE_PRIVATE);
    }

    private void recordSignalHistory(ArrayList<Scan> scans) {
        synchronized (MainActivity.class) {
            JSONArray history;
            try { history = new JSONArray(paperPrefs().getString("signal_history", "[]")); }
            catch (Exception e) { history = new JSONArray(); }
            long now = System.currentTimeMillis();
            int count = Math.min(18, scans.size());
            for (int i = 0; i < count; i++) {
                Scan scan = scans.get(i);
                JSONObject item = new JSONObject();
                try {
                    item.put("time", now);
                    item.put("name", scan.q.name);
                    item.put("price", scan.q.price);
                    item.put("priceText", scan.q.priceText);
                    item.put("score", scan.score);
                    item.put("change", scan.q.change);
                    item.put("coin", scan.q.coin);
                    history.put(item);
                } catch (Exception ignored) { }
            }
            while (history.length() > 360) history.remove(0);
            paperPrefs().edit().putString("signal_history", history.toString()).apply();
        }
    }

    private void showSignalHistory() {
        JSONArray history;
        try { history = new JSONArray(paperPrefs().getString("signal_history", "[]")); }
        catch (Exception e) { history = new JSONArray(); }
        if (history.length() == 0) {
            new android.app.AlertDialog.Builder(this).setTitle("Sinyal Geçmişi")
                    .setMessage("Henüz kayıt yok. Önce tam piyasa taraması yap.")
                    .setPositiveButton("TAMAM", null).show();
            return;
        }
        ArrayList<String> lines = new ArrayList<>();
        int evaluated1h=0, positive1h=0, evaluated24h=0, positive24h=0, evaluated7d=0, positive7d=0;
        for (int i = history.length() - 1; i >= 0 && lines.size() < 60; i--) {
            JSONObject item = history.optJSONObject(i);
            if (item == null) continue;
            long t = item.optLong("time", 0);
            double price = item.optDouble("price", 0);
            boolean coin = item.optBoolean("coin", false);
            String name = item.optString("name");
            String time = new java.text.SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(new Date(t));
            String oneHour = historyReturn(history, i, 60L*60*1000, 6L*60*60*1000);
            String oneDay = historyReturn(history, i, 24L*60*60*1000, 36L*60*60*1000);
            String oneWeek = historyReturn(history, i, 7L*24*60*60*1000, 3L*24*60*60*1000);
            if (!"bekleniyor".equals(oneHour)) { evaluated1h++; if (oneHour.startsWith("+")) positive1h++; }
            if (!"bekleniyor".equals(oneDay)) { evaluated24h++; if (oneDay.startsWith("+")) positive24h++; }
            if (!"bekleniyor".equals(oneWeek)) { evaluated7d++; if (oneWeek.startsWith("+")) positive7d++; }
            lines.add(time + "  " + name + " • " + item.optString("priceText") +
                    " • Skor " + item.optInt("score") +
                    "\n  1s: " + oneHour + "  |  24s: " + oneDay + "  |  7g: " + oneWeek);
        }
        String summary = "Geçmiş fiyat kayıtları: " + history.length() +
                "\n1 saat: " + positive1h + "/" + evaluated1h + " pozitif ölçüm" +
                "\n24 saat: " + positive24h + "/" + evaluated24h + " pozitif ölçüm" +
                "\n7 gün: " + positive7d + "/" + evaluated7d + " pozitif ölçüm" +
                "\n'Bekleniyor' = hedef zaman için henüz yeterli sonraki fiyat kaydı yok. Bu oranlar geçmiş gözlemdir, garanti değildir.";
        TextView content = new TextView(this);
        content.setText(summary + "\n\n" + android.text.TextUtils.join("\n\n", lines));
        content.setTextSize(14);
        content.setPadding(24, 16, 24, 16);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        new android.app.AlertDialog.Builder(this).setTitle("Sinyal Geçmişi • Performans")
                .setView(scroll).setPositiveButton("KAPAT", null).show();
    }

    private String historyReturn(JSONArray history, int baseIndex, long targetMs, long toleranceMs) {
        JSONObject base = history.optJSONObject(baseIndex);
        if (base == null) return "bekleniyor";
        long baseTime = base.optLong("time", 0);
        double basePrice = base.optDouble("price", 0);
        if (baseTime <= 0 || basePrice <= 0) return "bekleniyor";
        String name = base.optString("name");
        boolean coin = base.optBoolean("coin", false);
        JSONObject nearest = null;
        long nearestDiff = Long.MAX_VALUE;
        for (int j = 0; j < history.length(); j++) {
            if (j == baseIndex) continue;
            JSONObject candidate = history.optJSONObject(j);
            if (candidate == null || !name.equals(candidate.optString("name")) ||
                    coin != candidate.optBoolean("coin", false)) continue;
            long elapsed = candidate.optLong("time", 0) - baseTime;
            if (elapsed < targetMs || elapsed > targetMs + toleranceMs) continue;
            long diff = elapsed - targetMs;
            if (diff < nearestDiff && candidate.optDouble("price", 0) > 0) {
                nearest = candidate;
                nearestDiff = diff;
            }
        }
        if (nearest == null) return "bekleniyor";
        double change = (nearest.optDouble("price", 0) - basePrice) * 100.0 / basePrice;
        return String.format(Locale.US, "%+.2f%%", change);
    }

    private void showPaperBuyDialog(Scan scan) {
        EditText quantity = new EditText(this);
        quantity.setSingleLine(true);
        quantity.setInputType(8194);
        quantity.setText("1");
        quantity.setHint("Miktar / adet");
        new android.app.AlertDialog.Builder(this)
                .setTitle("Sanal alım • " + scan.q.name)
                .setMessage("Anlık referans fiyatı: " + scan.q.priceText + " " + currencyFor(scan.q.coin) +
                        "\nGerçek emir gönderilmez. Deneme işlemi kaydedilir.")
                .setView(quantity)
                .setPositiveButton("SANAL AL", (dialog, which) -> {
                    double qty;
                    try { qty = Double.parseDouble(quantity.getText().toString().trim()); }
                    catch (Exception e) { qty = 0; }
                    if (qty <= 0 || scan.q.price <= 0) {
                        Toast.makeText(this, "Geçerli bir miktar gir.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    addPaperPosition(scan, qty);
                })
                .setNegativeButton("İPTAL", null).show();
    }

    private void addPaperPosition(Scan scan, double quantity) {
        synchronized (MainActivity.class) {
            JSONArray positions;
            try { positions = new JSONArray(paperPrefs().getString("paper_positions", "[]")); }
            catch (Exception e) { positions = new JSONArray(); }
            JSONObject p = new JSONObject();
            try {
                p.put("id", System.currentTimeMillis());
                p.put("name", scan.q.name);
                p.put("coin", scan.q.coin);
                p.put("currency", currencyFor(scan.q.coin));
                p.put("entry", scan.q.price);
                p.put("current", scan.q.price);
                p.put("quantity", quantity);
                p.put("score", scan.score);
                p.put("opened", System.currentTimeMillis());
                positions.put(p);
                paperPrefs().edit().putString("paper_positions", positions.toString()).apply();
            } catch (Exception e) {
                Toast.makeText(this, "Sanal işlem kaydedilemedi.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Toast.makeText(this, "Sanal alım kaydedildi: " + scan.q.name, Toast.LENGTH_LONG).show();
    }

    private void updatePaperPositionPrices(ArrayList<Scan> scans) {
        synchronized (MainActivity.class) {
            JSONArray positions;
            try { positions = new JSONArray(paperPrefs().getString("paper_positions", "[]")); }
            catch (Exception e) { return; }
            boolean changed = false;
            for (int i = 0; i < positions.length(); i++) {
                JSONObject p = positions.optJSONObject(i);
                if (p == null) continue;
                String name = p.optString("name");
                boolean coin = p.optBoolean("coin");
                for (Scan scan : scans) {
                    if (scan.q.name.equals(name) && scan.q.coin == coin) {
                        try { p.put("current", scan.q.price); changed = true; }
                        catch (Exception ignored) { }
                        break;
                    }
                }
            }
            if (changed) paperPrefs().edit().putString("paper_positions", positions.toString()).apply();
        }
    }

    private String currencyFor(boolean coin) {
        return coin ? "USDT" : "TL";
    }

    private String formatPortfolioPrice(double value, boolean coin) {
        if (!coin) return String.format(Locale.US, "%.2f", value);
        if (Math.abs(value) < 0.0001) return String.format(Locale.US, "%.8f", value);
        if (Math.abs(value) < 0.01) return String.format(Locale.US, "%.6f", value);
        if (Math.abs(value) < 1) return String.format(Locale.US, "%.4f", value);
        return String.format(Locale.US, "%.3f", value);
    }

    private void refreshPaperPricesThenShow() {
        refreshPool.execute(() -> {
            JSONArray ps;
            try { ps = new JSONArray(paperPrefs().getString("paper_positions", "[]")); }
            catch (Exception e) { ps = new JSONArray(); }
            boolean changed = false;
            for (int i = 0; i < ps.length(); i++) {
                JSONObject p = ps.optJSONObject(i);
                if (p == null) continue;
                String name = p.optString("name", "");
                boolean coin = p.optBoolean("coin", false);
                try {
                    Quote q = coin ? fetchBinance(name + "USDT") : fetchYahoo(name + ".IS", name);
                    p.put("current", q.price);
                    p.put("priceUpdated", System.currentTimeMillis());
                    changed = true;
                } catch (Exception ignored) { }
            }
            if (changed) paperPrefs().edit().putString("paper_positions", ps.toString()).apply();
            runOnUiThread(() -> showPaperPortfolio());
        });
    }

    private void choosePaperPositionToClose() {
        JSONArray ps;
        try { ps = new JSONArray(paperPrefs().getString("paper_positions", "[]")); }
        catch (Exception e) { ps = new JSONArray(); }
        ArrayList<Integer> indexes = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < ps.length(); i++) {
            JSONObject p = ps.optJSONObject(i);
            if (p == null) continue;
            indexes.add(i);
            names.add(p.optString("name") + " • Alış " +
                    formatPortfolioPrice(p.optDouble("entry", 0), p.optBoolean("coin", false)) + " " +
                    p.optString("currency", currencyFor(p.optBoolean("coin", false))));
        }
        if (names.isEmpty()) {
            Toast.makeText(this, "Kapatılacak açık işlem yok.", Toast.LENGTH_SHORT).show();
            return;
        }
        new android.app.AlertDialog.Builder(this).setTitle("Kapatılacak sanal işlemi seç")
                .setItems(names.toArray(new String[0]), (dialog, which) -> confirmClosePaperPosition(indexes.get(which)))
                .setNegativeButton("GERİ", null).show();
    }

    private void showPaperPortfolio() {
        JSONArray positions;
        JSONArray closed;
        try { positions = new JSONArray(paperPrefs().getString("paper_positions", "[]")); }
        catch (Exception e) { positions = new JSONArray(); }
        try { closed = new JSONArray(paperPrefs().getString("paper_closed", "[]")); }
        catch (Exception e) { closed = new JSONArray(); }

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(14, 8, 14, 8);
        scroll.addView(content);

        double unrealizedTL = 0, unrealizedUSDT = 0;
        int openCount = 0;
        for (int i = 0; i < positions.length(); i++) {
            JSONObject p = positions.optJSONObject(i);
            if (p == null) continue;
            final int positionIndex = i;
            double entry = p.optDouble("entry", 0);
            double current = p.optDouble("current", entry);
            double qty = p.optDouble("quantity", 0);
            boolean coin = p.optBoolean("coin", false);
            String currency = p.optString("currency", currencyFor(coin));
            double cost = entry * qty, value = current * qty, pnl = value - cost;
            double pct = cost > 0 ? pnl * 100.0 / cost : 0;
            if ("USDT".equals(currency)) unrealizedUSDT += pnl; else unrealizedTL += pnl;
            openCount++;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(14, 12, 14, 12);
            setRoundedBackground(card, Color.rgb(248, 250, 252), 14);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.setMargins(0, 5, 0, 7);
            card.setLayoutParams(cp);

            TextView heading = new TextView(this);
            heading.setText("● " + p.optString("name") + "  •  AÇIK İŞLEM");
            heading.setTextSize(16);
            heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            heading.setTextColor(Color.rgb(35, 45, 60));
            card.addView(heading);

            TextView detail = new TextView(this);
            detail.setText("Miktar: " + String.format(Locale.US, "%.8g", qty) + "\n" +
                    "Alış fiyatı: " + formatPortfolioPrice(entry, coin) + " " + currency + "\n" +
                    "Güncel fiyat: " + formatPortfolioPrice(current, coin) + " " + currency + "\n" +
                    "Toplam maliyet: " + formatPortfolioPrice(cost, coin) + " " + currency + "\n" +
                    "Güncel değer: " + formatPortfolioPrice(value, coin) + " " + currency);
            detail.setTextSize(13);
            detail.setTextColor(Color.rgb(75, 82, 95));
            detail.setPadding(0, 7, 0, 7);
            card.addView(detail);

            TextView profit = new TextView(this);
            profit.setText("Kâr / Zarar: " + String.format(Locale.US, "%+.4f", pnl) + " " + currency +
                    "  (" + String.format(Locale.US, "%+.2f%%", pct) + ")");
            profit.setTextSize(15);
            profit.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            profit.setTextColor(pnl >= 0 ? Color.rgb(30, 125, 75) : Color.rgb(190, 55, 55));
            card.addView(profit);

            TextView hint = new TextView(this);
            hint.setText("İşlemi kapatmak için bu karta dokun");
            hint.setTextSize(11);
            hint.setTextColor(Color.rgb(95, 105, 120));
            hint.setPadding(0, 5, 0, 0);
            card.addView(hint);
            card.setOnClickListener(v -> confirmClosePaperPosition(positionIndex));
            content.addView(card);
        }

        TextView summary = new TextView(this);
        summary.setText("PORTFÖY ÖZETİ\n" +
                "Açık işlem: " + openCount + "\n" +
                "Açık K/Z (TL): " + String.format(Locale.US, "%+.2f TL", unrealizedTL) + "\n" +
                "Açık K/Z (USDT): " + String.format(Locale.US, "%+.4f USDT", unrealizedUSDT) + "\n" +
                "TL ve USDT bakiyeleri ayrı hesaplanır; birbirine eklenmez.");
        summary.setTextSize(14);
        summary.setTextColor(Color.rgb(35, 45, 60));
        summary.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        summary.setPadding(4, 8, 4, 12);
        content.addView(summary, 0);

        if (openCount == 0) {
            TextView empty = new TextView(this);
            empty.setText("Henüz açık sanal işlem yok. Bir sinyalin detayına girip SANAL ALIM seçebilirsin.");
            empty.setTextSize(14);
            empty.setTextColor(Color.rgb(85, 90, 100));
            empty.setPadding(4, 8, 4, 14);
            content.addView(empty);
        }

        if (closed.length() > 0) {
            TextView closedHeading = new TextView(this);
            closedHeading.setText("SON KAPATILAN İŞLEMLER");
            closedHeading.setTextSize(14);
            closedHeading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            closedHeading.setTextColor(Color.rgb(65, 75, 90));
            closedHeading.setPadding(4, 12, 4, 5);
            content.addView(closedHeading);
            for (int i = closed.length() - 1, shown = 0; i >= 0 && shown < 10; i--, shown++) {
                JSONObject p = closed.optJSONObject(i);
                if (p == null) continue;
                boolean coin = p.optBoolean("coin", false);
                String currency = p.optString("currency", currencyFor(coin));
                TextView closedLine = new TextView(this);
                closedLine.setText(p.optString("name") + " • Sonuç: " +
                        String.format(Locale.US, "%+.4f", p.optDouble("pnl", 0)) + " " + currency +
                        " • " + new java.text.SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
                        .format(new Date(p.optLong("closed", 0))));
                closedLine.setTextSize(12);
                closedLine.setTextColor(Color.rgb(85, 90, 100));
                closedLine.setPadding(4, 5, 4, 5);
                content.addView(closedLine);
            }
        }

        new android.app.AlertDialog.Builder(this).setTitle("Sanal Portföy")
                .setView(scroll)
                .setPositiveButton("FİYATLARI YENİLE", (dialog, which) -> refreshPaperPricesThenShow())
                .setNeutralButton("İŞLEM KAPAT", (dialog, which) -> choosePaperPositionToClose())
                .setNegativeButton("KAPAT", null).show();
    }

    private void confirmClosePaperPosition(int index) {
        JSONArray positions;
        try { positions = new JSONArray(paperPrefs().getString("paper_positions", "[]")); }
        catch (Exception e) { return; }
        JSONObject p = positions.optJSONObject(index);
        if (p == null) return;
        double entry = p.optDouble("entry", 0);
        double current = p.optDouble("current", entry);
        double qty = p.optDouble("quantity", 0);
        double pnl = (current - entry) * qty;
        new android.app.AlertDialog.Builder(this)
                .setTitle("Sanal işlemi kapat?")
                .setMessage(p.optString("name") + "\nKapanış referans fiyatı: " +
                        formatPortfolioPrice(current, p.optBoolean("coin", false)) + " " +
                        p.optString("currency", currencyFor(p.optBoolean("coin", false))) + "\nSanal K/Z: " +
                        String.format(Locale.US, "%+.2f", pnl) + " " +
                        p.optString("currency", currencyFor(p.optBoolean("coin", false))) +
                        "\nBu, son kaydedilen fiyattır; canlı fiyat garantisi değildir.")
                .setPositiveButton("İŞLEMİ KAPAT", (dialog, which) -> closePaperPosition(index))
                .setNegativeButton("GERİ", null).show();
    }

    private void closePaperPosition(int index) {
        synchronized (MainActivity.class) {
            JSONArray positions, closed;
            try { positions = new JSONArray(paperPrefs().getString("paper_positions", "[]")); }
            catch (Exception e) { return; }
            try { closed = new JSONArray(paperPrefs().getString("paper_closed", "[]")); }
            catch (Exception e) { closed = new JSONArray(); }
            if (index < 0 || index >= positions.length()) return;
            JSONObject p = positions.optJSONObject(index);
            if (p == null) return;
            double entry = p.optDouble("entry", 0);
            double current = p.optDouble("current", entry);
            double qty = p.optDouble("quantity", 0);
            try {
                p.put("pnl", (current - entry) * qty);
                p.put("closed", System.currentTimeMillis());
                closed.put(p);
                while (closed.length() > 100) closed.remove(0);
                positions.remove(index);
                paperPrefs().edit().putString("paper_positions", positions.toString())
                        .putString("paper_closed", closed.toString()).apply();
            } catch (Exception ignored) { }
        }
        Toast.makeText(this, "Sanal işlem kapatıldı ve sonucu kaydedildi.", Toast.LENGTH_LONG).show();
        showPaperPortfolio();
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

    private String num(double x, boolean coin) {
        if (x <= 0) return "-";
        return formatPortfolioPrice(x, coin);
    }

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

        String unit = currencyFor(q.coin);
        return "ALIM–SATIM PLANLAYICI ("+unit+")\\n\\n"+
                "Alım bölgesi: "+num(entryLow, q.coin)+" – "+num(entryHigh, q.coin)+" "+unit+"\\n"+
                "Kırılım seviyesi: "+num(breakout, q.coin)+" "+unit+"\\n"+
                "Hedef 1: "+num(target1, q.coin)+" "+unit+"\\n"+
                "Hedef 2: "+num(target2, q.coin)+" "+unit+"\\n"+
                "Hedef 3: "+num(target3, q.coin)+" "+unit+"\\n"+
                "Zarar-kes: "+num(stop, q.coin)+" "+unit+"\\n"+
                "Risk/Getiri (H1): "+String.format(Locale.US,"%.2f",rr)+"R\\n\\n"+
                "Destek: "+num(support, q.coin)+" "+unit+"\\n"+
                "Direnç: "+num(resistance, q.coin)+" "+unit+"\\n"+
                "ATR14: "+num(atr, q.coin)+" "+unit+"\\n\\n"+
                "Durum: "+condition+"\\n\\n"+
                "Hesaplama; destek/direnç, ATR14 ve mevcut fiyat kullanılarak dinamik yapılır.\\n"+
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
        String name,priceText,currency; boolean coin; double price,change,rsi,sma,macd,volumeRatio,atr,support,resistance;
        Quote(String n,String p,double pr,double ch,double r,double s){name=n;priceText=p;price=pr;change=ch;rsi=r;sma=s;macd=0;volumeRatio=1;atr=0;support=0;resistance=0;coin=false;currency="TL";}
        Quote(String n,String p,double pr,double ch,double r,double s,double m,double vr,double a,double sup,double res,boolean isCoin){name=n;priceText=p;price=pr;change=ch;rsi=r;sma=s;macd=m;volumeRatio=vr;atr=a;support=sup;resistance=res;coin=isCoin;currency=isCoin?"USDT":"TL";}
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
