package com.datvexpress.ws.versatune.model;

import com.datvexpress.ws.versatune.enums.TunerStateMachineEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Optional;

@Component
public class TunerEventMap {

    Logger logger = LoggerFactory.getLogger(getClass());
    final HashMap<String, TunerStateMachineEvents> hMap;

    public TunerEventMap() {
        this.hMap = new HashMap<>();
        hMap.put("Unable to claim interface", TunerStateMachineEvents.TUNER_UNABLE_TO_CLAIM_INTERFACE);
        hMap.put("[GetChipId] chip id:AVL6862", TunerStateMachineEvents.TUNER_FOUND);
        hMap.put("[GetFamilyId] Family ID:0x4955", TunerStateMachineEvents.TUNER_INTIALIZING);
        hMap.put("[AVL_Init] AVL_Initialize Failed!", TunerStateMachineEvents.TUNER_INITIALIZE_FAILED);
        hMap.put("[AVL_Init] ok", TunerStateMachineEvents.TUNER_INTIAZLIZED);
        hMap.put("[AVL_Init] AVL_Initialize Booted!", TunerStateMachineEvents.TUNER_BOOTED);
        hMap.put("[DVB_Tx_tuner_Lock] Tuner locked!", TunerStateMachineEvents.TUNER_SIGNAL_LOCKED);
        hMap.put("Tuner locked", TunerStateMachineEvents.TUNER_SIGNAL_LOCKED);
        hMap.put("[DVB_Tx_tuner_Lock] Tuner unlock!", TunerStateMachineEvents.TUNER_UNLOCKED);
        hMap.put("Tuner Unlocked", TunerStateMachineEvents.TUNER_UNLOCKED);
        hMap.put("[AVL_ChannelScan_Tx] Lock Tuner :", TunerStateMachineEvents.TUNER_ATTEMPTING_LOCK);
        hMap.put("[AVL_LockChannel_T] Freq is ", TunerStateMachineEvents.TUNER_SIGNAL_LOCK_DETECTED);
        hMap.put("[AVL_LockChannel_T] Failed to lock the channel!", TunerStateMachineEvents.TUNER_LOCK_FAILED);
        hMap.put("[DVBTx_Channel_ScanLock_Example] DVBTx channel scan is fail,Err.", TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH);
        hMap.put("[AVL_LockChannel_T] demod mode is not DVB-Tx,Err.", TunerStateMachineEvents.TUNER_DEMOD_MODE_NOT_DVBTX);
        hMap.put("SDK Version,Major-Minor-Build:2-20-25", TunerStateMachineEvents.TUNER_SDK_VERSION_FOUND);

        hMap.put("TS thread cannot be started ", TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START);
        hMap.put("Transport Stream sync (0x47) not found", TunerStateMachineEvents.TUNER_TRANSPORT_SYNC);
        hMap.put("MOD  :", TunerStateMachineEvents.TUNER_MOD_FOUND);
        hMap.put("FFT  :", TunerStateMachineEvents.TUNER_FFT_FOUND);
        hMap.put("Const:", TunerStateMachineEvents.TUNER_CONST_FOUND);
        hMap.put("FEC  :", TunerStateMachineEvents.TUNER_FEC_FOUND);
        hMap.put("SSI is", TunerStateMachineEvents.TUNER_SSI_FOUND);
        hMap.put("PER is", TunerStateMachineEvents.TUNER_PER_FOUND);
        hMap.put("SQI is", TunerStateMachineEvents.TUNER_SQI_FOUND);
        hMap.put("SNR is", TunerStateMachineEvents.TUNER_SNR_FOUND);
        hMap.put("Guard:", TunerStateMachineEvents.TUNER_GUARD_FOUND);
        hMap.put("locked", TunerStateMachineEvents.TUNER_LOCKED_FOUND);
        hMap.put("Unlocked", TunerStateMachineEvents.TUNER_UNLOCKED);

        hMap.put("===  Freq is", TunerStateMachineEvents.TUNER_NEW_FREQ_DATA);
        hMap.put("===  Bandwidth is", TunerStateMachineEvents.TUNER_NEW_BW_DATA);
        hMap.put("[AVL_LockChannel_T] Freq is", TunerStateMachineEvents.TUNER_SIGNAL_DETECTED_ATTEMPTING_LOCK); // Note! This is only a partial key.
        hMap.put("[AVL_ChannelScan_Tx] Freq is", TunerStateMachineEvents.TUNER_SEARCHING_FOR_SIGNAL); // NOTE! This is only a partial key.
        hMap.put("Patch Version,Major-Minor-Build:", TunerStateMachineEvents.TUNER_PATCH_VERSION_FOUND);
        hMap.put("Failed to set work mode!", TunerStateMachineEvents.TUNER_FAILED_TO_SET_WORK_MODE);


    }

    /*
            Returns TunerEvent based on value of the input which is the message from the
            knucker_status_fifo
    */
    public TunerStateMachineEvents fetchTunerEventFromInputData(String inputData){
        logger.info("----- ENTERING fetchTunerEventFromInputData for "+inputData+" .");

        Optional<String> matchingEventRef = hMap.keySet().stream().filter(p -> inputData.contains(p)).findFirst();
        if (matchingEventRef.isPresent()){
            return hMap.get(matchingEventRef.get());
        }
        logger.info("Could not match input data [" + inputData + "] to known tuner event.");
        return TunerStateMachineEvents.UNKNOWN_EVENT;
    }
}
