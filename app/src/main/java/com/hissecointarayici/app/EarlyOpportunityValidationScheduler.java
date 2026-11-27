package com.hissecointarayici.app;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public final class EarlyOpportunityValidationScheduler {
    private static final String PERIODIC_NAME = "early-opportunity-validation-periodic";
    private static final String IMMEDIATE_NAME = "early-opportunity-validation-now";

    private EarlyOpportunityValidationScheduler() { }

    public static void ensureScheduled(Context context) {
        Context app = context.getApplicationContext();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
                EarlyOpportunityValidationWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic);

        OneTimeWorkRequest immediate = new OneTimeWorkRequest.Builder(EarlyOpportunityValidationWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(app).enqueueUniqueWork(
                IMMEDIATE_NAME, ExistingWorkPolicy.KEEP, immediate);
    }
}
