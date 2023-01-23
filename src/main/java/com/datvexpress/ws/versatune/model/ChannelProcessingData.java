package com.datvexpress.ws.versatune.model;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class ChannelProcessingData {
    long channelId;
    boolean scanComplete = false;

    public ChannelProcessingData(long channelId){
        this.channelId = channelId;
    }
}

