package com.datvexpress.ws.versatune.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class TunerScanStatus {
    private int currentZeroCount;
    private int maxZeroCount;
    private int currentPollCount;
    private int maxPollCount;
    private int currentSearchFailedCount;
    private int maxSearchFailedCount;
    private int unlockedCount;
    private int maxUnlockedCount;
}
