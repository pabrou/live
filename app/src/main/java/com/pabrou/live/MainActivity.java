package com.pabrou.live;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import com.pabrou.live.service.RadioPlayback;
import com.pabrou.live.service.RadioPlaybackState;
import com.pabrou.live.service.RadioService;

public class MainActivity extends AppCompatActivity {

    private static final String METRO = "http://mp3.metroaudio1.stream.avstreaming.net:7200/metro";
    private static final String BLUE = "http://mp3.metroaudio1.stream.avstreaming.net:7200/bluefmaudio1";

    private RadioPlayback mRadioPlayback;

    private Button playbackButton;
    private RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radioGroup = (RadioGroup) findViewById(R.id.rg_radios);

        playbackButton = (Button) findViewById(R.id.bt_playback);
        playbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mRadioPlayback.getState()){
                    case PLAYING:
                        mRadioPlayback.stop();
                        break;
                    case STOPPED:
                        String url;
                        switch (radioGroup.getCheckedRadioButtonId()){
                            case R.id.rb_blue:
                                url = BLUE;
                                break;
                            case R.id.rb_metro:
                                url = METRO;
                                break;
                            default:
                                url = METRO;
                                break;
                        }
                        mRadioPlayback.play(url);
                        break;
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to RadioService
        Intent intent = new Intent(this, RadioService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(RadioService.PLAYBACK_STATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (mRadioPlayback != null) {
            unbindService(mConnection);
            mRadioPlayback = null;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mRadioPlayback = (RadioPlayback) service;

            // We might already be playing or loading a radio, so we must update the UI
            updatePlaybackControls(mRadioPlayback.getState());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mRadioPlayback = null;
            playbackButton.setEnabled(false);
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            RadioPlaybackState playbackState = (RadioPlaybackState)
                    intent.getSerializableExtra(RadioService.EXTRA_STATE);
            updatePlaybackControls(playbackState);
        }
    };

    private void updatePlaybackControls(RadioPlaybackState playbackState){
        switch (playbackState){
            case PLAYING:
                playbackButton.setEnabled(true);
                playbackButton.setText(R.string.stop);
                break;
            case LOADING:
                playbackButton.setEnabled(false);
                playbackButton.setText(R.string.loading);
                break;
            case STOPPED:
                playbackButton.setEnabled(true);
                playbackButton.setText(R.string.play);
                break;
        }
    }
}
