package com.datvexpress.ws.versatune.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TunerSetupInputGroup {

    private boolean ftm4762_dvb_s_s2;
    private boolean ftm4762_dvb_t_t2;
    private boolean fts4334_dvb_s_s2a;
    private boolean fts4334_dvb_s_s2b;
    private boolean fts3261_dvb_s_s2;
    private boolean fts3261_dvb_s_s2x;
    private boolean usb;
}
