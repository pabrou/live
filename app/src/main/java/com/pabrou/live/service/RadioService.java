package com.pabrou.live.service;

import android.app.Notification;
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
import android.util.Log;
import android.widget.Toast;

import com.pabrou.live.NotificationHelper;

import java.io.IOException;

import static com.pabrou.live.NotificationHelper.EXTRA_URL;
import static com.pabrou.live.service.RadioPlaybackState.LOADING;
import static com.pabrou.live.service.RadioPlaybackState.PLAYING;
import static com.pabrou.live.service.RadioPlaybackState.STOPPED;

/**
 * Created by pablo on 12/10/16.
 */

public class RadioService extends Service implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener {

    private final static int NOTIFICATION_ID = 67234;
    private final static String WIFI_LOCK = "radioServiceLock";

    public final static String PLAYBACK_STATE = "playbackState";
    public final static String EXTRA_STATE = "extraPlaybackState";

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

        initPlayer();

        registerReceiver(receiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null){
            switch (intent.getAction()){
                case  NotificationHelper.ACTION_PLAY:
                    String url = intent.getStringExtra(EXTRA_URL);
                    startPlayback(url);
                    break;
                case NotificationHelper.ACTION_PAUSE:
                    pausePlayback();
                    break;
                case NotificationHelper.ACTION_CLOSE:
                    stopPlayback();
                    break;
            }
        }

        return START_STICKY;
    }

    private void initPlayer(){
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    public void startPlayback(String url){
        mRadioUrl = url;

        wifiLock.acquire();

        Notification playingNotification = NotificationHelper.newPlayingNotification(
                getApplicationContext(), mRadioUrl);
        startForeground(NOTIFICATION_ID, playingNotification);

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

    private void pausePlayback() {
        Notification pausedNotification = NotificationHelper.newPausedNotification(
                getApplicationContext(), mRadioUrl);
        startForeground(NOTIFICATION_ID, pausedNotification);

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }

        if (wifiLock.isHeld())
            wifiLock.release();

        mState = STOPPED;
        notifyPlaybackStateChange();
        stopForeground(false);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();

        mState = PLAYING;
        notifyPlaybackStateChange();
    }

    public void stopPlayback(){
        Notification pausedNotification = NotificationHelper.newPausedNotification(
                getApplicationContext(), mRadioUrl);
        startForeground(NOTIFICATION_ID, pausedNotification);

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

        stopSelf();
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
}
