package com.datvexpress.ws.versatune.background;

import com.datvexpress.ws.versatune.enums.ScannerControl;
import com.datvexpress.ws.versatune.model.ScannerControlRecord;
import com.datvexpress.ws.versatune.model.TunerConfigRecord;
import com.datvexpress.ws.versatune.repo.ScannerControlRepository;
import com.datvexpress.ws.versatune.repo.TunerSetupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/*
     Starts up the Combituner. This will write the new parameters into the config file.
     Each time the Versatune decides to run a different channel it will have to first
     stop the combituner then start a new one. We could go ahead and just let this one
     do the stop but better to separate concerns
 */
@Component
public class StartCombituner implements Runnable {

    final ScannerControlRepository scannerRepo;
    final TunerSetupRepository tunerRepo;

    public StartCombituner(ScannerControlRepository scannerRepo,
                           TunerSetupRepository tunerRepo) {
        this.scannerRepo = scannerRepo;
        this.tunerRepo = tunerRepo;
    }

    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run() {
        logger.info("Background  for StartSlideShow task running.");
        startTuner();
    }

    private void startTuner() {

        logger.info("Running Start Tuner Stream.");

        List<String> commandList = new ArrayList<>();
        commandList.add("#!/bin/bash \n");
        commandList.add("cd /usr/local/apps/versatune");
        commandList.add("sudo killall CombiTunerExpress >/dev/null 2>/dev/null \n");
        commandList.add("sleep 0.1 \n");
        commandList.add("sudo rm knucker_status_fifo \n");
        commandList.add("mkfifo knucker_status_fifo");

        // get list of enabled channels - we will need these
        List<TunerConfigRecord> enabledChannels = tunerRepo.findAll()
                .stream()
                .filter(p -> p.isEnableChan())
                .collect(Collectors.toList());
        // get active channel - we'll really only have one record in this table. But we need
        // a place to store it globally
        List<ScannerControlRecord> scanList = scannerRepo.findAll();
        ScannerControlRecord scannerControlRecord;
        int nextChannel = 0;
        Optional<ScannerControlRecord> scanRef = scanList
                .stream()
                .filter(p -> p.getStatus().equals(ScannerControl.ENABLED_ACTIVE.name()))
                .findFirst();
        if (scanRef.isEmpty()) {
            // create a new scan Control re4cord.
            scannerControlRecord = new ScannerControlRecord();
            // get first channel from enabled list
            nextChannel = getNextChannelRecord(enabledChannels, 1);
        } else {
            scannerControlRecord = scanRef.get();
            // get tuner config for this channel
            long channelId = scanRef.get().getChannelId();
            // set active channel idle
            scanRef.get().setStatus(ScannerControl.ENABLED_IDLE.name());
            // get list of tuner config records (one for each channel)
            List<TunerConfigRecord> channels = tunerRepo.findAll();
            // find the channel that was active
            Optional<TunerConfigRecord> chanRef = channels.stream()
                    .filter(p -> p.getId() == channelId)
                    .findFirst();
            if (chanRef.isPresent()) {
                // now find it's chan number
                int chanNumber = chanRef.get().getChannel();
                // now look for channel with next highest channel number or back to first channel
                nextChannel = getNextChannelRecord(enabledChannels, chanNumber + 1);
            }
        }
        if (nextChannel > 0){
                // now find the configuration for this channel
            int temp = nextChannel;
            Optional<TunerConfigRecord> targetRef = enabledChannels.stream()
                    .filter( p -> p.getChannel() == temp)
                    .findFirst();
            if (targetRef.isPresent()){
                // set the scanner control record to point to the new channel
                scannerControlRecord.setChannelId(targetRef.get().getId());
                scannerControlRecord.setStatus(ScannerControl.ENABLED_PENDING.name());
                scannerControlRecord.setScanChannel(nextChannel);
                scannerRepo.save(scannerControlRecord);
            }
        }


        // set active channel idle

        // find next channel in the enabled channels list

        // make the new channel active

        commandList.add("su -c '/home/pi/dvbt/dvb-t_start_tuner_udp.sh' pi &");
        try {
            executeCommandList(commandList);
        } catch (Exception e) {
            logger.error("ERROR: could not execute the script to start the Tuner Stream.", e.getMessage());
        }
    }

    private int getNextChannelRecord(List<TunerConfigRecord> enabledChannels, int nextChannelTarget) {
        if (enabledChannels.size() > 0) {
            // find highest numbered channel
            int hiChan = Integer.MIN_VALUE;
            int loChan = Integer.MAX_VALUE;
            for (TunerConfigRecord r : enabledChannels) {
                if (r.getChannel() > hiChan)
                    hiChan = r.getChannel();
                if (r.getChannel() < loChan)
                    loChan = r.getChannel();
            }
            // now figure out where we are
            if (nextChannelTarget > hiChan) {
                nextChannelTarget = loChan;
            }
            return nextChannelTarget;
        } else {
            // no enabled channels so cannot scan.
            return 0;
        }

    }


    private void executeCommandList(List<String> commandList) throws IOException {
        File tempScript = createTempScriptFromCommandList(commandList);
        logger.info("creating and starting the process to execute a commandlist.");

        logger.info("Executing script: " + tempScript);
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", tempScript.toString());
            pb.inheritIO();
            Process process = pb.start();
            logger.info("Process started... " + process.info());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.info("*** Script did NOT run properly.");
                logger.info("   ERRORCODE = exitCode");
            }
        } catch (InterruptedException ie) {
            logger.error("Caught interrupted exception. Not sure what to do. " + ie.getMessage());
        } finally {
            tempScript.delete();
        }

    }

    private File createTempScriptFromCommandList(List<String> commands) throws IOException {
        logger.info("creating the temporary script file from command list.");
        File tempScript = File.createTempFile("scriptX1", null);
        Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
                tempScript));
        PrintWriter printWriter = new PrintWriter(streamWriter);
        for (String s : commands) {
            printWriter.println(s);
        }
        printWriter.close();
        return tempScript;
    }

}
