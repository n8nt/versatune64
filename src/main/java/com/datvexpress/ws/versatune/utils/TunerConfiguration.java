package com.datvexpress.ws.versatune.utils;

import com.datvexpress.ws.versatune.enums.FECoptions;
import com.datvexpress.ws.versatune.enums.InputDevices;
import com.datvexpress.ws.versatune.model.TunerConfigRecord;
import com.datvexpress.ws.versatune.service.ChannelService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class TunerConfiguration {
    Logger myLog = LoggerFactory.getLogger(getClass());
    final private ChannelService channelService;
    String tunerConfigFolder = "/usr/local/apps/btsocket/conf/";
    String tunerConfigFilename = "tunerconfig.json";
    /*
      tunerconfig:
    directory: "/usr/local/apps/versatune/"
    filename: "tunerconfig.json"
     */

    public TunerConfiguration( ChannelService channelService ){
        this.channelService = channelService;
    }

    /*
            Reads current tuner configuration from file in conf area. If none exists, then one will be created
            with 1 channel consisting of what we are using for development.
     */
    public List<TunerConfigRecord> initTunerSetup(){
        ObjectMapper mapper = new ObjectMapper();
        List<TunerConfigRecord> configData = new ArrayList<>();
        String pathName  = tunerConfigFolder + "/" +
                tunerConfigFilename;
        // check if file exists that contains the most recent tuner setup configuration
        try{
            if (Files.notExists(Paths.get(pathName))) {
                // create new file
                if (Files.isDirectory(Paths.get(tunerConfigFolder))) {
                    // create our config file
                    Path configFilePath = Files.createFile(Paths.get(pathName));
                    // add one record for one channel
                    TunerConfigRecord data = new TunerConfigRecord();

                    /*
                    printf("\n");
                    printf("-m mode options dvbs dvbt (defaults to DVB-S)\n");
                    printf("-f frequency in KHz (defaults to 1 GHz)\n");
                    printf("-s DVB-S/S2 symbol rate in Ksymbols/sec (defaults to 2000 KS/s)\n");
                    printf("-b DVB-T/T2 Channel bandwidth in KHz options 8000 7000 6000 5000 2000 1700 1000 500 and below variable (defaults to 2000 KHz)\n");
                    printf("-p UDP port address (defaults to 1314)\n");
                    printf("-i UDP ip address (defaults to 127.0.0.1)\n");
                    printf("-w Time in seconds before a relock is tried (defaults to 60s)\n");
                    printf("-n Named pipe to write output transport stream (defaults to not used)\n");
                    printf("\n");
                    printf("Example setting receiver to receive 500 KHz DVB-T on 437 MHz\n");
                    printf("%s -m dvbt -f 437000 -b 500\n",name);
                    printf("Example setting receiver to receive 333 KS/s DVB-S2 on 1249 GHz\n");
                    printf("%s -m dvbs -f 1249000 -s 333\n",name);
                    printf("\n");
                    */

                    data.setChannel(1);
                    data.setFrequency(423000);
                    data.setBandwidth(2000);
                    data.setPidAudio("hdmi");
                    data.setSymbolRate(2000);
                    data.setInputDevice(InputDevices.FTM4762_DVB_T_T2.name());
                    data.setPidVideo("hdmi");
                    data.setLnb_13v(false);
                    data.setKhz_22(false);
                    data.setLnb_18v(false);
                    data.setEnableChan(true);
                    data.setFec(FECoptions.DVBT_1x2.name());

                    // Convert the object to a json string.
                    String jsonString = mapper.writeValueAsString(data);
                    jsonString = jsonString + System.lineSeparator();
                    Files.writeString(configFilePath, jsonString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                    data.setChannel(2);
                    jsonString = mapper.writeValueAsString(data);
                    jsonString = jsonString + System.lineSeparator();
                    Files.writeString(configFilePath, jsonString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } else {
                    myLog.error("Could not open a file to write the tuner config data to.");
                }
            }
            if (Files.exists(Paths.get(pathName))){

                Reader reader = Files.newBufferedReader(Paths.get(pathName), StandardCharsets.UTF_8);
                try (BufferedReader r = Files.newBufferedReader(Paths.get(pathName), StandardCharsets.UTF_8)){
                    String line;
                    while((line = r.readLine()) != null){
                        // convert line to object and store in database
                        mapper = new ObjectMapper();
                        TunerConfigRecord newData = mapper.readValue(line, TunerConfigRecord.class);

                        try {
                            channelService.save(newData);

                        }catch(Exception repoError){
                            myLog.error("Could not write record to database.", repoError);
                        }
                        configData.add(newData);
                    }
                }
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // if exists
        // then read file into list of channels

        // else create new list with default data
        //

        return configData;
    }
}
