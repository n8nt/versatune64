package com.datvexpress.ws.versatune;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


public class ParsePropertiesTest {

    @Test
    public void getFrequencyFromChannelScan(){
        String fifoLine = "[AVL_ChannelScan_Tx] Freq is 423.000000 MHz";
        int fromIndex = fifoLine.indexOf("Freq is ") + 8;
        int toIndex = fifoLine.lastIndexOf(" MHz");
        String theFreq = fifoLine.substring(fromIndex,toIndex);
    }

    @Test
    public void getFrequencyFromLockChannel(){
        String fifoLine = "[AVL_LockChannel_T] Freq is 417 MHz,";
        int fromIndex = fifoLine.indexOf("Freq is ") + 8;
        int toIndex = fifoLine.lastIndexOf(" MHz");
        String theFreq = fifoLine.substring(fromIndex,toIndex);
    }

}


/*

                if (transitionIO.getTunerEvent().equals(TunerStateMachineEvents.TUNER_SEARCHING_FOR_SIGNAL)) {
                    // save frequency Freq is 417.000000 MHz,
                    int fromIndex = line.indexOf("Freq is ") + 8;
                    int toIndex = line.lastIndexOf(" Mhz");
                    context.setScanChannelFrequency(line.substring(fromIndex,toIndex));

                }
                if (transitionIO.getTunerEvent().equals(TunerStateMachineEvents.TUNER_SIGNAL_DETECTED_ATTEMPTING_LOCK)) {
                    // save Freq is 417 MHz,
                    int fromIndex = line.indexOf("Freq is ") + 8;
                    int toIndex = line.lastIndexOf(" Mhz");
                    context.setLockChannelFrequency(line.substring(fromIndex,toIndex));
                }

 */