package com.datvexpress.ws.versatune.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;

/*
        Tracks (or attempts to track) when VLC is running.
        Because I don't want to start it up if it is already running.
 */
@Setter
@Getter
@ToString
@Component
public class VlcTracker {
    private long startTime;
    private long stopTime;
    private boolean isRunning = false;
    private boolean slideShowRunning = false;
    private boolean tunerRunning = false;
    private long tunerStartTime;
    private long slideShowStartTime;
    private long tunerStopTime;
    private long slideShowStopTime;
    private int vlcStartStopError;
}
