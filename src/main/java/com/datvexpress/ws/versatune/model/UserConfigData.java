package com.datvexpress.ws.versatune.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserConfigData {

    private String frequency="0";
    private String bandwidth="0";
    private String symbolRate="0";
    private String audioOut="hdmi";
    private String channel="0";
}
