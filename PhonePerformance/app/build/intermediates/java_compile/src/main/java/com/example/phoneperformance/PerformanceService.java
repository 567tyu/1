package com.example.phoneperformance;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.annotation.Nullable;

public class PerformanceService extends Service {

    private static final String CHANNEL_ID = "PerformanceChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private Handler handler;
    private Runnable runnable;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        isRunning = true;
        startMonitoring();
        
        return START_STICKY;
    }

    private void startMonitoring() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // 在这里可以更新通知中的性能数据
                    updateNotification();
                    handler.postDelayed(this, 1000); // 每秒更新
                }
            }
        };
        handler.post(runnable);
    }

    private void updateNotification() {
        Notification notification = buildNotification();
        NotificationManager nm = (NotificationManager) 
            getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = getResources().getString(R.string.channel_name);
            String description = getResources().getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = 
                getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("手机性能监控")
            .setContentText("正在监控中...")
            .setSmallIcon(R.drawable.ic_monitoring)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
        super.onDestroy();
    }
}
