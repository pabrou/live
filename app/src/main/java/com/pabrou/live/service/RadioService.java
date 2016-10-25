package com.pabrou.live.service;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.pabrou.live.NotificationHelper;
import com.pabrou.live.R;

import java.util.List;

/**
 * Created by pablo on 12/10/16.
 */

public class RadioService extends MediaBrowserServiceCompat implements PlaybackManager.PlaybackListener {

    private final static String TAG = "RadioService";

    private static final String BLUE = "http://mp3.metroaudio1.stream.avstreaming.net:7200/bluefmaudio1";

    private final static int NOTIFICATION_ID = 67234;

    private PlaybackManager mPlaybackManager;
    private MediaSessionCompat mSession;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

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
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.d(TAG, "onPause");
                pausePlayback();
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "onStop");
                stopPlayback();
            }
        });

        mSession.setSessionActivity(NotificationHelper.
                newMainActivityPendingIntent(getApplicationContext()));

        mPlaybackManager = new PlaybackManager(getApplicationContext());
        mPlaybackManager.setPlaybackListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        if (intent != null){
            MediaButtonReceiver.handleIntent(mSession, intent);
        }

        return super.onStartCommand(intent, flags, startId);
    }


    private void startPlayback(String url){
        startService(new Intent(getApplicationContext(), RadioService.class));

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        mPlaybackManager.play(url);
    }

    private void pausePlayback() {
        mPlaybackManager.pause();
    }

    private void stopPlayback(){
        mPlaybackManager.stop();
        mSession.setActive(false);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mPlaybackManager.stop();
        mPlaybackManager.release();
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
    public void onPlaybackStateChange(int playbackState) {
        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {

            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_STOP);
            stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1);

            mSession.setPlaybackState(stateBuilder.build());
        } else if (playbackState == PlaybackStateCompat.STATE_PAUSED){
            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY |
                            PlaybackStateCompat.ACTION_STOP);
            stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);

            mSession.setPlaybackState(stateBuilder.build());
        } else if (playbackState == PlaybackStateCompat.STATE_STOPPED){
            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_STOP);
            stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);

            mSession.setPlaybackState(stateBuilder.build());
        }
    }
}
