package com.gas.gasassist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.HashMap;
import java.util.Map;

public class MultiTimerService extends Service {
    public static final String CHANNEL_ID = "MultiTimerServiceChannel";
    public static final String ACTION_STOP_TIMER = "STOP_TIMER";
    private Map<Integer, CountDownTimer> timers = new HashMap<>();
    private Map<Integer, String> timerNames = new HashMap<>();
    private int timerIdCounter = 1;
    private SoundPool soundPool;
    private Context context;
    private int soundId;
    private boolean loaded = false;

    private Map<Integer, Integer> soundIds = new HashMap<>();
    private Map<Integer, Boolean> soundPlaying = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        context = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(attributes)
                    .build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        }

        soundId = soundPool.load(context, R.raw.notification, 1);

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            loaded = true;
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_TIMER.equals(intent.getAction())) {
            int timerId = intent.getIntExtra("timerId", -1);
            if (timerId != -1) {
                stopTimer(timerId);
                stopNotificationSound(timerId);
            }
            return START_NOT_STICKY;
        }

        long duration = intent.getLongExtra("duration", 0);
        String timerName = intent.getStringExtra("timerName");
        startTimer(duration, timerName);
        return START_NOT_STICKY;
    }

    private void startTimer(long duration, String timerName) {
        final int timerId = timerIdCounter++;
        CountDownTimer countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateNotification(timerId, timerName, millisUntilFinished);
            }

            @Override
            public void onFinish() {
                playNotification(timerId, timerName);
                timers.remove(timerId);
                stopSelfIfNoTimers();
            }
        };
        timers.put(timerId, countDownTimer);
        timerNames.put(timerId, timerName);
        countDownTimer.start();
    }

    private void updateNotification(int timerId, String timerName, long millisUntilFinished) {
        int minutes = (int) (millisUntilFinished / 1000) / 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;
        String timeLeft = String.format("%02d:%02d", minutes, seconds);

        Intent stopIntent = new Intent(this, MultiTimerService.class);
        stopIntent.setAction(ACTION_STOP_TIMER);
        stopIntent.putExtra("timerId", timerId);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, timerId, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Таймер: " + timerName)
                .setContentText("Оставшееся время: " + timeLeft)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
                .setOnlyAlertOnce(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(timerId, notification);
    }

//    private void stopNotificationSound() {
//        if (soundPool != null && loaded && soundId != 0) {
//            soundPool.stop(soundId);
//            soundPool.release();
//            soundPool = null;
//        }
//        stopSelf();
//    }
    private void stopNotificationSound(int timerId) {
        Integer playId = soundIds.get(timerId);
        if (soundPool != null && loaded && playId != null) {
            soundPool.stop(playId);
            soundPlaying.put(timerId, false);
        }
    }

    private void releaseSoundPool() {
        if (soundPool != null) {
            for (Boolean isPlaying : soundPlaying.values()) {
                if (isPlaying) {
                    return; // Do not release if any sound is still playing
                }
            }
            soundPool.release();
            soundPool = null;
            loaded = false;
        }
    }


//    public void playNotification(int timerId, String timerName) {
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setContentTitle("Таймер " + timerName)
//                .setContentText("Таймер завершен")
//                .setSmallIcon(android.R.drawable.ic_dialog_info)
//                .setAutoCancel(true)
//                .build();
//
//        Intent stopIntent = new Intent(context, MultiTimerService.class);
//        stopIntent.setAction(ACTION_STOP_TIMER);
//        stopIntent.putExtra("timerId", timerId);
//        PendingIntent stopPendingIntent = PendingIntent.getService(context, timerId, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        notification.contentIntent = stopPendingIntent;
//        notificationManager.notify(timerId + 1000, notification);
//        notificationManager.cancel(timerId);
//
//        try {
//            if (loaded) {
//                Log.e("MultiTimerService", "Sound loaded, start playing");
//                soundPool.play(soundId, 1.0f, 1.0f, 0, -1, 1.0f);
//            } else {
//                Log.e("MultiTimerService", "Sound not loaded yet");
//                return;
//            }
//
//            new Handler().postDelayed(() -> stopNotificationSound(), 10000); // 10000ms = 10s
//
//        } catch (Exception ex) {
//            Log.e("MultiTimerService", "Exception occurred: " + ex.getMessage(), ex);
//        }
//    }

//    private void stopTimer(int timerId) {
//        CountDownTimer countDownTimer = timers.get(timerId);
//        if (countDownTimer != null) {
//            countDownTimer.cancel();
//            timers.remove(timerId);
//            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            notificationManager.cancel(timerId);
//            stopSelfIfNoTimers();
//        }
//    }


    private void playNotification(int timerId, String timerName) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Таймер: " + timerName)
                .setContentText("Таймер завершен")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build();

        Intent stopIntent = new Intent(context, MultiTimerService.class);
        stopIntent.setAction(ACTION_STOP_TIMER);
        stopIntent.putExtra("timerId", timerId);
        PendingIntent stopPendingIntent = PendingIntent.getService(context, timerId, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        notification.contentIntent = stopPendingIntent;
        notificationManager.notify(timerId + 1000, notification);
        notificationManager.cancel(timerId);

        try {
            if (loaded) {
                int playId = soundPool.play(soundId, 1.0f, 1.0f, 0, -1, 1.0f);
                soundIds.put(timerId, playId);
                soundPlaying.put(timerId, true);
            } else {
                Log.e("MultiTimerService", "Sound not loaded yet");
                return;
            }

            new Handler().postDelayed(() -> {
                stopNotificationSound(timerId);
                stopTimer(timerId); // Ensure the timer stops after 10 seconds of sound
            }, 10000); // 10000ms = 10s

        } catch (Exception ex) {
            Log.e("MultiTimerService", "Exception occurred: " + ex.getMessage(), ex);
        }
    }

    private void stopTimer(int timerId) {
        CountDownTimer countDownTimer = timers.get(timerId);
        if (countDownTimer != null) {
            countDownTimer.cancel();
            timers.remove(timerId);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(timerId);
            stopSelfIfNoTimers();
        }
    }

//    private void stopSelfIfNoTimers() {
//        if (timers.isEmpty()) {
//            stopSelf();
//        }
//    }

    private void stopSelfIfNoTimers() {
        if (timers.isEmpty()) {
            releaseSoundPool();
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Multi Timer Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setSound(null, null);
            serviceChannel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        for (CountDownTimer timer : timers.values()) {
//            timer.cancel();
//        }
//        timers.clear();
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (CountDownTimer timer : timers.values()) {
            timer.cancel();
        }
        timers.clear();
        releaseSoundPool();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
