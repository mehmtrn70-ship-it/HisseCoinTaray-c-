package com.hissecointarayici.app;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public final class VirtualTestBackgroundScheduler {
    private static final String PERIODIC_NAME = "virtual-test-price-capture-periodic";
    private static final String IMMEDIATE_NAME = "virtual-test-price-capture-now";

    private VirtualTestBackgroundScheduler() { }

    public static void ensureScheduled(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
                VirtualTestPriceWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(new Data.Builder().putString("trigger_type", "periodic").build())
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic);

        OneTimeWorkRequest immediate = new OneTimeWorkRequest.Builder(VirtualTestPriceWorker.class)
                .setConstraints(constraints)
                .setInputData(new Data.Builder().putString("trigger_type", "immediate").build())
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                IMMEDIATE_NAME, ExistingWorkPolicy.KEEP, immediate);
    }
}
