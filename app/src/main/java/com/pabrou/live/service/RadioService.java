package com.pabrou.live.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;

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

        return START_NOT_STICKY;
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

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();

        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
            stateBuilder.setActions(PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP);
            stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1);

        } else if (playbackState == PlaybackStateCompat.STATE_PAUSED){
            stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_STOP);
            stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
            //stopForeground(false);
        } else if (playbackState == PlaybackStateCompat.STATE_STOPPED){
            stateBuilder.setActions(PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP);
            stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
            //stopForeground(true);
        }

        Bitmap art = BitmapFactory.decodeResource(getResources(), R.drawable.metro_art);

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        metadataBuilder.putText(MediaMetadataCompat.METADATA_KEY_TITLE, "Radio");
        metadataBuilder.putText(MediaMetadataCompat.METADATA_KEY_ALBUM, "Album");
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art);
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art);

        mSession.setMetadata(metadataBuilder.build());
        mSession.setPlaybackState(stateBuilder.build());

        MediaControllerCompat controller = mSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat mediaDescription = mediaMetadata.getDescription();

        android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(mediaDescription.getTitle())
                .setContentText(mediaDescription.getSubtitle())
                .setSubText(mediaDescription.getDescription())
                .setLargeIcon(mediaDescription.getIconBitmap())
                .setSmallIcon(R.drawable.ic_trophy)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentIntent(controller.getSessionActivity())
                .setDeleteIntent(getActionIntent(this, KeyEvent.KEYCODE_MEDIA_STOP))
                .addAction(new NotificationCompat.Action(
                        android.R.drawable.ic_media_pause, getString(R.string.pause),
                        getActionIntent(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)))
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setMediaSession(mSession.getSessionToken())
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(getActionIntent(this, KeyEvent.KEYCODE_MEDIA_STOP)));

        startForeground(NOTIFICATION_ID, builder.build());
    }

    public PendingIntent getActionIntent(Context context, int mediaKeyEvent){
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setPackage(context.getPackageName());
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyEvent));
        return PendingIntent.getBroadcast(context, mediaKeyEvent, intent, 0);
    }
}
