package com.pabrou.live.service;

/**
 * Created by pablo on 13/10/16.
 */

public interface RadioPlayback {

    void play(String url);

    void stop();

    RadioPlaybackState getState();
}
