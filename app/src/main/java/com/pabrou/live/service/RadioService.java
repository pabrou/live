package com.pabrou.live.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.pabrou.live.NotificationHelper;
import com.pabrou.live.R;

import java.io.IOException;
import java.util.List;

/**
 * Created by pablo on 12/10/16.
 */

public class RadioService extends MediaBrowserServiceCompat implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener {

    private final static String TAG = "RadioService";

    private static final String BLUE = "http://mp3.metroaudio1.stream.avstreaming.net:7200/bluefmaudio1";

    private final static int NOTIFICATION_ID = 67234;
//    private final static String WIFI_LOCK = "radioServiceLock";


    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mSession;
//    private WifiManager.WifiLock wifiLock;

//    private IBinder mBinder;

//    private RadioPlaybackState mState = STOPPED;
    private String mRadioUrl;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

//        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
//                .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK);

        mSession = new MediaSessionCompat(this, RadioService.class.getSimpleName());
        setSessionToken(mSession.getSessionToken());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                Log.d(TAG, "onPlay");
                startPlayback(BLUE);

                PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_STOP);
                stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1);

                mSession.setPlaybackState(stateBuilder.build());
            }

            @Override
            public void onPrepare() {
                super.onPrepare();
                Log.d(TAG, "onPrepare");
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.d(TAG, "onPause");
                pausePlayback();

                PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_STOP);
                stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);

                mSession.setPlaybackState(stateBuilder.build());
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "onStop");
                stopPlayback();

                PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                        .setActions(PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_STOP);
                stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);

                mSession.setPlaybackState(stateBuilder.build());

            }

            @Override
            public void onPlayFromUri(Uri uri, Bundle extras) {
                super.onPlayFromUri(uri, extras);
                Log.d(TAG, "onPlayFromUri");
            }
        });

        mSession.setSessionActivity(NotificationHelper.
                newMainActivityPendingIntent(getApplicationContext()));

        initPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        if (intent != null){
            MediaButtonReceiver.handleIntent(mSession, intent);
        }

//        if (intent != null && intent.getAction() != null){
//            switch (intent.getAction()){
//                case  NotificationHelper.ACTION_PLAY:
//                    String url = intent.getStringExtra(EXTRA_URL);
//                    startPlayback(url);
//                    break;
//                case NotificationHelper.ACTION_PAUSE:
//                    pausePlayback();
//                    break;
//                case NotificationHelper.ACTION_CLOSE:
//                    stopPlayback();
//                    break;
//            }
//        }

        return START_STICKY;
    }

    private void initPlayer(){
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    private void startPlayback(String url){
        startService(new Intent(getApplicationContext(), RadioService.class));

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        mRadioUrl = url;

//        wifiLock.acquire();
        registerReceiver(noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        if (mMediaPlayer.isPlaying())
            return;

        // Reset to the uninitialized state
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(mRadioUrl);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();
    }

    private void pausePlayback() {

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }

//        unregisterReceiver(noisyReceiver);

//        if (wifiLock.isHeld())
//            wifiLock.release();
    }

    private void stopPlayback(){
        if (mMediaPlayer.isPlaying())
            mMediaPlayer.stop();

        mSession.setActive(false);

//        if (wifiLock.isHeld())
//            wifiLock.release();

        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop playback in case it's still playing
        stopPlayback();

        // Release the player
        mMediaPlayer.release();
        mMediaPlayer = null;

        mSession.release();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(getString(R.string.app_name), null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(this, "Error starting playback", Toast.LENGTH_SHORT).show();
        Log.d("onError", "what:"+what);
        return false;
    }

    BroadcastReceiver noisyReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                Log.d("BroadcastReceiver", "ACTION_AUDIO_BECOMING_NOISY");
                RadioService.this.stopPlayback();
            }
        }
    };
}
