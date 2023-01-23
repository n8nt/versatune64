package com.datvexpress.ws.versatune.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Entity
@Table( name = "tunerConfigRecord")
public class TunerConfigRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "CHAN")
    private int channel;

    @Column(name = "FREQ")
    private int frequency;

    @Column(name = "BW")
    private int bandwidth;

    @Column(name = "FEC")
    private String fec;

    @Column(name="SYMB")
    private int symbolRate;

    @Column(name="IN_DEV")
    private String inputDevice;

    @Column(name="PID_AUD")
    private String pidAudio;

    @Column(name="PID_VID")
    private String pidVideo;

    @Column(name="ENB_CHAN")
    private boolean enableChan;

    // DISeQC group
    @Column(name="LNB_13V")
    private boolean lnb_13v;

    @Column(name="LNB_18V")
    private boolean lnb_18v;

    @Column(name="LNB_22KHZ")
    private boolean khz_22;
}
