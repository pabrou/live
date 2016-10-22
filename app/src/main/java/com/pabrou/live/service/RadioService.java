package com.pabrou.live.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.pabrou.live.MainActivity;

import java.io.IOException;

import static android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END;
import static android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START;
import static com.pabrou.live.service.RadioPlaybackState.LOADING;
import static com.pabrou.live.service.RadioPlaybackState.PLAYING;
import static com.pabrou.live.service.RadioPlaybackState.STOPPED;

/**
 * Created by pablo on 12/10/16.
 */

public class RadioService extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnInfoListener {

    public final static int NOTIFICATION_ID = 67234;

    public final static String WIFI_LOCK = "radioServiceLock";
    public final static String PLAYBACK_STATE = "playbackState";
    public final static String EXTRA_STATE = "extraPlaybackState";
    public final static String ACTION_TOGGLE_PLAYBACK = "com.pabrou.live.TOGGLE_PLAYBACK";
    public final static String ACTION_STOP_PLAYBACK = "com.pabrou.live.STOP_PLAYBACK";

    private NotificationManager mNotificationManager;
    private MediaPlayer mMediaPlayer;
    private WifiManager.WifiLock wifiLock;

    private IBinder mBinder;

    private RadioPlaybackState mState = STOPPED;
    private String mRadioUrl;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK);

        mBinder = new RadioBinder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        initPlayer();

        registerReceiver(receiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE_PLAYBACK.equals(intent.getAction())){
            togglePlayback();
        }
        return START_STICKY;
    }

    private void togglePlayback() {
        if (mState == PLAYING){
            stopPlayback();
        }else if (mRadioUrl != null){
            startPlayback(mRadioUrl);
        }
    }

    private void initPlayer(){
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    public void startPlayback(String url){
        mRadioUrl = url;

        wifiLock.acquire();
        showPlayingNotification();
        //showForegroundNotification();

        if (mMediaPlayer.isPlaying())
            mMediaPlayer.stop();

        // Reset to the uninitialized state
        mMediaPlayer.reset();

        try {
            mMediaPlayer.setDataSource(mRadioUrl);
            mMediaPlayer.prepareAsync();

            mState = LOADING;
            notifyPlaybackStateChange();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing player", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();

        mState = PLAYING;
        notifyPlaybackStateChange();
    }

    public void stopPlayback(){
        showPausedNotification();

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }else if (mState == LOADING) {
            mMediaPlayer.reset();
        }

        if (wifiLock.isHeld())
            wifiLock.release();

        mState = STOPPED;
        notifyPlaybackStateChange();
        stopForeground(false);
    }

    public RadioPlaybackState getPlaybackState(){
        return mState;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop playback in case it's still playing
        stopPlayback();

        unregisterReceiver(receiver);

        // Release the player
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    public void showForegroundNotification(){

        Intent togglePlaybackIntent = new Intent(getApplicationContext(), RadioService.class);
        togglePlaybackIntent.setAction(ACTION_TOGGLE_PLAYBACK);

        PendingIntent togglePlaybackPendingIntent = PendingIntent.getService(getApplicationContext(), 1,
                togglePlaybackIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopPlaybackIntent = new Intent(getApplicationContext(), RadioService.class);
        stopPlaybackIntent.setAction(ACTION_STOP_PLAYBACK);

        PendingIntent stopPlaybackPendingIntent = PendingIntent.getService(getApplicationContext(), 2,
                stopPlaybackIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent mainActivityPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .addAction(android.R.drawable.ic_media_play, "Play", togglePlaybackPendingIntent)
                        .addAction(android.R.drawable.ic_media_pause, "Stop", stopPlaybackPendingIntent)
                        .setStyle(new NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0)
                                .setShowActionsInCompactView(1)
                                .setShowCancelButton(true))
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContentTitle("My notification")
                        .setContentIntent(mainActivityPendingIntent)
                        .setContentText(mRadioUrl).build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private android.support.v4.app.NotificationCompat.Builder getNotificationBuilder(){

        PendingIntent mainActivityPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("My notification")
                .setContentIntent(mainActivityPendingIntent)
                .setContentText(mRadioUrl);
    }

    private void showPlayingNotification(){
        Intent togglePlaybackIntent = new Intent(getApplicationContext(), RadioService.class);
        togglePlaybackIntent.setAction(ACTION_TOGGLE_PLAYBACK);

        PendingIntent togglePlaybackPendingIntent = PendingIntent.getService(getApplicationContext(), 1,
                togglePlaybackIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = getNotificationBuilder()
                .addAction(android.R.drawable.ic_media_pause, "Stop", togglePlaybackPendingIntent)
                .setStyle(new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setShowCancelButton(true)).build();

        startForeground(NOTIFICATION_ID, notification);
        //mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void showPausedNotification(){
        Intent togglePlaybackIntent = new Intent(getApplicationContext(), RadioService.class);
        togglePlaybackIntent.setAction(ACTION_TOGGLE_PLAYBACK);

        PendingIntent togglePlaybackPendingIntent = PendingIntent.getService(getApplicationContext(), 1,
                togglePlaybackIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = getNotificationBuilder()
                .addAction(android.R.drawable.ic_media_play, "Play", togglePlaybackPendingIntent)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setShowCancelButton(true)).build();

        startForeground(NOTIFICATION_ID, notification);
        //mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(this, "Error starting playback", Toast.LENGTH_SHORT).show();
        Log.d("onError", "what:"+what);
        return false;
    }

    private void notifyPlaybackStateChange(){
        Intent intent = new Intent(PLAYBACK_STATE);
        intent.putExtra(EXTRA_STATE, mState);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    BroadcastReceiver receiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                Log.d("BroadcastReceiver", "ACTION_AUDIO_BECOMING_NOISY");
                RadioService.this.stopPlayback();
            }
        }
    };

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.d("onBufferingUpdate", "percent:"+percent);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d("onInfo", "what:"+what);
        if (what == MEDIA_INFO_BUFFERING_START){
            Log.d("onInfo", "buffering start");
        }else if (what == MEDIA_INFO_BUFFERING_END){
            Log.d("onInfo", "buffering stop");
        }else if(what == 703){ // MEDIA_INFO_NETWORK_BANDWIDTH = 703
            Log.d("onInfo", "bandwidth");
        }
        return true;
    }
}
