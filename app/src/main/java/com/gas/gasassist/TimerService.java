package com.gas.gasassist;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

// deprecated service
public class TimerService extends Service {
    public static final String CHANNEL_ID = "TimerServiceChannel";
    public static final String ACTION_STOP_TIMER = "STOP_TIMER";
    private CountDownTimer countDownTimer;
    private long remainingTime;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_TIMER.equals(intent.getAction())) {
            stopTimer();
            return START_NOT_STICKY;
        }

        long duration = intent.getLongExtra("duration", 0);
        startTimer(duration);
        return START_NOT_STICKY;
    }

    private void startTimer(long duration) {
        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTime = millisUntilFinished;
                updateNotification(remainingTime);
            }

            @Override
            public void onFinish() {
                playNotification();
                stopSelf();
            }
        };
        countDownTimer.start();
    }

    @SuppressLint("ForegroundServiceType")
    private void updateNotification(long millisUntilFinished) {
        int minutes = (int) (millisUntilFinished / 1000) / 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        String timeLeft = String.format("%02d:%02d", minutes, seconds);

        Intent stopIntent = new Intent(this, TimerService.class);
        stopIntent.setAction(ACTION_STOP_TIMER);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer Running")
                .setContentText("Remaining time: " + timeLeft)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
                .setOnlyAlertOnce(true)
                .build();

        startForeground(1, notification);
    }

    private void playNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer Finished")
                .setContentText("Your timer has ended.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(2, notification);
        // Воспроизведение звукового уведомления (необходимо добавить логику воспроизведения аудио)
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            stopForeground(true);
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Timer Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setSound(null, null);
            serviceChannel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
