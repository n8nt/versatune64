package com.datvexpress.ws.versatune.model;

import com.datvexpress.ws.versatune.enums.IpcFifoStatus;
import com.datvexpress.ws.versatune.enums.TunerStateMachineEvents;
import com.datvexpress.ws.versatune.enums.TunerStateMachineStates;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/*
    This context will be used between all calls so we can
    maintain state. I hope.
*/
@Setter
@Getter
@ToString
@Component
public class VersatuneContext {
    /*
    verlayPath, configPath, fifoPath, blankTsPath, appPath, enabledChannelCount,
     */
    // overlay path for putting text onto the video
    private String overlayPath;
    // configPath - path to the configuration file which is outside the jar file
    private String configPath;
    // fifoPath - same as the InterProcessCommunication FIFO - but here because we use in config
    private String fifoPath;
    // blankTsPath - file that contains the small TS file to be sent to the VLC controller to get it going
    private String blankTsPath;
    // appPath - this is the path to our Versatune APP
    private String appPath;
    // audioDevice - needed for VLC in case there is sound
    private String audioDevice;
    // enabled channel count - number of enabled channels (if any)
    private int enabledChannelCount;
    // frequency for parameter passing
    private String frequency;
    // bandwidth for parameter passing
    private String bandwidth;
    // symbolRate for parameter passing
    private String symbolRate;
    // input device (dvbt, dvbs, usb etc. - get from the channel data from the DB
    private String inputDevice;
    // get some other data from the channel
    private String pidv;
    private String pida;
    private String disec1;
    private String disec2;
    private String fec;

    // display status on HDMI while no VLC is going on
    private Boolean displayWanted = false;



    // path to the InterProcessCommunicationFIFO
    private String ipcFifo;
    // File Descriptor ID for the InterProcessCommunicationFIFO
    private int fd_ipcFifo;
    // File Status for the ipcFifo
    private String ipcFifoStatus = IpcFifoStatus.EMPTY.name();
    // ipcOpenFile error value
    private int ipcErrorValue;
    // keep track of tuner status
    private String tunerStatus;
    // keep track of read timeouts
    private int consecutiveReadTimeouts;
    // keep track of consecutive NO DATA
    private int consecutiveNoData;
    // current frequency
    private String currentFrequency;
    // current bandwidth
    private String currentBandwidth;
    // current symbolRate
    private String currentSymbolRate;
    // first channel  (1-n)
    private int firstChannel = 0;
    // scan pass number
    private long passNumber = 0;
    // Number of channels scanned this pass
    private int channelsScannedThisPass = 0;
    // number of channels this pass
    private int channelsInPass = 0;
    // number channels next pass
    private int channelsNextPass = 0;
    // current channelId in use by tuner
    private long currentChannelIdInUse = 0;
    // curretn channl number in use by tuner
    private long currentChannelNumberInUse = 0;

    // Channel Info
    private String scanChannelFrequency;
    private String lockChannelFrequency;

    // state machine
    private TunerStateMachineStates currentState;
    private TunerStateMachineStates nextState;
    private TunerStateMachineEvents currentEvent;
    private TunerStateMachineEvents lastEvent;

    // Scanner control
    private ScannerControlRecord scr;

    // VLC control
    private VlcTracker vlcTracker;

    private Boolean vlcRunningForTuner;
    private Boolean vlcRunningForSlideShow;
    private int consecutiveResetCount;




    public int bumpConsecutiveNoData(){
        return ++this.consecutiveNoData;
    }

    public void resetConsecutiveNoData(){
        this.consecutiveNoData = 0;
    }

    public int bumpConsecutiveReadTimeouts(){
        return ++this.consecutiveReadTimeouts;
    }

    public void resetConsecutiveReadTimeouts(){
        this.consecutiveReadTimeouts = 0;
    }

    public int bumpConsecutiveResetCount() { return ++this.consecutiveResetCount;}

    public void resetConsecutiveResetCount() {this.consecutiveResetCount = 0;}


    public List<String> fifoLines = new ArrayList<>();

}
