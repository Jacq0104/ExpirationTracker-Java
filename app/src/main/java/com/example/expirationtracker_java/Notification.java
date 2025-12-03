package com.example.expirationtracker_java;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.expirationtracker_java.data.Database;
import com.example.expirationtracker_java.data.entity.RecordEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;


public class Notification extends Worker {

    private static final String CHANNEL_ID = "expiry_channel";

    public Notification(@NonNull Context context,
                           @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // 👉 Android 13+ permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return Result.success();  // no permission-> do nothing
            }
        }

//        // test notification
//        NotificationCompat.Builder testBuilder =
//                new NotificationCompat.Builder(context, CHANNEL_ID)
//                        .setSmallIcon(android.R.drawable.ic_dialog_info)
//                        .setContentTitle("Test from Worker")
//                        .setContentText("Worker is running")
//                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                        .setAutoCancel(true);
//
//        NotificationManagerCompat.from(context).notify(1234, testBuilder.build());

        Database db = Database.getDatabase(context);

        //
        List<RecordEntity> records = db.recordDao().getAll();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate today = LocalDate.now();

        int soonCount = 0;

        for (RecordEntity r : records) {
            if (r.expiredDate == null || r.expiredDate.isEmpty()) continue;

            try {
                LocalDate expiry = LocalDate.parse(r.expiredDate, formatter);
                long diff = ChronoUnit.DAYS.between(today, expiry);

                int soonDays = (r.notifyDaysBefore > 0) ? r.notifyDaysBefore : 14;

                if (diff >= 0 && diff <= soonDays) {
                    soonCount++;
                }
            } catch (Exception e) {
                // ?ignore this when fail
            }
        }

        if (soonCount > 0) {
            showSummaryNotification(context, soonCount);
        }

        return Result.success();
    }

    private void showSummaryNotification(Context context, int count) {
        String title = "Expiration Tracker";
        String content = "You have " + count + " items marked as \"soon\".";

        // Android 13+ permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return;    // no permission-> do nothing
            }
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        // ?用固定 ID 就好，這樣每天只會覆蓋同一則通知
        nm.notify(1001, builder.build());
    }
}
