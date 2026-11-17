package com.hissecointarayici.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class EarlyOpportunityValidationWorker extends Worker {
    private static final String PREFS = "early_opportunity_v2";
    private static final String HISTORY_KEY = "validated_signals_v1";
    private static final String LAST_ATTEMPT = "validation_bg_last_attempt";
    private static final String LAST_SUCCESS = "validation_bg_last_success";
    private static final String LAST_UPDATED = "validation_bg_last_updated";
    private static final String LAST_FAILED = "validation_bg_last_failed";
    private static final String LAST_ERROR = "validation_bg_last_error";
    private static final long[] TARGETS = {
        60L*60L*1000L, 4L*60L*60L*1000L, 24L*60L*60L*1000L,
        3L*24L*60L*60L*1000L, 7L*24L*60L*60L*1000L
    };
    // Grace is now used only to label a measurement as late, never to discard it.
    private static final long[] GRACE = {
        20L*60L*1000L, 30L*60L*1000L, 2L*60L*60L*1000L,
        6L*60L*60L*1000L, 12L*60L*60L*1000L
    };
    private static final String[] KEYS = {"m1h","m4h","m24h","m3d","m7d"};

    public EarlyOpportunityValidationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long attemptAt = System.currentTimeMillis();
        prefs.edit().putLong(LAST_ATTEMPT, attemptAt).commit();

        JSONArray initial;
        try {
            initial = new JSONArray(prefs.getString(HISTORY_KEY, "[]"));
        } catch (Exception e) {
            saveFailure(prefs, 0, 0, "Doğrulama kayıtları okunamadı");
            return Result.failure();
        }
        if (initial.length() == 0) {
            prefs.edit().putInt(LAST_UPDATED, 0).putInt(LAST_FAILED, 0)
                    .putString(LAST_ERROR, "Henüz teyitli sinyal kaydı yok").commit();
            return Result.success();
        }

        Map<String, Quote> quotes;
        try {
            quotes = fetchAllBinanceQuotes();
        } catch (Exception e) {
            saveFailure(prefs, 0, initial.length(), safeMessage(e));
            return Result.retry();
        }

        long completedAt = System.currentTimeMillis();
        int measured = 0;
        int missingPrice = 0;
        int pending = 0;
        int missed = 0;
        boolean saved;

        // Foreground scanner and background worker share this lock and the same JSON store.
        synchronized (EarlyOpportunityActivity.class) {
            try {
                // Re-read after the network request so newer foreground records are not overwritten.
                JSONArray records = new JSONArray(prefs.getString(HISTORY_KEY, "[]"));
                for (int i = 0; i < records.length(); i++) {
                    JSONObject record = records.optJSONObject(i);
                    if (record == null) continue;
                    long entryAt = record.optLong("entryAt", 0L);
                    double entry = record.optDouble("entryPrice", 0.0);
                    if (entryAt <= 0 || entry <= 0) continue;

                    long age = completedAt - entryAt;
                    if (age < 0) continue;
                    String symbol = record.optString("symbol", "");
                    Quote quote = quotes.get(symbol);
                    boolean hasPrice = quote != null && quote.price > 0
                            && !Double.isNaN(quote.price) && !Double.isInfinite(quote.price);
                    boolean priceNeededNow = false;
                    for (int j = 0; j < TARGETS.length; j++) {
                        if (!record.has(KEYS[j]) && age >= TARGETS[j]) priceNeededNow = true;
                    }
                    if (priceNeededNow && !hasPrice) missingPrice++;

                    for (int j = 0; j < TARGETS.length; j++) {
                        String key = KEYS[j];
                        if (record.has(key)) continue;

                        // Re-open old missed checkpoints; they can now be measured late with
                        // their true elapsed time instead of being permanently discarded.
                        record.remove(key + "_missed");
                        record.remove(key + "_missedAt");

                        if (age >= TARGETS[j] && hasPrice) {
                            JSONObject checkpoint = new JSONObject();
                            checkpoint.put("price", quote.price);
                            checkpoint.put("returnPct", (quote.price / entry - 1.0) * 100.0);
                            checkpoint.put("measuredAt", completedAt);
                            checkpoint.put("elapsedMs", age);
                            checkpoint.put("targetMs", TARGETS[j]);
                            checkpoint.put("delayMs", Math.max(0L, age - TARGETS[j]));
                            checkpoint.put("late", age > TARGETS[j] + GRACE[j]);
                            checkpoint.put("exchangeCloseTime", quote.closeTime);
                            checkpoint.put("source", "Binance ticker lastPrice; app capture time");
                            record.put(key, checkpoint);
                            measured++;
                        }
                    }
                }

                // These are cumulative outstanding checkpoints, not just changes in this run.
                for (int i = 0; i < records.length(); i++) {
                    JSONObject record = records.optJSONObject(i);
                    if (record == null || record.optLong("entryAt", 0L) <= 0
                            || record.optDouble("entryPrice", 0.0) <= 0) continue;
                    for (String key : KEYS) {
                        if (record.has(key)) continue;
                        if (record.optBoolean(key + "_missed", false)) missed++;
                        else pending++;
                    }
                }

                saved = prefs.edit().putString(HISTORY_KEY, records.toString())
                        .putLong(LAST_SUCCESS, completedAt)
                        .putInt(LAST_UPDATED, measured)
                        .putInt(LAST_FAILED, missingPrice)
                        .putString(LAST_ERROR, (missingPrice > 0
                                ? "Fiyatı alınamayan sinyal: " + missingPrice + " • "
                                : "Hata yok • ")
                                + "Bekleyen: " + pending + " • Kaçırılan: " + missed)
                        .commit();
            } catch (Exception e) {
                saveFailure(prefs, measured, missingPrice, "Ölçüm kaydı hazırlanamadı: " + safeMessage(e));
                return Result.retry();
            }
        }

        if (!saved) return Result.retry();
        return missingPrice > 0 ? Result.retry() : Result.success();
    }

    private Map<String, Quote> fetchAllBinanceQuotes() throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://api.binance.com/api/v3/ticker/24hr");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "HisseCoinTarayici/3.0");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("Binance HTTP " + code);
            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                JSONArray array = new JSONArray(body.toString());
                Map<String, Quote> result = new HashMap<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject ticker = array.optJSONObject(i);
                    if (ticker == null) continue;
                    String symbol = ticker.optString("symbol", "");
                    if (!symbol.endsWith("USDT")) continue;
                    double price = ticker.optDouble("lastPrice", 0.0);
                    long closeTime = ticker.optLong("closeTime", 0L);
                    if (price > 0 && !Double.isNaN(price) && !Double.isInfinite(price))
                        result.put(symbol, new Quote(price, closeTime));
                }
                return result;
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void saveFailure(SharedPreferences prefs, int updated, int failed, String message) {
        prefs.edit().putInt(LAST_UPDATED, updated).putInt(LAST_FAILED, failed)
                .putString(LAST_ERROR, message == null ? "Bilinmeyen hata" : message).commit();
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty() ? e.getClass().getSimpleName() : message;
    }

    private static final class Quote {
        final double price;
        final long closeTime;
        Quote(double price, long closeTime) { this.price = price; this.closeTime = closeTime; }
    }
}
