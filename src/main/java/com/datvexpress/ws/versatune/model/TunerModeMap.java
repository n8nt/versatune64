package com.datvexpress.ws.versatune.model;

import org.springframework.stereotype.Component;

import java.util.HashMap;

/*
        looks up specified device type and returns matching mode. If we cannot find
        the value then we return a default value of 'dvbt'
 */
@Component
public class TunerModeMap {
    final HashMap<String, String> modeMap;

    public TunerModeMap(){
        this.modeMap = new HashMap<>();
        modeMap.put("FTM4762_DVB_S_S2", "dvbs");
        modeMap.put("FTM4762_DVB_T_T2", "dvbt");
        modeMap.put("FTS4334_DVB_S_S2A", "dvbs");
        modeMap.put("FTS4334_DVB_S_S2B", "dvbs");
        modeMap.put("FTS3261_DVB_S_S2", "dvbs");
        modeMap.put("FTS3261_DVB_S_S2X", "dvbs");
    }

    public String getModeFromDeviceInput(String deviceInput){
        if(modeMap.containsKey(deviceInput)){
            return modeMap.get(deviceInput);
        }else{
            return "dvbt";
        }
    }


}
