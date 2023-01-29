package com.datvexpress.ws.versatune.background;

import com.datvexpress.ws.versatune.config.VersatuneStartupConfig;
import com.datvexpress.ws.versatune.enums.ScannerControl;
import com.datvexpress.ws.versatune.enums.TunerStateMachineEvents;
import com.datvexpress.ws.versatune.enums.TunerStateMachineStates;
import com.datvexpress.ws.versatune.model.*;
import com.datvexpress.ws.versatune.pi4j.Pi4jMinimalBT;
import com.datvexpress.ws.versatune.screenutils.DisplayMessage;
import com.datvexpress.ws.versatune.service.ChannelService;
import com.datvexpress.ws.versatune.service.LinuxIoService;
import com.datvexpress.ws.versatune.service.ScannerService;
import com.datvexpress.ws.versatune.service.SignalService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class DvbtWorker implements Runnable {
    Logger logger = LoggerFactory.getLogger(getClass());

    private final ScannerService scannerService;
    private final SignalService signalService;
    private final ChannelService channelService;
    private final DisplayMessage displayMessage;

    private final TaskExecutor executor;

    private final VersatuneStartupConfig config;

    private final VlcTracker vlcTracker;

    private final VersatuneContext context;

    private final LinuxIoService linuxIoService;

    private final SignalMap signalMap;

    private final TunerEventMap eventMap;

    private final TunerStateMap stateMap;

    private final StateTruthTable stateTable;

    private final Pi4jMinimalBT gpioService;

    public DvbtWorker(
            DisplayMessage displayMessage,
            ChannelService chanService,
            SignalService signalService,
            ScannerService scannerService,
            VersatuneStartupConfig config,
            VlcTracker vlcTracker,
            LinuxIoService linuxIoService,
            VersatuneContext context,
            SignalMap signalMap,
            TunerEventMap eventMap,
            TunerStateMap stateMap,
            StateTruthTable truthTable,
            Pi4jMinimalBT gpioService,
            @Qualifier("threadPoolExecutor") TaskExecutor executor
    ) {
        this.displayMessage = displayMessage;
        this.executor = executor;
        this.channelService = chanService;
        this.signalService = signalService;
        this.scannerService = scannerService;
        this.config = config;
        this.vlcTracker = vlcTracker;
        this.linuxIoService = linuxIoService;
        this.context = context;
        this.signalMap = signalMap;
        this.eventMap = eventMap;
        this.stateMap = stateMap;
        this.stateTable = truthTable;
        this.gpioService = gpioService;
    }

    List<String> allowedExtensions = Arrays.asList("jpeg", "mp4", "png", "jpg", "gif");

    /*
            Get the Last Known Active Tuner Record, if any.
    */
    private Optional<TunerConfigRecord> getLastKnownActiveTunerRecord() {
        Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
        long dbId = scannerService.getActiveChannelDbId();
        TunerConfigRecord tcr = channelService.get(dbId);
        return Optional.of(tcr);

    }


    /*
            This tries to start the slide show using an executor service.
     */
    private Integer doStartSlideShow(String audioDevice, String appPath, VersatuneContext context) {

        logger.info("==> ENTER doStartSlideShow.");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Supplier<Integer> runSlideShow = () -> {
            Integer result = -1;
            try {
                result = executeStartupForSlideShow(appPath, context);
            } catch (Exception e) {
                logger.error("could not start slide show.", e);
            }
            return result;

        };

        CompletableFuture<Integer> cf1 = CompletableFuture.supplyAsync(runSlideShow, executor);
        Integer result = null;
        try {
            result = cf1.get(5000L, TimeUnit.MILLISECONDS);
            context.getVlcTracker().setSlideShowStartTime(System.currentTimeMillis());
            context.getVlcTracker().setSlideShowRunning(true);
        } catch (InterruptedException e) {
            //throw new RuntimeException(e);
            result = -2;
            context.getVlcTracker().setSlideShowRunning(false);
            logger.error("ERROR: Runtime Exception. ", e);
        } catch (ExecutionException e) {
            //throw new RuntimeException(e);
            logger.error("ERROR: Execution Error. ", e);
            context.getVlcTracker().setSlideShowRunning(false);
            result = -3;
        } catch (TimeoutException e) {
            result = -4;
            //throw new RuntimeException(e);
            context.getVlcTracker().setSlideShowRunning(false);
            logger.info("ERROR: Timeout when trying to start up slideshow. ");
        }
        executor.shutdown();
        logger.info("<== EXITING doStartSlideShow with result: " + result);
        return result;

    }

    private void updateUserConfigData() {
        String newFreq = "50000";
        String newBw = "2000";
        String newSymb = "2000";
        String newAudio = "hdmi";
        String newChan = "1";
        Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();

        ScannerControlRecord sr = srRef.get();
        // if we are here then ScannerControlRecord is valid.
        newFreq = String.valueOf(sr.getFrequency());
        newBw = String.valueOf(sr.getBandwidth());
        newSymb = String.valueOf(sr.getSymbolRate());
        newChan = String.valueOf(sr.getScanChannel());

        // update the parameters config file - let's make it a JSON file
        // or, we can pass in the parameters we want to use...
        String pathName = "/usr/local/apps/versatune/data/dvb-t_config.txt";
        if (Files.exists(Paths.get(pathName))) {

            String sb = "freq=" + newFreq + System.lineSeparator() +
                    "bw=" + newBw + System.lineSeparator() +
                    "symb=" + newSymb + System.lineSeparator() +
                    "audio=" + newAudio + System.lineSeparator() +
                    "chan=" + newChan + System.lineSeparator();

            // write out new content
            modifyFile(pathName, sb);
        }
    }


    @Override
    public void run() {

        int enabledChannelCount = -1;
        String overlayPath = config.getVersatuneOverlayPath();
        String configPath = config.getVersatuneDvbConfigPath();
        String fifoPath = config.getVersatuneKnuckerFifo();
        String blankTsPath = config.getBlankTsPath();
        String appPath = config.getAppPath();

        Optional<ScannerControlRecord> scrRef = scannerService.getOrCreateScannerControlRecord();
        if (scrRef.isEmpty()) {
            throw new RuntimeException("Must have Scanner Control Record but none exists.");
        }
        ScannerControlRecord scr = scrRef.get();
        logger.info("Starting up with scanner in state: " + scr.getStatus());

        // NTM: what to do if nothing is in the scanner control yet?

        // set up context with channel info for whatever the last channel that was running.
        context.setCurrentBandwidth(String.valueOf(scr.getBandwidth()));
        context.setCurrentFrequency(String.valueOf(scr.getFrequency()));
        context.setCurrentSymbolRate(String.valueOf(scr.getSymbolRate()));
        context.setCurrentChannelIdInUse(scr.getChannelId());
        context.setCurrentChannelNumberInUse(scr.getScanChannel());
        context.setChannelsInPass(channelService.getEnabledChannelCount());
        context.setAppPath(appPath);
        context.setFifoPath(fifoPath);
        context.setBlankTsPath(blankTsPath);
        context.setConfigPath(configPath);
        context.setOverlayPath(overlayPath);
        context.setVlcTracker(vlcTracker);

        // we will change state to RESET_IDLE because we don't know where we are at startup.
        scr.setStatus(ScannerControl.RESET_IDLE.name());
        // get next ready channel...
        long testChannelId = scr.getChannelId();

        long newNextReadyId = channelService.getNextReadyId(testChannelId);
        if (newNextReadyId == 0) {
            newNextReadyId = channelService.getFirstReadyChannelsId();
        }
        context.setCurrentChannelIdInUse(newNextReadyId);
        logger.info("Changing start up state to: " + scr.getStatus());
        scannerService.save(scr);

        shutdownVlcIfRunning(appPath, context);
        shutdownCombiTunerExpressIfRunning(appPath);

        // now we are ready to go

        logger.info("InitIal Value of VersatuneContext is: " + context);

        int totalEnabledChannels = 0;

        context.resetConsecutiveResetCount();

        try {
            while (true) {

                // get the parameters from the configuration file that we need.
                UserConfigData userConfigData = getUserConfigData(configPath);
                String audioDevice = getAudioDeviceForScan(getUserConfigData(configPath));
                context.setAudioDevice(audioDevice);
                // this should ALWAYS return a record
                Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
                if (srRef.isEmpty()) {
                    throw new RuntimeException("Could not create a scanner control record. Exiting.");
                }
                // we need the Scanner control record.
                scr = srRef.get();
                context.setScr(scr);

                // now, we need the number of all enabled channels.
                enabledChannelCount = channelService.getEnabledChannelCount();
                context.setEnabledChannelCount(enabledChannelCount);

                String status = scr.getStatus();
                if ((status.equals(ScannerControl.SLIDESHOW_RUNNING.name()) && enabledChannelCount == 0) ||
                        context.getConsecutiveResetCount() > 1 ||
                        context.getConsecutiveResetIdleCount() > 2) {
                    // then start up slide show unless it is already running
                    if (!context.getVlcRunningForSlideShow()) {
                        context.setDisplayWanted(false);

                        // get default tuner parameters from user config
                        context.setFrequency(userConfigData.getFrequency());
                        context.setBandwidth(userConfigData.getBandwidth());
                        context.setSymbolRate(userConfigData.getSymbolRate());

                        // start slide show on a separate thread
                        int result = doStartSlideShow(audioDevice, appPath, context);
                        if (result != 900) {
                            logger.error("Tried to start slide show but got an error. Might not be running.");
                        }else{
                            context.resetConsecutiveResetIdleCount();
                            scr.setStatus(ScannerControl.SLIDESHOW_RUNNING.name());
                            scannerService.save(scr);
                        }
                    }
                } else {
                    context.setDisplayWanted(true);
                    if( scr.getStatus().equals(ScannerControl.RESET_IDLE.name()))
                        context.bumpConsecutiveResetIdleCount();
                    // NTM: Should we pick a status here from the enabled channel list?

                }
                // if there are enabled channels
                if (context.getEnabledChannelCount() > 0) {
                    // make a list of channels to process.
                    List<ChannelProcessingData> processingList = new ArrayList<>();
                    List<TunerConfigRecord> enabledChannels = channelService.getEnabledChannels();
                    List<TunerConfigRecord> orderedEnabledChannels = new ArrayList<>();
                    // create an ordered list where the current channel in use is put into the list first.
                    orderedEnabledChannels.add(channelService.get(context.getCurrentChannelIdInUse()));
                    // now put the rest of the enabled channels into the list after the one we are working on.
                    // now we can try each one starting with whatever the last one was.
                    orderedEnabledChannels.addAll(enabledChannels.stream().filter(p ->
                            p.getId() != context.getCurrentChannelIdInUse()).collect(Collectors.toList()));

                    orderedEnabledChannels.forEach(p -> processingList.add(new ChannelProcessingData(p.getId())));

                    // now loop through this list until we find one that is receiving.

                    // process each channel until either we get one that receives ore they are all processed
                    // and if none is receiving at the moment, then start the slide xhow and then go process
                    // them again.

                    Iterator<TunerConfigRecord> it = orderedEnabledChannels.listIterator();
                    /*
                            We will go through each channel in the list in order
                            to find one that will lock. If we exit this loop then that means
                            we went through all the channels and none reached lock.
                    */
                    while (it.hasNext()) {
                        // while here, we are searching for a new channel. If there are enabled channels
                        // then I think we need to disable VLC for SLIDE SHOW because otherwise VLC takes
                        // up too much CPU time.
                        if (scr.getStatus().equals(ScannerControl.SLIDESHOW_RUNNING.name())) {
                            // change it to slideShowRunningScanningForTUner
                            scr.setStatus(ScannerControl.SLIDESHOW_RUNNING_WHILE_SEARCHING.name());
                        }
                        // make sure we have correct parameters for the channel we are going to scan
                        // also, the current channel needs to be the one in the ScannerControl so if
                        // this is not the current channel, then update it
                        TunerConfigRecord tcr = it.next();
                        long currentChannelId = scannerService.getEnabledIdleChannelId();
                        if (context.getCurrentChannelIdInUse() != currentChannelId ||
                                tcr.getId() != currentChannelId) {
                            context.setFrequency(String.valueOf(tcr.getFrequency()));
                            context.setSymbolRate(String.valueOf(tcr.getSymbolRate()));
                            context.setBandwidth(String.valueOf(tcr.getBandwidth()));
                            context.setInputDevice(String.valueOf(tcr.getInputDevice()));
                            context.setCurrentChannelIdInUse(tcr.getId());
                        }
                        // I think we need to change the ScannerControl to point to the new channel now
                        scr.setChannelId(tcr.getId());
                        scannerService.copyTunerConfigData(tcr);
                        scannerService.save(scr);
                        logger.info("STATE MACHINE STARTING: SCR: " + scr);
                        TunerStateMachineStates result = processChannel(context);
                        logger.info("STATE MACHINE EXITING: with result " + result + " and SCR: " + scr);
                    }
                    // if we got here, then there were no channels that attained lock
                    // so try to go to another channel or turn on the slide show.
                    logger.info("Scanned all enabled channels but no LOCK found. Going to slide show.");
                    scr.setStatus(ScannerControl.SLIDESHOW_RUNNING.name());
                    scr.setChannelId(0);
                    scannerService.save(scr);
                    context.bumpConsecutiveResetCount();
                } else {
                    // wait a few seconds and then look again
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        logger.error("Sleep interrupted while sleeping with slide show running.");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Got error in main loop. ", e);
        }
    }


    // process this channel to see if we can lock on to its signal
    // we'll return only if the state changes or is still in RESET
    // if it is in RESET when it returns, then it failed to lock. look
    // at the context to make sure.
    // before returning under any condition, we'll close the fifo
    private TunerStateMachineStates processChannel(VersatuneContext context) {
        long currentChannelId = context.getCurrentChannelIdInUse();
        TunerStateMachineStates result = TunerStateMachineStates.TUNER_STARTING;

        long newChannelId = currentChannelId;
        // start tuner up for this channel
        executeStartUpForCombituner(context);
        context.setCurrentState(TunerStateMachineStates.TUNER_STARTING);
        context.setCurrentEvent(TunerStateMachineEvents.TUNER_NEW_SCAN);
        // turn off the Signal Locked
        try{
            gpioService.TurnLockOff();
        }catch(Exception e){
            logger.error("Could not turn off the Signal Locked LED.", e);
        }
        try {
            // get next block of lines...
            List<String> fifoLines = new ArrayList<>();
            while (true) {
                fifoLines.clear();
                int currentFifoLineCount = context.getFifoLines().size();
                if (context.getFifoLines().size() > 1000) {
                    linuxIoService.clearFifoLinesInContext(context);
                }
                try {
                    context.setIpcFifo(context.getFifoPath());
                    fifoLines = linuxIoService.linuxGetLinesFromPipe(context);
                    context.setConsecutiveReadTimeouts(0);
                } catch (ExecutionException ee) {
                    logger.error("CompleteableFuture returned Execution Error. ", ee);
                    throw new RuntimeException("EEE - getLinesFromFifo -- CompleteableFuture returned Execution Error");
                } catch (InterruptedException ie) {
                    logger.error("*** TIMEOUT *** CompletableFuture returned InterruptedException ie. ", ie);
                    throw new RuntimeException("EEE - getLinesFromFifo -- CompletableFuture returned InterruptedException");
                } catch (TimeoutException to) {
                    //  most likely timeout...
                    context.setConsecutiveReadTimeouts(context.getConsecutiveReadTimeouts() + 1);
                    logger.error("CompletableFuture returned Timeout for Pipe Read - currentState is: " + context.getCurrentState().name());
                    int timeoutFifoLineCount = context.getFifoLines().size();
                    logger.info("*** TIMEOUT *** Original FIFO lineCount: " + currentFifoLineCount + " and lineCount at timeout: " + timeoutFifoLineCount);
                    if (context.getConsecutiveReadTimeouts() > 1) {
                        // close the fifo
                        linuxIoService.closeLinuxPipe(context);
                        newChannelId = -1L;
                        context.setConsecutiveReadTimeouts(0);
                        break;
                    }
                    if (fifoLines.size() == 0) {
                        if (timeoutFifoLineCount == currentFifoLineCount) {
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                logger.error("Interrupted during sleep on timeout in READ.", e);
                            }
                            continue;
                        } else {
                            // need to get most recent lines
                            List<String> missingLines = new ArrayList<>();
                            try {
                                missingLines.addAll(context.getFifoLines().subList(currentFifoLineCount, timeoutFifoLineCount - 1));
                            } catch (Exception ex) {
                                logger.error("Could not get extra lines.", ex);
                            }
                            fifoLines.clear();
                            fifoLines.addAll(missingLines);
                        }
                        //continue;
                    }
                }
                // at this point we have lines, so let us process the lines and see what state we can go to
                TunerStateMachineStates nextState = processLines(fifoLines, context);
                if (nextState.equals(TunerStateMachineStates.TUNER_RESET)) {
                    // break if we are reset.
                    result = nextState;
                    break;
                } else if (nextState.equals(TunerStateMachineStates.TUNER_RECEIVING_DATA) &&
                        context.getCurrentState().equals(TunerStateMachineStates.TUNER_SIGNAL_LOCKED)) {
                    // start up the VLC for the tuner
                    if (!context.getVlcRunningForSlideShow() && !context.getVlcRunningForTuner()) {
                        executeStartupForTunerVlc(context.getAppPath(), context.getAudioDevice(), context);
                    }
                    context.setCurrentState(nextState);
                    // since we are now receiving we need to make the current channel the active Scan Channel
                    context.getScr().setChannelId(currentChannelId);
                    scannerService.copyTunerConfigData(channelService.get(context.getCurrentChannelIdInUse()));
                    scannerService.save(context.getScr());
                    linuxIoService.clearFifoLinesInContext(context);

                    // turn on the SIGNAL LOCKED LED now
                    try{
                        gpioService.TurnLockOn();
                    }catch(Exception e){
                        logger.error("Could not turn on the Signal Locked LED.", e);
                    }


                } else if (nextState.equals(TunerStateMachineStates.TUNER_RECEIVING_DATA)) {
                    // just need to verify that VLC is tracking the correct stream
                    // let's try another way to see if VLC is running for the tuner.
                    int code = checkIfVlcIsStreamingTuner(context);
                    if (code == 0) {
                        // we can start up the VLC for the tuner as it is not yet running according
                        // to port 1314
                    }
                    if (!context.getVlcRunningForTuner()) {
                        executeStartupForTunerVlc(context.getAppPath(), context.getAudioDevice(), context);
                    }
                    context.resetConsecutiveResetCount();
                    linuxIoService.clearFifoLinesInContext(context);
                } else {
                    // verify we are running in vlcTunermode
                    // sleep a bit, so we can wait for the next report from the CombiTuner
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ie) {
                        throw new RuntimeException("StateMachineSleep interrupted. ");
                    }
                    // look here to see if anything changed from the Scanner Control
                }
            }
        } catch (Exception e) {
            logger.error("processChannel had error. ", e);
        } finally {
            linuxIoService.closeLinuxPipe(context);
            shutdownCombiTunerExpressIfRunning(config.getAppPath());
            // Note: fifo will be removed and then re-initialized when next channel is started.
        }
        return result;
    }

    /*
            This is the state machine.
            We'll simply take each line, convert it to an event then look up the event in the
            truth table to see what the next state is. The Next State will be returned.
            It will be up to the caller of this method to exit if the next state has changed.

    */
    private TunerStateMachineStates processLines(List<String> fifoLines, VersatuneContext context) {
        int num = fifoLines.size();
        TunerStateMachineStates result = TunerStateMachineStates.TUNER_UNDEFINED;
        if (num < 0) {
            result = TunerStateMachineStates.TUNER_RESET;
            throw new RuntimeException("Got error when reading the fifo.");
        } else if (num == 0) {
            context.bumpConsecutiveNoData();
            logger.info("processLines: Got NO DATA while in state " + context.getCurrentState().name());
            if (context.getConsecutiveNoData() > 120) {
                result = TunerStateMachineStates.TUNER_RESET;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            // got some data, let's process it. Map input to an event
            context.setConsecutiveNoData(0);
            Iterator<String> it = fifoLines.listIterator();
            boolean resetNeeded = false;
            StateTransitionIO transitionResult = new StateTransitionIO();
            while (it.hasNext()) {
                String line = it.next();
                if (context.getDisplayWanted() && !line.isEmpty()) {
                    displayMessage.dsiplayMessageText(line);
                }
                if (line.length() > 3) {
                    String responseLine = processTunerInputData(line);
                    if (responseLine.length() > 3 && responseLine.startsWith("-> ")) {
                        updateVlcOverlayText(responseLine, context.getOverlayPath());
                    }
                }
                StateTransitionIO transitionIO = new StateTransitionIO();
                transitionIO.setCurrentState(context.getCurrentState());
                transitionIO.setTunerEvent(eventMap.fetchTunerEventFromInputData(line));
                if (transitionIO.getTunerEvent().equals(TunerStateMachineEvents.TUNER_SEARCHING_FOR_SIGNAL)) {
                    // save frequency Freq is 417.000000 MHz,
                    int fromIndex = line.indexOf("Freq is ") + 8;
                    int toIndex = line.lastIndexOf(" MHz");
                    context.setScanChannelFrequency(line.substring(fromIndex, toIndex));

                }
                if (transitionIO.getTunerEvent().equals(TunerStateMachineEvents.TUNER_SIGNAL_DETECTED_ATTEMPTING_LOCK)) {
                    // save Freq is 417 MHz,
                    int fromIndex = line.indexOf("Freq is ") + 8;
                    int toIndex = line.lastIndexOf(" MHz");
                    context.setLockChannelFrequency(line.substring(fromIndex, toIndex));
                }
                transitionResult = stateTable.processCurrentEvent(transitionIO);
                // transitionResult should be the same as transitionIO for input, but new for output
                if (transitionResult.getNextState().equals(TunerStateMachineStates.TUNER_RESET)) {
                    // need this in case there are more inputs to scan through. We want to make sure we process
                    // all input messages before reading the next set from the fifo.
                    resetNeeded = true;
                    break;
                }
                context.setNextState(TunerStateMachineStates.TUNER_UNDEFINED);
                context.setCurrentState(transitionResult.getNextState());
                if (context.getCurrentState().equals(TunerStateMachineStates.TUNER_RECEIVING_DATA)) {
                    // first check if VLC is running;
                    if (!context.getVlcTracker().isRunning()) {
                        context.setDisplayWanted(false);
                        executeStartupForTunerVlc(context.getAppPath(), context.getAudioDevice(), context);
                        // this should turn on "tuner is running"
                    }
                }
            }
            if (resetNeeded) {
                result = TunerStateMachineStates.TUNER_RESET;
            } else {
                result = transitionResult.getNextState();
            }
        }
        return result;
    }


    private void modifyFile(String filePath, String newString) {
        File fileToBeModified = new File(filePath);
        StringBuilder oldContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileToBeModified))) {
            String line = reader.readLine();
            while (line != null) {
                oldContent.append(line).append(System.lineSeparator());
                line = reader.readLine();
            }
            String content = oldContent.toString();
            String newContent = content.replaceAll(content, newString);
            try (FileWriter writer = new FileWriter(fileToBeModified)) {
                writer.write(newContent);
            }
        } catch (IOException e) {
            logger.error("Could not update the config file.", e);
        }
    }

    private UserConfigData getUserConfigData(String filePath) {
        UserConfigData userData = new UserConfigData();
        File configFile = new File(filePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line = reader.readLine();
            List<String> lines = new ArrayList<>();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            if (lines.size() > 0) {
                for (String s : lines) {
                    s = s.toLowerCase();
                    if (s.startsWith("freq") || s.startsWith("frequency")) {
                        String[] parts = s.split("=", -1);
                        if (parts.length >= 2) {
                            userData.setFrequency(parts[1]);
                        }
                    } else if (s.startsWith("bw") || s.startsWith("bandwidth")) {
                        String[] parts = s.split("=", -1);
                        if (parts.length >= 2) {
                            userData.setBandwidth(parts[1]);
                        }
                    } else if (s.startsWith("symb") || s.startsWith("symbolrate")) {
                        String[] parts = s.split("=", -1);
                        if (parts.length >= 2) {
                            userData.setSymbolRate(parts[1]);
                        }
                    } else if (s.startsWith("aud") || s.startsWith("audio")) {
                        String[] parts = s.split("=", -1);
                        if (parts.length >= 2) {
                            userData.setAudioOut(parts[1]);
                        }
                    } else if (s.startsWith("chan") || s.startsWith("channel")) {
                        String[] parts = s.split("=", -1);
                        if (parts.length >= 2) {
                            userData.setChannel(parts[1]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Could read the dvb-t_config.txt file.", e);
        }
        return userData;
    }

    /*
            This looks up data in the userConfigFile to create the audioDevice that
            will be used in VLC calls
     */
    private String getAudioDeviceForScan(UserConfigData userConfigData) {
        // default audio device:
        String audioDevice = "hw:CARD=b1,DEV=0";
        try {
            // try writing a command and getting the response back
            Runtime runtime = Runtime.getRuntime();
            String commandToRun = "aplay -l | grep bcm2835 | grep \"Headphones\" | perl -lane 'print substr($F[1],0,1)'";
            commandToRun = "aplay -L";
            Process process = runtime.exec(commandToRun);
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String inputLine;
            List<String> lines = new ArrayList<>();
            while ((inputLine = br.readLine()) != null) {
                lines.add(inputLine);
            }

            // create list of AudioDeviceSpec objects from the output
            List<AudioDeviceSpec> audioSpecs = new ArrayList<>();
            AudioDeviceSpec spec = null;
            for (String s : lines) {
                if (!s.startsWith(" ")) {
                    if (null != spec) {
                        audioSpecs.add(spec);
                    }
                    spec = new AudioDeviceSpec();
                    spec.setDeviceType(s);
                } else {
                    if (spec.getDeviceInfo1().isEmpty())
                        spec.setDeviceInfo1(s);
                    else
                        spec.setDeviceInfo2(s);
                }
            }

            // I believe we only need the hw:CARD types, so I'll filter those out of the list.
            // now based on what is in the config file for audio we can pick one or the other of the hw:CARD types.
            List<AudioDeviceSpec> hwCards = audioSpecs.stream()
                    .filter(p -> p.getDeviceType().toLowerCase().contains("hw:card"))
                    .collect(Collectors.toList());

            if (userConfigData.getAudioOut().equals("rpi")) {
                // we want to use headphones, so look for card headphones
                Optional<AudioDeviceSpec> aoutRef = hwCards.stream().filter(p -> p.getDeviceType().contains("Headphones"))
                        .findFirst();
                if (aoutRef.isPresent()) {
                    audioDevice = aoutRef.get().getDeviceType();
                }
            }
        } catch (IOException e) {
            logger.error("Could not get audio device from configuration. Using default. ", e);
        }
        logger.info(">> getAudioDeviceForScan << - returning " + audioDevice + " for audioDevice.");
        return audioDevice;
    }


    /*
            input parameters: runningId = long sequence number of channel that is running

            Check if that is the channel that control record thinks is running. Then check
            if it is enabled.
    */
    private boolean checkIfRunningIsEnabled(long runningId) {
        ScannerControlRecord scr = getScannerControlRecord();
        if (scr.getChannelId() == runningId) {
            if (channelService.getOptonal(runningId).isPresent()) {
                return channelService.get(runningId).isEnableChan();
            }
        }
        return false;
    }


    /*

            Returns a good scanner control record.
    */
    private ScannerControlRecord getScannerControlRecord() {
        Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
        if (srRef.isEmpty()) {
            throw new RuntimeException("Could not create a scanner control record. Exiting.");
        }
        return srRef.get();
    }


    private void shutdownVlcIfRunning(String appPath, VersatuneContext context) {
        List<String> cmdList = new ArrayList<>();
        cmdList.clear();
        // full path to shell script
        cmdList.add(String.format("%s/scripts/killRunningVlc.sh", appPath));
        int result = runCommand(cmdList, true);
        if (result == 0) {
            context.getVlcTracker().setRunning(false);
            context.getVlcTracker().setTunerRunning(false);
            context.getVlcTracker().setSlideShowRunning(false);
            context.setVlcRunningForSlideShow(false);
            context.setVlcRunningForTuner(false);
        } else {
            context.getVlcTracker().setVlcStartStopError(result);
            logger.info("ERROR: could not stop VLC." + result);
        }
    }

    /*
            Check to see if port 1314 is in use. If it is, that means that the
            VLC is connected to the CombiTunerExpress stream. That is, the VLC
            is running the Tuner stream.
    */
    private int checkIfVlcIsStreamingTuner(VersatuneContext context) {

        List<String> cmdList = new ArrayList<>();
        cmdList.clear();
        cmdList.add(String.format("%s/scripts/checkIfVlcRunningForTuner.sh", context.getAppPath()));
        int result = runCommand(cmdList, true);
        return result;
    }

    private int shutdownCombiTunerExpressIfRunning(String appPath) {
        List<String> cmdList = new ArrayList<>();
        cmdList.clear();
        // full path to shell script
        cmdList.add(String.format("%s/scripts/killRunningCombiTuner.sh", appPath));
        int result = runCommand(cmdList, true);
        return result;
    }


    /*
            This will start up the combituner.
            We will pass in a context here rather than all the parameters.
            Just calls the original with th parameters
    */
    private int executeStartUpForCombituner(VersatuneContext context) {
        return executeStartUpForCombituner(context.getFrequency(),
                context.getSymbolRate(),
                context.getBandwidth(),
                context.getOverlayPath(),
                context.getFifoPath(),
                context.getAppPath()
        );
    }

    /*
        This will start up the combituner.
        We might have to update a file here instead of passing the args. I think I'm going to look
        at trying a few other things. (We can use the VersatuneContext object to pass the args - RFU)
    */
    private int executeStartUpForCombituner(String frequency,
                                            String symbolRate,
                                            String bandwidth,
                                            String overlayPath,
                                            String fifoPath,
                                            String appPath) {
        List<String> commands = new ArrayList<>();
        // just build the command with its args and send it out.

        ProcessBuilder processBuilder = new ProcessBuilder();


        long startTime = System.currentTimeMillis();
        logger.trace("creating and starting the process.");

        ////////////////////////////  BELOW - New on Tuesday Nov 23 2022 /////////////////////////////////

        int result;
        // executed each of the commands that were in the script but do them here with commands.
        String runPath = appPath + "/scripts";
        List<String> cmdList = new ArrayList<>();

        cmdList.clear();
        cmdList.add("id");
        cmdList.add("-u");
        cmdList.add("-n");
        result = runCommand(cmdList, true);


        cmdList.clear();
        cmdList.add("bash");
        cmdList.add("-c");
        cmdList.add("cd");
        cmdList.add(runPath);
        result = runCommand(cmdList, true);

        cmdList.clear();
        cmdList.add("bash");
        cmdList.add("-c");
        cmdList.add("pwd");
        result = runCommand(cmdList, true);

        cmdList.clear();
        cmdList.add("rm");
        cmdList.add(overlayPath);
        result = runCommand(cmdList, true);

        cmdList.clear();
        cmdList.add("cp");
        String blankOverlayPath = appPath + "/data/blank_vlc_overlay.txt";
        cmdList.add(blankOverlayPath);
        cmdList.add(overlayPath);
        result = runCommand(cmdList, true);

        // shut down combituner if it is running
        shutdownCombiTunerExpressIfRunning(appPath);

        // make sure FIFO is closed
        linuxIoService.closeLinuxPipe(context);

        // remove old fifo path - hopefully it was closed
        logger.trace("Removing fifo before starting a new combituner instance");
        cmdList.clear();
        cmdList.add("rm");
        cmdList.add(fifoPath);
        result = runCommand(cmdList, true);
        logger.info("Status from script after removing FIFO: " + result);

        try {
            Thread.sleep(100);
        } catch (Exception e) {
            // ignoring
        }


        logger.trace("Making new fifo before starting a new combituner instance with frequency: " + frequency + " and BW: " + bandwidth);
        // make new fifo
        cmdList.clear();
        cmdList.add("mkfifo");
        cmdList.add(fifoPath);
        result = runCommand(cmdList, true);
        logger.info("Status from script after making new FIFO: " + result);

        cmdList.clear();
        cmdList.add("id");
        cmdList.add("-u");
        cmdList.add("-n");
        result = runCommand(cmdList, true);

        // change directory to the scripts directory
        cmdList.clear();
        cmdList.add("bash");
        cmdList.add("-c");
        cmdList.add("cd");
        cmdList.add(String.format("%s/scripts", appPath));
        result = runCommand(cmdList, true);

        // show working directory now
        cmdList.clear();
        cmdList.add("bash");
        cmdList.add("-c");
        cmdList.add("pwd");
        result = runCommand(cmdList, true);

        // Now try to start the CombiTuner from the combituner shell script.

        cmdList.clear();
        // full path to shell script
        cmdList.add(String.format("%s/scripts/runCombiTunerExpress.sh", appPath));
        cmdList.add("dvbt");
        cmdList.add(frequency);
        cmdList.add(bandwidth);
        // full path to CombiTunerExpress
        cmdList.add(String.format("%s/data/CombiTunerExpress", appPath));
        // full path to fifo
        cmdList.add(String.format("%s/data/knucker_status_fifo", appPath));

        ProcessBuilder pb = new ProcessBuilder(cmdList);
        try {
            Process process = pb.start();
            result = process.waitFor();
        } catch (Exception e) {
            logger.error("Could not start CombiTunerExpress.", e);
        } finally {
            logger.info("CombiTunerExpress should now be running.");
        }

        cmdList.clear();
        cmdList.add("sleep");
        cmdList.add("0.1");
        result = runCommand(cmdList, true);


        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Elapsed Time to start Combituner iw " + elapsed + " milliseconds.");
        return result;
    }

    ////////////////////////////  ABOVE - New on Tuesday Nov 23 2022 /////////////////////////////////

    ////////////////////////////  BELOW - New on Wed Nov 30 2022 /////////////////////////////////////

    /*
        Run shell commands to start up the VLC for the tuner
    */
    private int executeStartupForTunerVlc(String appPath, String audioDevice, VersatuneContext context) {
        logger.info(">> executeStartupForTunerVlc <<");
        int result = -1;
        List<String> cmdList = new ArrayList<>();

        cmdList.clear();
        cmdList.add("bash");
        cmdList.add("-c");
        cmdList.add("cd");
        cmdList.add(String.format("%s/scripts", appPath));
        result = runCommand(cmdList, true);

        shutdownVlcIfRunning(appPath, context);

        try {
            Thread.sleep(100L);
        } catch (Exception e) {
            logger.error("Sleep interrupted after killing vlc");
        }

        // Create dummy marquee overlay file - clear any old ones
        String blankOverlayPath = appPath + "/data/blank_vlc_overlay.txt";
        String overlayPath = appPath + "/data/vlc_overlay.txt";
        String blankTsPath = appPath + "/data/blank.ts";

        // deleted existing overlayPath
        cmdList.clear();
        cmdList.add("rm");
        cmdList.add(overlayPath);
        result = runCommand(cmdList, true);

        // set it to a blank line
        cmdList.clear();
        cmdList.add("cp");
        cmdList.add(blankOverlayPath);
        cmdList.add(overlayPath);
        result = runCommand(cmdList, true);

        logger.info(">> executeStartupForTunerVlc << -- audioDevice is: [" + audioDevice + "].");
        // play short dummy file to prime VLC (not sure why we need to actually do this)
        StringBuffer sb = new StringBuffer("su -c 'cvlc --codec h264_v4l2m2m --no-video-title-show --codec h264_v412m2m --quiet ");
        sb.append("--sub-filter marq --marq-size 20 --marq-x 25 --marq-position=8 --marq-file ");
        sb.append("\"").append(overlayPath).append("\"");
        sb.append(" --gain 3 --alsa-audio-device ").append(audioDevice);
        sb.append(" ").append(blankTsPath).append(" vlc://quit -L' pi &\n");
        cmdList.add(sb.toString());
        try {
            executeCommandList(cmdList, true);
            result = 901;
            logger.info("Successfully ran short VLC Prime Script.");

        } catch (Exception e) {
            logger.error("ERROR: could not execute the script to  start the slide show.", e);
            result = -1;
        }

        // sleep a little
        cmdList.clear();
        cmdList.add("sleep");
        cmdList.add("0.1");
        result = runCommand(cmdList, true);

        // now get ready to send the hdmi output to the VLC
        cmdList.clear();
        sb = new StringBuffer("su -c 'cvlc --codec h264_v4l2m2m --video-title-timeout=100 --codec h264_v412m2m --quiet ");
        sb.append("--sub-filter marq --marq-size 20 --marq-x 25 --marq-position=8 --marq-file ");
        sb.append("\"").append(overlayPath).append("\"");
        sb.append(" --gain 3 --alsa-audio-device ").append(audioDevice);
        sb.append(" udp://@127.0.0.1:1314 -L' pi &\n");
        cmdList.add(sb.toString());
        logger.info("** STARTING TUNER:");
        logger.info("   " + sb);
        logger.info("** STARTING TUNER:");
        try {
            executeCommandList(cmdList, true);
            result = 902;
            logger.info("Successfully started VLC for Tuner.");
            context.getVlcTracker().setTunerRunning(true);
            context.getVlcTracker().setRunning(true);
            context.getVlcTracker().setSlideShowRunning(false);
            context.getVlcTracker().setTunerStartTime(System.currentTimeMillis());
            context.setVlcRunningForTuner(true);

        } catch (Exception e) {
            logger.error("ERROR: could not execute the script to  start the tuner VLC.", e);
            result = -1;
        }

        return result;
    }
    ////////////////////////////  ABOVE - new on Wed Nov 30 2022  /////////////////////////////////

    ////////////////////////////  BELOW - new on Monday Nov 28 2022  /////////////////////////////////

    /*
            Run shell commands to start up the slide show when we don't have a tuner running
            This starts the slide show running, but independently the scanner will scan looking
            for a new channel to lock so that it can take over the video from the slide show.
     */
    private int executeStartupForSlideShow(String appPath, VersatuneContext context) {
        logger.info(">> executeStartupForSlideShow <<");
        List<String> cmdList = new ArrayList<>();
        int result = 0;
        // change directory to the images directory
        cmdList.clear();
        cmdList.add("bash");
        cmdList.add("-c");
        cmdList.add("cd");
        cmdList.add(String.format("%s/images", appPath));
        result = runCommand(cmdList, true);

        // show working directory now
        cmdList.clear();
        cmdList.add("bash");
        cmdList.add("-c");
        cmdList.add("pwd");
        result = runCommand(cmdList, true);

        shutdownVlcIfRunning(appPath, context);

        // sleep a bit
        cmdList.clear();
        cmdList.add("sleep");
        cmdList.add("0.1");
        result = runCommand(cmdList, true);

        cmdList.clear();

        StringBuffer sb = new StringBuffer("su -c 'cvlc  --codec h264_v4l2m2m --no-video-title-show ");
        File[] files = new File(String.format("%s/images", appPath)).listFiles();
        for (File file : files) {
            if (file.isFile()) {
                // check to make sure file is one of the allowed extensions
                String extension = FilenameUtils.getExtension(file.getName());
                if (allowedExtensions.contains(extension.toLowerCase())) {
                    sb.append("file://").append(appPath).append("/images/").append(file.getName()).append(" ");
                }
            }
        }
        if (null == files || files.length < 1) {
            sb.append("file://").append(appPath).append("/data/MyGrandPianoInItsNewHome.jpeg");

        }
        // to get current hdmi screen resolution
        //  cat /sys/class/graphics/fb0/virtual_size
        // ours is 1920 x 1080
        //sb.append(" -L' pi &\n");
        sb.append(" -L' pi &\n");
        cmdList.add(sb.toString());
        //result = runCommand(cmdList, true);

        try {
            executeCommandList(cmdList, false);
            result = 900;
        } catch (Exception e) {
            logger.error("ERROR: could not execute the script to  start the slide show.", e.getMessage());
            result = -1;
        }
        if (result == 900) {
            context.getVlcTracker().setSlideShowRunning(true);
            context.getVlcTracker().setSlideShowStartTime(System.currentTimeMillis());
            context.getVlcTracker().setStartTime(System.currentTimeMillis());
            context.getVlcTracker().setRunning(true);
            context.getVlcTracker().setTunerRunning(false);
            context.setVlcRunningForSlideShow(true);
        }
        return result;
    }

    ////////////////////////////  ABOVE - new on Monday Nov 28 2022  /////////////////////////////////

    ////////////////////////////  BELOW - This can or should be replaced /////////////////////////////
    private void executeCommandList(List<String> commandList, boolean logit) throws IOException {
        File tempScript = createTempScriptFromCommandList(commandList, logit);
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

    private File createTempScriptFromCommandList(List<String> commands, boolean logit) throws IOException {
        logger.info("creating the temporary script file from command list.");
        File tempScript = File.createTempFile("scriptX", null);
        Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
                tempScript));
        PrintWriter printWriter = new PrintWriter(streamWriter);
        if (logit)
            logger.info("##########################################################################################");
        for (String s : commands) {
            printWriter.println(s);
            if (logit)
                logger.info(s);
        }
        if (logit)
            logger.info("##########################################################################################");
        printWriter.close();
        return tempScript;
    }

    ////////////////////////////  ABOVE - This can or should be replaced /////////////////////////////


    public int runCommand(List<String> commands, boolean destroy) {
        ProcessBuilder processBuilder = new ProcessBuilder().command(commands);
        int result = -1;
        try {
            Process process = processBuilder.start();

            //read the output
            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String output = null;
            while ((output = bufferedReader.readLine()) != null) {
                System.out.println(output);
            }

            //wait for the process to complete
            result = process.waitFor();

            //close the resources
            bufferedReader.close();
            // don't want to destroy process that was created in for the background
            if (destroy)
                process.destroy();

        } catch (IOException | InterruptedException e) {
            logger.error("Theres was an error. ", e);
        }
        return result;
    }


    /*
            Need this to be a future and have the future return true when the stream is running
            then we can set the stream active to true.
     */
    private void startTunerStream2() {
        StartTunerStreamTask t = new StartTunerStreamTask();
        executor.execute(t);
    }


    private void startSlideShow2() {
        StartSlideShowTask t = new StartSlideShowTask();
        executor.execute(t);
    }


    public void updateVlcOverlayText(String text, String overlaytextPath) {
        int len = text.length();
        if (logger.isTraceEnabled()) logger.trace("In updateVlcOverlayText with text: " + text);
        if (text.startsWith("-> ")) {
            String overlayText = text.substring(3);
            try {
                FileWriter writeObj = new FileWriter(overlaytextPath, false);
                writeObj.write(overlayText);
                writeObj.close();
            } catch (IOException ioe) {
                logger.error("Failed to update the VLC overlay text file.", ioe);
            }
        }
    }


    private String processTunerInputData(String iModel) {

        RcvrSignal signal = signalService.getSignalFromPool();
        List<RcvrSignal> recs = signalService.listall();

        String test = iModel.toUpperCase().substring(0, 3);

        switch (test) {
            case "SSI":
                if (signal.getSsi().isEmpty() || signal.getSsi().isBlank()) {
                    signal.setSsi(iModel);
                }
                break;
            case "SQI":
                signal.setSqi(iModel);
                if (signal.getSqi().isEmpty() || signal.getSqi().isBlank()) {
                    signal.setSqi(iModel);
                }
                break;
            case "SNR":
                signal.setSnr(iModel);
                if (signal.getSnr().isEmpty() || signal.getSnr().isBlank()) {
                    signal.setSnr(iModel);
                }
                break;
            case "PER":
                signal.setPer(iModel);
                if (signal.getPer().isEmpty() || signal.getPer().isBlank()) {
                    signal.setPer(iModel);
                }
                break;
            default:
        }
        // now check if all 4 parts are present
        if (signal.getSsi().isEmpty() || signal.getSsi().isBlank())
            signal.setReady(false);
        else if (signal.getSqi().isEmpty() || signal.getSqi().isBlank())
            signal.setReady(false);
        else if (signal.getSnr().isEmpty() || signal.getSnr().isBlank())
            signal.setReady(false);
        else signal.setReady(!signal.getPer().isEmpty() && !signal.getPer().isBlank());

        signalService.save(signal);

        String result = "NO DATA";
        if (signal.isReady()) {
            result = "-> SSI=" + signal.getSsi().substring(7) + "  SQI=" + signal.getSqi().substring(7) + "  SNR=" + signal.getSnr().substring(7) + "  PER=" + signal.getPer().substring(7);
            long currentTime = System.currentTimeMillis();
            List<RcvrSignal> batch = new ArrayList<>();
            for (RcvrSignal rs : recs) {
                if (rs.getTimestamp() < currentTime - 30000L) {
                    batch.add(rs);
                }
            }
            if (batch.size() > 0) {
                signalService.deleteBatch(batch);
            }
        }

        return result;
    }


}

