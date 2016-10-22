package com.pabrou.live.service;

import android.os.Binder;

/**
 * Created by pablo on 12/10/16.
 */
public class RadioBinder extends Binder implements RadioPlayback {

    private RadioService mRadioService;

    public RadioBinder(RadioService radioService) {
        mRadioService = radioService;
    }

    @Override
    public void play(String url) {
        mRadioService.startPlayback(url);
    }

    public void stop(){
        mRadioService.stopPlayback();
    }

    @Override
    public RadioPlaybackState getState() {
        return mRadioService.getPlaybackState();
    }
}
