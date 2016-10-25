package com.pabrou.live.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.media.session.PlaybackStateCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;

/**
 * Created by pablo on 24/10/16.
 */

public class PlaybackManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {

    private final static String WIFI_LOCK = "playbackManagerLock";

    private WeakReference<Context> mContext;
    private WifiManager.WifiLock mWifiLock;
    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;

    private String mCurrentSource;
    private int mState;
    private boolean mAudioHasFocus = false;
    private PlaybackListener mPlaybackListener;
    private boolean mAudioNoisyReceiverRegistered = false;

    private BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                PlaybackManager.this.pause();
            }
        }
    };

    public PlaybackManager(Context context){

        mContext = new WeakReference<>(context);

        mWifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK);

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setWakeMode(mContext.get(), PowerManager.PARTIAL_WAKE_LOCK);

        setState(PlaybackStateCompat.STATE_NONE);
    }

    public void setPlaybackListener(PlaybackListener listener){
        mPlaybackListener = listener;
    }

    public void play(String source){
        if (mMediaPlayer.isPlaying())
            stop();

        mCurrentSource = source;

        // If we couldn;t get the audio focus, then don't play
        if (!tryToGetAudioFocus())
            return;

        // Reset to the uninitialized state
        mMediaPlayer.reset();
        try {
            setState(PlaybackStateCompat.STATE_BUFFERING);

            mMediaPlayer.setDataSource(mCurrentSource);
            mMediaPlayer.prepareAsync();

            registerAudioNoisyReceiver();
            mWifiLock.acquire();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();
        setState(PlaybackStateCompat.STATE_PLAYING);
    }

    public void pause(){
        if (mMediaPlayer.isPlaying())
            mMediaPlayer.pause();

        setState(PlaybackStateCompat.STATE_PAUSED);

        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();

        if (mWifiLock.isHeld())
            mWifiLock.release();
    }

    public void stop(){
        if (mMediaPlayer.isPlaying())
            mMediaPlayer.stop();

        mCurrentSource = null;
        setState(PlaybackStateCompat.STATE_STOPPED);

        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();

        if (mWifiLock.isHeld())
            mWifiLock.release();
    }

    public void resume(){
        if (mCurrentSource != null)
            play(mCurrentSource);
    }

    private void setState(int state){
        mState = state;

        // Notify of the change of state
        if (mPlaybackListener != null)
            mPlaybackListener.onPlaybackStateChange(state);
    }

    public int getState(){
        return mState;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    private void registerAudioNoisyReceiver() {
        if (mContext.get() != null && !mAudioNoisyReceiverRegistered) {
            mContext.get().registerReceiver(mAudioNoisyReceiver,
                    new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mContext.get() != null && mAudioNoisyReceiverRegistered) {
            mContext.get().unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    /**
     * Try to get the system audio focus.
     */
    private boolean tryToGetAudioFocus() {
        if (!mAudioHasFocus) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioHasFocus = true;
            }
        }
        return mAudioHasFocus;
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        if (mAudioHasFocus) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioHasFocus = false;
            }
        }
    }

    public void release(){
        stop();

        mMediaPlayer.release();
        mMediaPlayer = null;
        mContext.clear();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
            // Pause playback
            pause();
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume playback
            resume();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            // Stop playback
            stop();
        }
    }

    interface PlaybackListener {
        void onPlaybackStateChange(int playbackState);
    }
}
