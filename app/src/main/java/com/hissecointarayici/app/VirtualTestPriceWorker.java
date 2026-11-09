package com.hissecointarayici.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class VirtualTestPriceWorker extends Worker {
    private static final String PREFS = "hisse_coin_paper_v1";
    private static final String TEST_KEY = "virtual_signal_test";
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long HORIZON_TOLERANCE_MS = 6L * 60L * 60L * 1000L;
    private static final long MAX_QUOTE_AGE_MS = 72L * 60L * 60L * 1000L;
    private static final String AUDIT_CHANNEL_ID = "virtual_test_background_audit";
    private static final int AUDIT_NOTIFICATION_ID = 7144;
    private static final String HISTORY_KEY = "virtual_test_bg_history";

    public VirtualTestPriceWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long attemptAt = System.currentTimeMillis();
        prefs.edit().putLong("virtual_test_bg_last_attempt", attemptAt).commit();

        JSONArray test;
        try {
            test = new JSONArray(prefs.getString(TEST_KEY, "[]"));
        } catch (Exception e) {
            saveStatus(prefs, attemptAt, 0, "Sanal test kaydı okunamadı");
            recordBackgroundRun(prefs, attemptAt, 0, 0, "Sanal test kaydı okunamadı", false);
            return Result.failure();
        }
        if (test.length() == 0) {
            prefs.edit().putInt("virtual_test_bg_last_count", 0)
                    .putString("virtual_test_bg_last_error", "Aktif sanal test kaydı yok")
                    .commit();
            recordBackgroundRun(prefs, attemptAt, 0, 0, "Aktif sanal test kaydı yok", false);
            return Result.success();
        }

        int updated = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();
        long now = System.currentTimeMillis();

        for (int i = 0; i < test.length(); i++) {
            JSONObject item = test.optJSONObject(i);
            if (item == null) { failed++; appendError(errors, "Bozuk kayıt"); continue; }
            String name = item.optString("name", "").trim();
            boolean coin = item.optBoolean("coin", false);
            if (name.isEmpty()) { failed++; appendError(errors, "Varlık adı eksik"); continue; }

            try {
                PriceQuote quote = coin ? fetchBinance(name) : fetchYahoo(name);
                long age = now - quote.timestamp;
                if (quote.price <= 0 || (Double.isNaN(quote.price) || Double.isInfinite(quote.price))
                        || quote.timestamp <= 0 || age < -5L * 60L * 1000L || age > MAX_QUOTE_AGE_MS) {
                    throw new Exception("Fiyat verisi eski veya geçersiz");
                }

                item.put("current", quote.price);
                item.put("lastUpdate", quote.timestamp);
                captureHorizons(item, quote.price, quote.timestamp);
                updated++;
            } catch (Exception e) {
                failed++;
                appendError(errors, name + ": " + safeMessage(e));
            }
        }

        boolean saved = prefs.edit().putString(TEST_KEY, test.toString()).commit();
        if (!saved) {
            saveStatus(prefs, attemptAt, updated, "Fiyat kayıtları kalıcı olarak yazılamadı");
            recordBackgroundRun(prefs, attemptAt, updated, failed, "Fiyat kayıtları kalıcı olarak yazılamadı", false);
            return Result.retry();
        }

        long completedAt = System.currentTimeMillis();
        if (updated > 0) {
            prefs.edit().putLong("virtual_test_bg_last_success", completedAt)
                    .putInt("virtual_test_bg_last_count", updated)
                    .putString("virtual_test_bg_last_error", errors.toString())
                    .commit();
        } else {
            prefs.edit().putInt("virtual_test_bg_last_count", 0)
                    .putString("virtual_test_bg_last_error", errors.length() == 0 ? "Yeni fiyat alınamadı" : errors.toString())
                    .commit();
        }
        recordBackgroundRun(prefs, attemptAt, updated, failed, errors.toString(), updated > 0 && failed == 0);
        if (updated > 0) postAuditNotification(updated, test.length(), completedAt);
        return failed > 0 ? Result.retry() : Result.success();
    }


    private void recordBackgroundRun(SharedPreferences prefs, long attemptAt, int updated, int failed,
                                    String error, boolean success) {
        try {
            JSONArray history = new JSONArray(prefs.getString(HISTORY_KEY, "[]"));
            JSONObject record = new JSONObject();
            long completedAt = System.currentTimeMillis();
            record.put("attemptAt", attemptAt);
            record.put("completedAt", completedAt);
            record.put("updated", updated);
            record.put("failed", failed);
            record.put("success", success);
            record.put("error", error == null ? "" : error);
            history.put(record);
            while (history.length() > 10) history.remove(0);
            prefs.edit().putString(HISTORY_KEY, history.toString()).commit();
        } catch (Exception ignored) { }
    }

    private void postAuditNotification(int updated, int total, long completedAt) {
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(AUDIT_CHANNEL_ID,
                    "Sanal test arka plan kontrolü", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Sanal test fiyat kontrolünün son başarılı zamanını gösterir");
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(false);
            manager.createNotificationChannel(channel);
        }
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault());
        String stamp = fmt.format(new java.util.Date(completedAt));
        String body = (updated == total ? "Başarılı • " : "Kısmi • ") + updated + "/" + total + " varlık • " + stamp;
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, AUDIT_CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Sanal test: arka plan kontrolü")
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setShowWhen(true)
                .setWhen(completedAt);
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, AUDIT_NOTIFICATION_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        builder.setContentIntent(pending);
        manager.notify(AUDIT_NOTIFICATION_ID, builder.build());
    }

    private static void captureHorizons(JSONObject item, double price, long quoteTime) throws Exception {
        // Existing legacy records still receive current prices, but are never given invented
        // 1d/3d/7d checkpoints. Only records already using the timestamped format are eligible.
        if (item.optInt("horizonCaptureVersion", 0) < 1) return;
        long start = item.optLong("time", 0L);
        if (start <= 0) return;
        long elapsed = quoteTime - start;
        for (int days : new int[]{1, 3, 7}) {
            String priceKey = "horizon" + days + "Price";
            String timeKey = "horizon" + days + "Time";
            String missedKey = "horizon" + days + "Missed";
            long target = days * DAY_MS;
            if (!item.has(priceKey) && !item.optBoolean(missedKey, false)) {
                if (elapsed >= target && elapsed <= target + HORIZON_TOLERANCE_MS) {
                    item.put(priceKey, price);
                    item.put(timeKey, quoteTime);
                } else if (elapsed > target + HORIZON_TOLERANCE_MS) {
                    item.put(missedKey, true);
                }
            }
        }
    }

    private static PriceQuote fetchBinance(String base) throws Exception {
        String symbol = base.toUpperCase(Locale.ROOT).endsWith("USDT") ? base.toUpperCase(Locale.ROOT) : base.toUpperCase(Locale.ROOT) + "USDT";
        String url = "https://api.binance.com/api/v3/ticker/24hr?symbol=" + URLEncoder.encode(symbol, "UTF-8");
        JSONObject json = getJson(url);
        double price = json.optDouble("lastPrice", 0.0);
        long timestamp = json.optLong("closeTime", 0L);
        return new PriceQuote(price, timestamp);
    }

    private static PriceQuote fetchYahoo(String symbol) throws Exception {
        String encoded = URLEncoder.encode(symbol + ".IS", "UTF-8");
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + encoded + "?interval=1d&range=5d";
        JSONObject root = getJson(url);
        JSONObject chart = root.getJSONObject("chart");
        if (chart.optJSONArray("error") != null && chart.optJSONArray("error").length() > 0)
            throw new Exception("Yahoo fiyat hatası");
        JSONObject result = chart.getJSONArray("result").getJSONObject(0);
        JSONObject meta = result.getJSONObject("meta");
        double price = meta.optDouble("regularMarketPrice", 0.0);
        long timestamp = meta.optLong("regularMarketTime", 0L) * 1000L;
        return new PriceQuote(price, timestamp);
    }

    private static JSONObject getJson(String address) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 HisseCoinTarayici/3.0");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                return new JSONObject(body.toString());
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void appendError(StringBuilder errors, String message) {
        if (errors.length() > 0) errors.append(" | ");
        if (errors.length() < 360) errors.append(message.length() > 100 ? message.substring(0, 100) : message);
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty() ? e.getClass().getSimpleName() : message;
    }

    private static void saveStatus(SharedPreferences prefs, long attemptAt, int count, String error) {
        prefs.edit().putLong("virtual_test_bg_last_attempt", attemptAt)
                .putInt("virtual_test_bg_last_count", count)
                .putString("virtual_test_bg_last_error", error).commit();
    }

    private static final class PriceQuote {
        final double price;
        final long timestamp;
        PriceQuote(double price, long timestamp) { this.price = price; this.timestamp = timestamp; }
    }
}
