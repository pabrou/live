package com.pabrou.live;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import com.pabrou.live.service.RadioService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String METRO = "http://mp3.metroaudio1.stream.avstreaming.net:7200/metro";
    private static final String BLUE = "http://mp3.metroaudio1.stream.avstreaming.net:7200/bluefmaudio1";

    private MediaBrowserCompat mMediaBrowser;

    private Button playbackButton;
    private RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, RadioService.class),
                mConnectionCallback,
                null);

        radioGroup = (RadioGroup) findViewById(R.id.rg_radios);

        playbackButton = (Button) findViewById(R.id.bt_playback);
        playbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMedia();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMediaBrowser.disconnect();
    }

    private void playMedia() {
        MediaControllerCompat controller = getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia() {
        MediaControllerCompat controller = getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }

    private void stopMedia() {
        MediaControllerCompat controller = getSupportMediaController();
        if (controller != null) {
            controller.getTransportControls().stop();
        }
    }

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnected");
                    try {
                        // Ah, here’s our Token again
                        MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();

                        // This is what gives us access to everything
                        MediaControllerCompat controller =
                                new MediaControllerCompat(MainActivity.this, token);

                        // Convenience method of FragmentActivity to allow you to use
                        // getSupportMediaController() anywhere
                        setSupportMediaController(controller);

                        controller.registerCallback(mMediaControllerCallback);
                    } catch (RemoteException e) {
                        Log.e(MainActivity.class.getSimpleName(),
                                "Error creating controller", e);
                    }
                }
            };

    MediaControllerCompat.Callback mMediaControllerCallback = new MediaControllerCompat.Callback(){
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            Log.d(TAG, "onPlaybackStateChanged:" + state.toString());

            switch (state.getState()){
                case PlaybackStateCompat.STATE_PLAYING:
                    playbackButton.setEnabled(true);
                    playbackButton.setText(R.string.pause);
                    playbackButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pauseMedia();
                        }
                    });
                    break;
                case PlaybackStateCompat.STATE_BUFFERING:
                    playbackButton.setEnabled(true);
                    playbackButton.setText(R.string.loading);
                    playbackButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            stopMedia();
                        }
                    });
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    playbackButton.setEnabled(true);
                    playbackButton.setText(R.string.play);
                    playbackButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            playMedia();
                        }
                    });
                    break;
                default:
                    playbackButton.setEnabled(false);
                    playbackButton.setText(R.string.stop);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            Log.d(TAG, "onMetadataChanged");
        }
    };
}
