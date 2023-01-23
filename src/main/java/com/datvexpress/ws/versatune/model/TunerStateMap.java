package com.datvexpress.ws.versatune.model;

import com.datvexpress.ws.versatune.enums.TunerStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class TunerStateMap {
    final HashMap<String, Integer> hMap;

    public TunerStateMap(){
        this.hMap = new HashMap<>();
        hMap.put("[GetChipId] chip id:AVL6862",TunerStatus.TUNER_FOUND.ordinal());
        hMap.put("[GetFamilyId] Family ID:0x4955", TunerStatus.INITIALIZING_TUNER.ordinal());

        hMap.put("[AVL_Init] AVL_Initialize Failed!",TunerStatus.INITIALIZE_FAILED.ordinal());
        hMap.put("[AVL_Init] ok",TunerStatus.TUNER_INITIALIZED.ordinal());
        hMap.put("[AVL_Init] AVL_Initialize Booted!",TunerStatus.TUNER_BOOTED.ordinal());
        hMap.put("[DVB_Tx_tuner_Lock] Tuner locked!",TunerStatus.SIGNAL_LOCKED.ordinal());
        hMap.put("Tuner locked",TunerStatus.SIGNAL_LOCKED.ordinal());
        hMap.put("[DVB_Tx_tuner_Lock] Tuner unlock!",TunerStatus.TUNER_UNLOCKED.ordinal());
        hMap.put("Tuner Unlocked",TunerStatus.TUNER_UNLOCKED.ordinal());
        hMap.put("[AVL_LockChannel_T] Freq is",TunerStatus.SIGNAL_DETECTED_ATTEMPTING_LOCK.ordinal()); // Note! This is only a partial key.
        hMap.put("[AVL_ChannelScan_Tx] Lock Tuner:",TunerStatus.TUNER_ATTEMPTING_LOGK.ordinal());
        hMap.put("[AVL_ChannelScan_Tx] Freq is",TunerStatus.SEARCHING_FOR_SIGNAL.ordinal()); // NOTE! This is only a partial key.
        hMap.put("[AVL_LockChannel_T] Failed to lock the channel!",TunerStatus.TUNER_LOCK_FAILED.ordinal());
        hMap.put("[DVBTx_Channel_ScanLock_Example] DVBTx channel scan is fail,Err.",TunerStatus.SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH.ordinal());
        hMap.put("SDK Version,Major-Minor-Build:2-20-25",TunerStatus.TUNER_SDK_VERSION_FOUND.ordinal());
        hMap.put("Patch Version,Major-Minor-Build:2-0-27709",TunerStatus.TUNER_PATCH_VERSION_FOUND.ordinal());


        // these are keys that are contained in the response but not the complete response.
        hMap.put("MOD  :", TunerStatus.MOD_FOUND.ordinal());
        hMap.put("FFT  :",TunerStatus.MOD_FOUND.ordinal());
        hMap.put("Const:",TunerStatus.MOD_FOUND.ordinal());
        hMap.put("FEC  :",TunerStatus.FEC_FOUND.ordinal());
        hMap.put("SSI is",TunerStatus.SSI_FOUND.ordinal());
        hMap.put("SQI is",TunerStatus.SQI_FOUND.ordinal());
        hMap.put("SNR is",TunerStatus.SNR_FOUND.ordinal());
        hMap.put("=== Freq", TunerStatus.NEW_FREQ_DATA.ordinal());
        hMap.put("=== Bandwidth", TunerStatus.NEW_BW_DATA.ordinal());
    }
}
