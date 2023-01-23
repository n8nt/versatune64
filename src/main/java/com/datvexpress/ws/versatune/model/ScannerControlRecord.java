package com.datvexpress.ws.versatune.model;

import com.datvexpress.ws.versatune.enums.ScannerControl;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;



/*
        This record will be used to control how the input devices are scanned.

 */

@Setter
@Getter
@Entity
@ToString
@Table( name = "scannerControlRecord")
public class ScannerControlRecord {

    public ScannerControlRecord (){

    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "ACTIVE_CHAN_ID")
    private long channelId = 0L;

    @Column(name = "STATUS")
    private String status = ScannerControl.DISABLED.name();

    @Column(name = "SCAN_CHAN")
    private int scanChannel;

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

    public ScannerControlRecord( TunerConfigRecord tr){
        this.khz_22 = tr.isKhz_22();
        this.lnb_13v = tr.isLnb_13v();
        this.lnb_18v = tr.isLnb_18v();
        this.bandwidth = tr.getBandwidth();
        this.channelId = tr.getId();
        this.scanChannel = tr.getChannel();
        this.fec = tr.getFec();
        this.frequency = tr.getFrequency();
        this.symbolRate = tr.getSymbolRate();
        this.pidAudio = tr.getPidAudio();
        this.pidVideo = tr.getPidVideo();
        this.enableChan = tr.isEnableChan();
        this.inputDevice = tr.getInputDevice();
    }

    public ScannerControlRecord updateWithTunerConfiguration(TunerConfigRecord tr){
        this.frequency = tr.getFrequency();
        this.fec = tr.getFec();
        this.symbolRate = tr.getSymbolRate();
        this.channelId = tr.getId();
        this.scanChannel = tr.getChannel();
        this.bandwidth = tr.getBandwidth();
        this.lnb_13v = tr.isLnb_13v();
        this.lnb_18v = tr.isLnb_18v();
        this.khz_22 = tr.isKhz_22();
        this.pidAudio = tr.getPidAudio();
        this.pidVideo = tr.getPidVideo();
        this.enableChan = tr.isEnableChan();
        this.inputDevice = tr.getInputDevice();
        return this;
    }

}
