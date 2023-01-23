//package com.datvexpress.ws.versatune.background;
//
//import com.datvexpress.ws.versatune.config.VersatuneStartupConfig;
//import com.datvexpress.ws.versatune.enums.ScannerControl;
//import com.datvexpress.ws.versatune.enums.TunerStatus;
//import com.datvexpress.ws.versatune.model.*;
//import com.datvexpress.ws.versatune.screenutils.DisplayMessage;
//import com.datvexpress.ws.versatune.service.ChannelService;
//import com.datvexpress.ws.versatune.service.ScannerService;
//import com.datvexpress.ws.versatune.service.SignalService;
//import com.sun.jna.Library;
//import com.sun.jna.Native;
//import org.apache.commons.io.FilenameUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.ApplicationContext;
//import org.springframework.core.task.TaskExecutor;
//import org.springframework.stereotype.Component;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.function.Supplier;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//@Component
//public class DvbtWorker implements Runnable {
//    Logger logger = LoggerFactory.getLogger(getClass());
//
//    private final ScannerService scannerService;
//    private final SignalService signalService;
//    private final ChannelService channelService;
//    private final DisplayMessage displayMessage;
//    private final ApplicationContext context;
//    private final TaskExecutor executor;
//
//    private final VersatuneStartupConfig config;
//
//    public DvbtWorker(
//            DisplayMessage displayMessage,
//            ApplicationContext context,
//            ChannelService chanService,
//            SignalService signalService,
//            ScannerService scannerService,
//            VersatuneStartupConfig config,
//            @Qualifier("threadPoolExecutor") TaskExecutor executor
//    ) {
//        this.displayMessage = displayMessage;
//        this.context = context;
//        this.executor = executor;
//        this.channelService = chanService;
//        this.signalService = signalService;
//        this.scannerService = scannerService;
//        this.config = config;
//    }
//
//    List<String> allowedExtensions = Arrays.asList("jpeg", "mp4", "png", "jpg", "gif");
//
//    /*
//            Get the Last Known Active Tuner Record, if any.
//     */
//    private Optional<TunerConfigRecord> getLastKnownActiveTunerRecord() {
//        Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
//        long dbId = scannerService.getActiveChannelDbId();
//        TunerConfigRecord tcr = channelService.get(dbId);
//        return Optional.of(tcr);
//
//    }
//
//    /*
//            This is the scan state logic. State comes from the ScannerControlRecord. We return
//            the DbId for the scannerService record that is currently scanning. If the SlideShow
//            is running then the DbId will be 0;
//     */
//    private long runScanLogic(String overlayPath,
//                              String configPath,
//                              String fifoPath,
//                              String blankTsPath,
//                              String appPath,
//                              int enabledChannelCount,
//                              boolean newStartup){
//
//        String audioDevice = "";
//        // this should ALWAYS return a record
//        Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
//        if (srRef.isEmpty()){
//            throw new RuntimeException("Could not create a scanner control record. Exiting.");
//        }
//        // we need the Scanner control record.
//        ScannerControlRecord scr = srRef.get();;
//
//        // now, we need the number of all enabled channels.
//        enabledChannelCount = channelService.getEnabledChannelCount();
//
//        // the rest depends on the current state of the scr.
//        // get status from Scanner Control
//        String currentStatus = scr.getStatus();
//        long currentChannelId = scr.getChannelId();
//
//        long nextId = 0;
//
//        if (currentStatus.equals(ScannerControl.EMPTY.name())){
//            // get first enabled channel
//            nextId = channelService.getFirstReadyChannelsId();
//        }else if(currentStatus.startsWith("ENABLED")){
//            // an enabled channel must have changed state but still is an enabled channel
//            // so use the current channel number from ScannerControl and get next enabled channel
//            nextId = channelService.getNextScanChannelFromEnabledChannels(scr.getScanChannel());
//        }else if(currentStatus.equals(ScannerControl.DISABLED)){
//            nextId = channelService.getNextScanChannelFromEnabledChannels(scr.getScanChannel());
//        }else if (currentStatus.equals(ScannerControl.SLIDESHOW_RUNNING)){
//            nextId = 0L;
//            startScanWithSlideShow( configPath, audioDevice,  scr,  appPath);
//        }
//        if (nextId > 0){
//            scr.setChannelId(nextId);
//            TunerConfigRecord tcr = channelService.get(nextId);
//            scr.updateWithTunerConfiguration(channelService.get(nextId));
//            scr.setStatus(ScannerControl.ENABLED_PENDING.name());
//            // save the updated scanner info.
//            scannerService.save(scr);
//
//            if (nextId != currentChannelId){
//                // save the configuration
//                updateUserConfigData();
//            }
//
//            // stop VLC and Start up Message Display
//            doStopVlcAndStartMessageDisplay();
//        }else{
//            scr.setScanChannel(0);
//            scr.setChannelId(0);
//            int frequency = Integer.parseInt(getUserConfigData(configPath).getFrequency());
//            int bandwidth = Integer.parseInt(getUserConfigData(configPath).getBandwidth());
//            int symbolRate = Integer.parseInt(getUserConfigData(configPath).getSymbolRate());
//            scr.setFrequency(frequency);
//            scr.setBandwidth(bandwidth);
//            scr.setSymbolRate(symbolRate);
//            scr.setStatus(ScannerControl.SLIDESHOW_RUNNING.name());
//            scannerService.save(scr);
//            startScanWithSlideShow( configPath, audioDevice,  scr,  appPath);
//        }
//        return nextId;
//
//    }
//
//    private void startScanWithSlideShow(String configPath, String audioDevice, ScannerControlRecord scr, String appPath){
//        audioDevice = getAudioDeviceForScan(getUserConfigData(configPath));
//        scr.setScanChannel(0);
//        scr.setChannelId(0);
//        scannerService.save(scr);
//
//        int result = doStartSlideShow(audioDevice,appPath);
//        logger.info("start slide show returned " + result);
//    }
//    /*
//            This tries to start the slide show using an executor service.
//     */
//    private Integer doStartSlideShow(String audioDevice, String appPath){
//
//        ExecutorService executor = Executors.newFixedThreadPool(6);
//
//        Supplier <Integer> runSlideShow = () -> {
//            Integer result = -1;
//            try{
//                result = executeStartupForSlideShow(appPath);
//            }catch(Exception e){
//                logger.error("could not start slide show.",e);
//            }
//            return result;
//
//        };
//
//        CompletableFuture<Integer> cf1 = CompletableFuture.supplyAsync(runSlideShow, executor);
//        Integer result = null;
//        try {
//            result = cf1.get(5000L, TimeUnit.MILLISECONDS);
//        } catch (InterruptedException e) {
//            //throw new RuntimeException(e);
//            result = -2;
//            logger.error("Runtime Exception. ",e);
//        } catch (ExecutionException e) {
//            //throw new RuntimeException(e);
//            logger.error("Execution Error. ", e);
//            result = -3;
//        } catch (TimeoutException e) {
//            result = -4;
//            //throw new RuntimeException(e);
//            logger.info("Timeout when trying to start up slideshow. ");
//        }
//        executor.shutdown();
//        return result;
//
//    }
//
//    private void updateUserConfigData(){
//        String newFreq = "50000";
//        String newBw = "2000";
//        String newSymb = "2000";
//        String newAudio = "hdmi";
//        String newChan = "1";
//        Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
//
//        ScannerControlRecord sr = srRef.get();
//        // if we are here then ScannerControlRecord is valid.
//        newFreq = String.valueOf(sr.getFrequency());
//        newBw = String.valueOf(sr.getBandwidth());
//        newSymb = String.valueOf(sr.getSymbolRate());
//        newChan = String.valueOf(sr.getScanChannel());
//
//        // update the parameters config file - let's make it a JSON file
//        // or, we can pass in the parameters we want to use...
//        String pathName = "/usr/local/apps/versatune/data/dvb-t_config.txt";
//        if (Files.exists(Paths.get(pathName))) {
//
//            StringBuilder sb = new StringBuilder();
//            sb.append("freq=").append(newFreq).append(System.lineSeparator());
//            sb.append("bw=").append(newBw).append(System.lineSeparator());
//            sb.append("symb=").append(newSymb).append(System.lineSeparator());
//            sb.append("audio=").append(newAudio).append(System.lineSeparator());
//            sb.append("chan=").append(newChan).append(System.lineSeparator());
//
//            // write out new content
//            modifyFile(pathName, sb.toString());
//        }
//    }
//
//    private void doStopVlcAndStartMessageDisplay(){
//        shutdownTunerVlc();
//    }
//    @Override
//    public void run() {
//
//        int enabledChannelCount = -1;
//        boolean newStartup = false;
//        String overlayPath = config.getVersatuneOverlayPath();
//        String configPath = config.getVersatuneDvbConfigPath();
//        String fifoPath = config.getVersatuneKnuckerFifo();
//        String blankTsPath = config.getBlankTsPath();
//        String appPath = config.getAppPath();
//
//
//        while (true) {
//
//            long runningId = runScanLogic(overlayPath, configPath, fifoPath, blankTsPath, appPath, enabledChannelCount, newStartup);
////            if(false) {
////                try {
////                    boolean okToGo = true;
////                    newStartup = false; // but check to see if it is...
////                    if (enabledChannelCount == -1) {
////                        // then this is a new startup so lets get the last running channel from
////                        // the database.
////                        newStartup = true;
////                    }
////
////                    enabledChannelCount = channelService.getEnabledChannelCount();
////                    logger.info("START scanning up to " + enabledChannelCount + " channels.");
////                    // read scanner control record; there should only be one
////                    // make sure we have one if there isn't one already.
////                    Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
////
////                    // get the last known ready SCAN CHAN and DB ID of the Scanner Control record
////                    int currentScanChannel = scannerService.getScanChannelNumber();
////
////                    // make sure we have a running scanner control
////                    if (srRef.isEmpty()) {
////                        // we cannot continue until we have a valid ScannerControlRecord
////                        // start slide show?
////
////                        Thread.sleep(10000);
////                        continue;
////                    }
////
////                    // Need to check here to see if SLIDE SHOW is running
////                    // if it is, then if there are no other enabled channels, then just let it run
////
////                    // check if the tuner channel specified in the ScannerControl is still enabled.
////                    ScannerControlRecord sr = srRef.get();
////                    long tunerActiveId = sr.getChannelId();
////                    // check if the tuner channel specified in the ScannerControl is still ACTIVE, i.e., it
////                    // did not go UNLOCKED for more than a few seconds
////                    // at this point if we have a 0 for tunerActiveId then nothing is active or if the
////                    // currently active channel is not enabled then we can try to get another enabled
////                    // channel to scan.
////                    TunerConfigRecord tcr = null;
////                    int currentTunerChannel = 0;
////                    Optional<TunerConfigRecord> tcrRef = channelService.getOptonal(tunerActiveId);
////                    if (tcrRef.isPresent()) {
////                        tcr = tcrRef.get();
////                        currentTunerChannel = tcr.getChannel();
////                    }
////
////                    boolean scanDefinitionChanged = false;
////                /*
////                    we need to see if the scanner is empty or if the slide show is running.
////                    In either case, THEN we check to see if there are any newly enabled channels ready
////                    to start. If there are then set things up appropriately.
////                 */
////                    String scannerStatus = scannerService.getScannerControlStatus();
////                    // this status will be status of channel that is running.
////
////
////                    if (scannerStatus.equals(ScannerControl.EMPTY.name()) ||
////                            scannerStatus.equals(ScannerControl.SLIDESHOW_RUNNING.name()) ||
////                            scannerStatus.equals(ScannerControl.ENABLED_UNLOCKED.name()) ||
////                            scannerStatus.equals(ScannerControl.ENABLED_SEARCH_FAILED.name()) ||
////                            scannerStatus.equals(ScannerControl.ENABLED_NODATA.name()) ||
////                            tunerActiveId == 0 ||
////                            !channelService.get(tunerActiveId).isEnableChan()) {
////                        // if not active, then find the next active tuner channel and put into the active channel
////                        // if there are no enabled channels then nextId will be 0. We really can't do anything so
////                        // just make sure active channel id in the Scanner Record is also 0
////                        // and indicate that scanner control definition changed
////                        scanDefinitionChanged = true;
////                        long nextId = channelService.getNextScanChannelFromEnabledChannels(currentScanChannel);
////
////                        if (nextId == 0L) {
////                            // nothing to scan at this time.
////                            // set the ScannerControlRecord to show that no tuners are enabled
////                            if (scannerStatus.equals(ScannerControl.SLIDESHOW_RUNNING.name())) {
////                                Thread.sleep(1000);
////                                continue;
////                            }
////                            sr.setStatus(ScannerControl.EMPTY.name());
////                            sr.setChannelId(0L);
////                            sr.setScanChannel(0);
////                        } else {
////                            // update the scanner record with the next channel to scan.
////                            sr.updateWithTunerConfiguration(channelService.get(nextId));
////                            // set the status to enabled and active
////                            sr.setStatus(ScannerControl.ENABLED_ACTIVE.name());
////                        }
////                        // save the ScannerControlRecord
////                        scannerService.save(sr);
////                        // get the active channel record
////                        tunerActiveId = nextId;
////
////                    }
////
////                    if ((scanDefinitionChanged || newStartup) && tunerActiveId > 0L) {
////                        // default values.
////                        String newFreq = "50000";
////                        String newBw = "2000";
////                        String newSymb = "2000";
////                        String newAudio = "hdmi";
////                        String newChan = "1";
////
////                        // if we are here then ScannerControlRecord is valid.
////                        newFreq = String.valueOf(sr.getFrequency());
////                        newBw = String.valueOf(sr.getBandwidth());
////                        newSymb = String.valueOf(sr.getSymbolRate());
////                        newChan = String.valueOf(sr.getScanChannel());
////
////                        // update the parameters config file - let's make it a JSON file
////                        // or, we can pass in the parameters we want to use...
////                        String pathName = "/usr/local/apps/versatune/data/dvb-t_config.txt";
////                        if (Files.exists(Paths.get(pathName))) {
////
////                            StringBuilder sb = new StringBuilder();
////                            sb.append("freq=").append(newFreq).append(System.lineSeparator());
////                            sb.append("bw=").append(newBw).append(System.lineSeparator());
////                            sb.append("symb=").append(newSymb).append(System.lineSeparator());
////                            sb.append("audio=").append(newAudio).append(System.lineSeparator());
////                            sb.append("chan=").append(newChan).append(System.lineSeparator());
////
////                            // write out new content
////                            modifyFile(pathName, sb.toString());
////
////                            ////////////////////////////////////////
////                        }
////                    } else {
////                        // no enabled channel so go probably should start up the SlideShow here.
////                        if (srRef.isPresent()) {
////                            if (!srRef.get().getStatus().equals(ScannerControl.SLIDESHOW_RUNNING.name())) {
////                                int result = executeStartupForSlideShow(appPath);
////                                if (result == 900) {
////                                    if (srRef.isPresent()) {
////                                        srRef.get().setStatus(ScannerControl.SLIDESHOW_RUNNING.name());
////                                        scannerService.save(srRef.get());
////                                    }
////                                }
////                            }
////                        }
////                        Thread.sleep(2000); // sleep 2 seconds.
////
////                        continue;
////                    }
////
////                    // get the parameters from the configuration file that we need.
////                    UserConfigData userConfigData = getUserConfigData(configPath);
////                    scan(userConfigData, overlayPath, fifoPath, blankTsPath, appPath);
////                } catch (Exception exception) {
////                    logger.error("got an exception while in scanning loop.", exception);
////                    continue;
////                }
////            }
//
//            try {
//                // get the parameters from the configuration file that we need.
//                UserConfigData userConfigData = getUserConfigData(configPath);
//                String audioDevice = getAudioDeviceForScan(getUserConfigData(configPath));
//                doDvbtTunerV2(audioDevice, userConfigData, overlayPath, fifoPath, blankTsPath, appPath);
//            } catch (Exception exception) {
//                logger.error("got an exception while in scanning loop.", exception);
//            }
//            try {
//                Thread.sleep(500);
//                continue;
//            } catch (InterruptedException e) {
//                logger.error("Got interrupted during sleep. Exiting.");
//                break;
//            }
//        }
//    }
//
//
//    private void modifyFile(String filePath, String newString) {
//        File fileToBeModified = new File(filePath);
//        StringBuilder oldContent = new StringBuilder();
//        try (BufferedReader reader = new BufferedReader(new FileReader(fileToBeModified))) {
//            String line = reader.readLine();
//            while (line != null) {
//                oldContent.append(line).append(System.lineSeparator());
//                line = reader.readLine();
//            }
//            String content = oldContent.toString();
//            String newContent = content.replaceAll(content, newString);
//            try (FileWriter writer = new FileWriter(fileToBeModified)) {
//                writer.write(newContent);
//            }
//        } catch (IOException e) {
//            logger.error("Could not update the config file.", e);
//        }
//    }
//
//    private UserConfigData getUserConfigData(String filePath) {
//        UserConfigData userData = new UserConfigData();
//        File configFile = new File(filePath);
//        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
//            String line = reader.readLine();
//            List<String> lines = new ArrayList<>();
//            while (line != null) {
//                lines.add(line);
//                line = reader.readLine();
//            }
//            if (lines.size() > 0) {
//                for (String s : lines) {
//                    s = s.toLowerCase();
//                    if (s.startsWith("freq") || s.startsWith("frequency")) {
//                        String[] parts = s.split("=", -1);
//                        if (parts.length >= 2) {
//                            userData.setFrequency(parts[1]);
//                        }
//                    } else if (s.startsWith("bw") || s.startsWith("bandwidth")) {
//                        String[] parts = s.split("=", -1);
//                        if (parts.length >= 2) {
//                            userData.setBandwidth(parts[1]);
//                        }
//                    } else if (s.startsWith("symb") || s.startsWith("symbolrate")) {
//                        String[] parts = s.split("=", -1);
//                        if (parts.length >= 2) {
//                            userData.setSymbolRate(parts[1]);
//                        }
//                    } else if (s.startsWith("aud") || s.startsWith("audio")) {
//                        String[] parts = s.split("=", -1);
//                        if (parts.length >= 2) {
//                            userData.setAudioOut(parts[1]);
//                        }
//                    } else if (s.startsWith("chan") || s.startsWith("channel")) {
//                        String[] parts = s.split("=", -1);
//                        if (parts.length >= 2) {
//                            userData.setChannel(parts[1]);
//                        }
//                    }
//                }
//            }
//        } catch (IOException e) {
//            logger.error("Could read the dvb-t_config.txt file.", e);
//        }
//        return userData;
//    }
//
//    private int getNextChannelRecord(List<TunerConfigRecord> enabledChannels, int nextChannelTarget) {
//        if (enabledChannels.size() > 0) {
//            // find highest numbered channel
//            int hiChan = Integer.MIN_VALUE;
//            int loChan = Integer.MAX_VALUE;
//            for (TunerConfigRecord r : enabledChannels) {
//                if (r.getChannel() > hiChan)
//                    hiChan = r.getChannel();
//                if (r.getChannel() < loChan)
//                    loChan = r.getChannel();
//            }
//            // now figure out where we are
//            if (nextChannelTarget > hiChan) {
//                nextChannelTarget = loChan;
//            }
//            return nextChannelTarget;
//        } else {
//            // no enabled channels so cannot scan.
//            return 0;
//        }
//
//    }
//
//    /*
//            This looks up data in the userConfigFile to create the audioDevice that
//            will be used in VLC calls
//     */
//    private String getAudioDeviceForScan(UserConfigData userConfigData){
//        // default audio device:
//        String audioDevice = "hw:CARD=b1,DEV=0";
//        try {
//            // try writing a command and getting the response back
//            Runtime runtime = Runtime.getRuntime();
//            String commandToRun = "aplay -l | grep bcm2835 | grep \"Headphones\" | perl -lane 'print substr($F[1],0,1)'";
//            commandToRun = "aplay -L";
//            Process process = runtime.exec(commandToRun);
//            InputStream is = process.getInputStream();
//            InputStreamReader isr = new InputStreamReader(is);
//            BufferedReader br = new BufferedReader(isr);
//            String inputLine;
//            List<String> lines = new ArrayList<>();
//            while ((inputLine = br.readLine()) != null) {
//                lines.add(inputLine);
//                logger.info("Response to aplay: [" + inputLine + "]");
//            }
//
//            // create list of AudioDeviceSpec objects from the output
//            List<AudioDeviceSpec> audioSpecs = new ArrayList<>();
//            AudioDeviceSpec spec = null;
//            for (String s : lines) {
//                if (!s.startsWith(" ")) {
//                    if (null != spec) {
//                        audioSpecs.add(spec);
//                    }
//                    spec = new AudioDeviceSpec();
//                    spec.setDeviceType(s);
//                } else {
//                    if (spec.getDeviceInfo1().isEmpty())
//                        spec.setDeviceInfo1(s);
//                    else
//                        spec.setDeviceInfo2(s);
//                }
//            }
//
//            // I believe we only need the hw:CARD types, so I'll filter those out of the list.
//            // now based on what is in the config file for audio we can pick one or the other of the hw:CARD types.
//            List<AudioDeviceSpec> hwCards = audioSpecs.stream()
//                    .filter(p -> p.getDeviceType().toLowerCase().contains("hw:card"))
//                    .collect(Collectors.toList());
//
//            if (userConfigData.getAudioOut().equals("rpi")) {
//                // we want to use headphones, so look for card headphones
//                Optional<AudioDeviceSpec> aoutRef = hwCards.stream().filter(p -> p.getDeviceType().contains("Headphones"))
//                        .findFirst();
//                if (aoutRef.isPresent()) {
//                    audioDevice = aoutRef.get().getDeviceType();
//                }
//            }
//        }  catch (IOException e) {
//            logger.error("Could not get audio device from configuration. Using default. ",e);
//        }
//        return audioDevice;
//    }
//
////    private void scan(UserConfigData userConfigData, String overlayPath, String fifoPath, String blankTsPath, String appPath) {
////        try {
////            // try writing a command and getting the response back
////            Runtime runtime = Runtime.getRuntime();
////            String commandToRun = "aplay -l | grep bcm2835 | grep \"Headphones\" | perl -lane 'print substr($F[1],0,1)'";
////            commandToRun = "aplay -L";
////            Process process = runtime.exec(commandToRun);
////            InputStream is = process.getInputStream();
////            InputStreamReader isr = new InputStreamReader(is);
////            BufferedReader br = new BufferedReader(isr);
////            String inputLine;
////            List<String> lines = new ArrayList<>();
////            while ((inputLine = br.readLine()) != null) {
////                lines.add(inputLine);
////                logger.info("Response to aplay: [" + inputLine + "]");
////            }
////
////            // create list of AudioDeviceSpec objects from the output
////            List<AudioDeviceSpec> audioSpecs = new ArrayList<>();
////            AudioDeviceSpec spec = null;
////            for (String s : lines) {
////                if (!s.startsWith(" ")) {
////                    if (null != spec) {
////                        audioSpecs.add(spec);
////                    }
////                    spec = new AudioDeviceSpec();
////                    spec.setDeviceType(s);
////                } else {
////                    if (spec.getDeviceInfo1().isEmpty())
////                        spec.setDeviceInfo1(s);
////                    else
////                        spec.setDeviceInfo2(s);
////                }
////            }
////
////            // I believe we only need the hw:CARD types, so I'll filter those out of the list.
////            // now based on what is in the config file for audio we can pick one or the other of the hw:CARD types.
////            List<AudioDeviceSpec> hwCards = audioSpecs.stream()
////                    .filter(p -> p.getDeviceType().toLowerCase().contains("hw:card"))
////                    .collect(Collectors.toList());
////            String audioDevice = "hw:CARD=b1,DEV=0";
////            if (userConfigData.getAudioOut().equals("rpi")) {
////                // we want to use headphones, so look for card headphones
////                Optional<AudioDeviceSpec> aoutRef = hwCards.stream().filter(p -> p.getDeviceType().contains("Headphones"))
////                        .findFirst();
////                if (aoutRef.isPresent()) {
////                    audioDevice = aoutRef.get().getDeviceType();
////                }
////            }
////
////            doDvbtTuner(audioDevice, userConfigData, overlayPath, fifoPath, blankTsPath, appPath);
////
////
////            Thread.sleep(1000L);
////        } catch (InterruptedException e) {
////            throw new RuntimeException(e);
////        } catch (IOException e) {
////            throw new RuntimeException(e);
////        }
////    }
//
//
//
//    /*
//             Use CompletableFuture to get our data from the fifo stream
//    */
//    public List<String> getLinesFromFifo(InputStream is) throws ExecutionException, InterruptedException, TimeoutException {
//
//        ExecutorService executor = Executors.newFixedThreadPool(4);
//
//        Supplier <List<String>> linesFetcher2 = () -> {
//            ByteArrayOutputStream res = new ByteArrayOutputStream();
//
//            List<String> resultLines = new ArrayList<>();
//            byte[] bytes = new byte[8192]; // so we can buffer a number of lines...
//            int numRead = 0;
//
//            try{
//                while ((numRead = is.read(bytes, 0, bytes.length)) >= 0) {
//                    res.write(bytes, 0, numRead);
//                    break;
//                };
//            }catch(Exception e){
//                logger.error("StreamReader error. ",e);
//            }
//            if (numRead > 0){
//                try {
//                    // we have an array of bytes in
//                    byte[] resultBytes = res.toByteArray();
//                    // now we need to convert this to Array of Strings
//                    String inputString = new String(resultBytes, StandardCharsets.UTF_8);
//                    resultLines = Stream.of(inputString.split("\n", -1))
//                            .collect(Collectors.toList());
//                }catch(Exception e){
//                    logger.error("Could not convert bytes to lines. ", e);
//                }
//
//            }
//            return resultLines;
//        };
//
//        CompletableFuture<List<String>> cf1 = CompletableFuture.supplyAsync(linesFetcher2, executor);
//        List<String> myLines = cf1.get(15000L,TimeUnit.MILLISECONDS);
//        executor.shutdown();
//        return myLines;
//
//    }
//
//
//    /*
//            get reference to active Scanner Record
//            Will return Optional.isPresent if there is one, or Optional.empty if not.
//     */
//
//    //    private void doDvbtTuner(String audioDevice,
////                             UserConfigData userConfigData,
////                             String overlayPath,
////                             String fifoPath,
////                             String blankTsPath,
////                             String appPath) {
////        logger.info("Running New DvbtWorker");
////        boolean tunerIsLocked = false;
////        boolean tunerStreamActive = false;
////        boolean displayWanted = true;       // we want to display info on screen when VLC not running
////
////        List<String> fifoLines = new ArrayList<>();
////
////        FileInputStream fio = null;
////        try {
////            // need another while loop around this that determines if this tuner should be running
////
////            // this starts up VLC and Combituner for DVBT
////            String frequency = userConfigData.getFrequency();
////            String symbolRate = userConfigData.getSymbolRate();
////            String bandwidth = userConfigData.getBandwidth();
////            int success = executeStartUpForCombituner(frequency,
////                    symbolRate,
////                    bandwidth,
////                    audioDevice,
////                    overlayPath,
////                    fifoPath,
////                    blankTsPath,
////                    appPath);
////            logger.info("Combituner Startup Script returned " + success);
////
////            // if scanner status is PENDING then we want display
////            displayWanted = true;
////
////            // let us try to use JNA here to access the std c library.
////
////            fio = new FileInputStream(fifoPath);
////
////            logger.trace("Path to the fifo is: " + fifoPath);
////
////            // preparing here to see if anything coming in on this tuner
////            int PollCount = 0;
////
////            StringBuffer line = new StringBuffer();
////
////            tunerStreamActive = false;
////            int maxZeroCount = 10;
////            int currentZeroCount = 0;
////            long unlockedCount = 0;
////            long searchFailedCount = 0;
////            long maxSearchFailedCount = 1;
////
////            while(true){
////                // Get Scanner Control Record
////
////                String x = tunerScanV2(
////                        fifoLines,
////                        audioDevice,
////                        userConfigData,
////                        overlayPath,
////                        fifoPath,
////                        blankTsPath,
////                        unlockedCount,
////                        searchFailedCount,
////                        maxSearchFailedCount,
////                        currentZeroCount,
////                        maxZeroCount,
////                        appPath);
////            }
//////            while (true) {
//////                Optional<TunerConfigRecord> tcrRef;
//////                Optional<ScannerControlRecord> scannerRef = scannerService.getActiveScanChannel();
//////                if (scannerService.getScannerControlStatus().equals(ScannerControl.EMPTY.name())||
//////                    scannerService.getScannerControlStatus().equals(ScannerControl.ENABLED_SEARCH_FAILED.name()) ||
//////                        scannerService.getScanControllerStatus().equals(ScannerControl.ENABLED_UNLOCKED.name()) ||
//////                        scannerService.getScanControllerStatus().equals(ScannerControl.ENABLED_NODATA.name())
//////                ){
//////                    break;
//////                }
//////
//////                fifoLines.clear();
//////                try {
//////                    fifoLines = getLinesFromFifo(fio);
//////                }catch(ExecutionException ee){
//////                    logger.error("CompleteableFuture returned Execution Error. ", ee);
//////                }catch(InterruptedException ie) {
//////                    logger.error("CompletableFuture returne InterruptedException ie. ", ie);
//////                }catch(TimeoutException to) {
//////                    logger.error("CompletableFuture returned Timeout.");
//////                }
//////
//////                int num = fifoLines.size();
//////
//////                if (num < 0) {
//////                    Thread.sleep(500);
//////                    PollCount++;
//////                    if (logger.isTraceEnabled()) logger.trace("PollCount is: " + PollCount);
//////                    if (PollCount > 120) {
//////                        logger.info("60 seconds since we saw any data. Exiting.");
//////                        PollCount = 0;
//////                        break;
//////                    }
//////                } else if (num == 0) {
//////                    if (currentZeroCount++ > maxZeroCount) {
////////                      let's disable this channel and break
////////                      no data for a while, so lets make this channel
//////                       // tcrRef = scannerService.getCurrentActiveEnabledTunerRecord();
//////                        scannerRef = scannerService.getActiveScanChannel();
//////                        if (scannerRef.isPresent()) {
//////                            // change status to enabled but not running...
//////                            scannerRef.get().setStatus(ScannerControl.ENABLED_NODATA.name());
//////                            scannerService.save(scannerRef.get());
//////                        }
//////                        logger.info("No Data for " + maxZeroCount + " counts, so breaking.");
//////                        // reset the currentZeroCount
//////                        currentZeroCount = 0;
//////                        break;
//////                    }
//////                } else {
//////                    currentZeroCount = 0;
//////                    List<String> skipList = Arrays.asList("SSI", "SQI", "SNR", "PER");
//////                    try {
//////
//////                        boolean unlockedFound = false;
//////                        // this flag will be true only if the last line in the current queue was locked.
//////                        for (String s : fifoLines) {
//////                            line = new StringBuffer(s);
//////                            if (!line.toString().isEmpty()) {
//////                                if (line.toString().toLowerCase().startsWith("unlocked")) {
//////                                    if (unlockedCount == 0) {
//////                                        logger.info(line.toString());
//////                                    }
//////                                    unlockedCount++;
//////                                    unlockedFound = true;
//////                                    // can't do anything so just continue
//////                                    if (unlockedCount > 3){
//////                                        // get out because we cannot do anything else as long as we
//////                                        // are unlocked.
//////                                        // let's get the active channel
//////                                        long activeChannelId = scannerService.getTunerIdFromActiveScanner();
//////                                        // if no active channel then just break out
//////                                        if (activeChannelId == 0) {
//////                                            // shutdown vlc for tuner
//////                                            break;
//////                                        }
//////                                        TunerConfigRecord tcr = channelService.get(activeChannelId);
//////                                        long id = tcr.getId();
//////
//////                                        // we need to tell the scannerService that our active channel is no longer
//////                                        // active
//////                                        Optional<ScannerControlRecord> scrRef = scannerService.getOrCreateScannerControlRecord();
//////                                        if ( scrRef.isPresent()){
//////                                            if ( scrRef.get().getChannelId() == id){
//////                                            scrRef.get().setChannelId(0);
//////                                            scrRef.get().setStatus(ScannerControl.EMPTY.name());
//////                                            scannerService.save(scrRef.get());
//////                                            }
//////                                        }
//////                                        // shutdown vlc for tumer
//////                                        break;
//////                                    }
//////
//////                                    continue;
//////                                } else {
//////                                    if (line.toString().toLowerCase().startsWith("tuner locked") ||
//////                                        line.toString().toLowerCase().startsWith("locked")){
//////                                        unlockedFound=false;
//////                                        tunerIsLocked = true;
//////                                    }
//////                                    if (!(line.toString().length() > 3 && skipList.contains(line.toString().substring(0, 3)))) {
//////                                        logger.info(line.toString());
//////                                    }
//////                                }
//////                            }
//////                            // we don't want to check for locked here because we need to go through the processResponse
//////                            // with the last item that came through. If we had a whole series of unlocked it will be the last
//////                            // and if anything else come in at the end of the list of lines then we'll process it.
//////                            if (tunerIsLocked)
//////                                displayWanted = false;
//////                            int responseStatus = processResponse(line.toString(), displayWanted, overlayPath);
//////                            // process this request.
//////
//////                            if (responseStatus == TunerStatus.SSI_FOUND.ordinal() ||
//////                                    responseStatus == TunerStatus.PER_FOUND.ordinal() ||
//////                                    responseStatus == TunerStatus.SQI_FOUND.ordinal() ||
//////                                    responseStatus == TunerStatus.SNR_FOUND.ordinal() ||
//////                                    responseStatus == TunerStatus.MOD_FOUND.ordinal()
//////                            ) {
//////                                if ( !unlockedFound){
//////                                    if (!tunerStreamActive){
//////                                        int result = executeStartupForTunerVlc(appPath, audioDevice);
//////                                        tunerStreamActive = true;
//////                                        tunerIsLocked = true;
//////                                    }
//////                                }
//////                            } else if (responseStatus == TunerStatus.SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH.ordinal()) {
//////                                // it's going to reset, so we could get out and go to next tuner
//////                                logger.info("Tuner returned SEARCH FAILED RESETTING FOR NEW SEARCH.");
//////                                searchFailedCount++;
//////                                if (searchFailedCount > maxSearchFailedCount) {
//////                                    long activeChannelId = scannerService.getTunerIdFromActiveScanner();
//////                                    if (activeChannelId == 0) {
//////                                        searchFailedCount = 0;
//////                                        tunerIsLocked = false;
//////                                        break;
//////                                    }
//////                                    TunerConfigRecord tcr = channelService.get(activeChannelId);
//////                                    if (tcr.isEnableChan()) {
//////                                        scannerRef = scannerService.getActiveScanChannel();
//////                                        if (scannerRef.isPresent()) {
//////                                            if (scannerRef.get().getStatus().equals(ScannerControl.ENABLED_ACTIVE.name())) {
//////                                                scannerRef.get().setStatus(ScannerControl.ENABLED_SEARCH_FAILED.name());
//////                                                scannerService.save(scannerRef.get());
//////                                            }
//////                                        }
//////                                    }
//////                                    searchFailedCount = 0;
//////                                    tunerIsLocked = false;
//////                                    break;
//////                                }
//////
//////                            } else if (responseStatus == TunerStatus.TUNER_UNLOCKED.ordinal()) {
//////                                // turn on the slide show for 10 seconds (old way)
//////                                // new way, we can return to the scan. We can mark this as unlocked. we might want to go again
//////                                // so that we remain unlocked for a few seconds. But if we stay unlocked, then exit the loop
//////
//////                                // lets allow a number of unlocks to go before deciding to get out...
//////                                if (unlockedFound && unlockedCount > 3) {
//////                                    // let's get the active channel
//////                                    long activeChannelId = scannerService.getTunerIdFromActiveScanner();
//////                                    // if no active channel then just break out
//////                                    if (activeChannelId == 0) {
//////
//////                                        tunerIsLocked = false;
//////                                        shutdownTunerVlc();
//////                                        break;
//////                                    }
//////                                    TunerConfigRecord tcr = channelService.get(activeChannelId);
//////                                    if (tcr.isEnableChan()) {
//////                                        tcr.setEnableChan(false);
//////                                        channelService.save(tcr);
//////                                    }
//////                                    // we need to tell the scannerService that our active channel is no longer
//////                                    // active
//////                                    tunerIsLocked = false;
//////                                    shutdownTunerVlc();
//////                                    break;
//////                                }
//////
//////                            } else if (responseStatus == TunerStatus.SIGNAL_LOCKED.ordinal()) {
//////                                // this is the signal we are looking for we can change the active scanner record if we need to.
//////                                unlockedCount = 0;
//////
//////                                long activeChannelId = scannerService.getTunerIdFromActiveScanner();
//////                                if (activeChannelId == 0) {
//////                                    logger.info("*** what the heck is going on...");
//////                                }
//////                                TunerConfigRecord tcr = channelService.get(activeChannelId);
//////                                //channelService.get(scannerService.getActiveChannelDbId());
//////                                scannerRef = scannerService.getActiveScanChannel();
//////                                if (scannerRef.isPresent()) {
//////                                    // change status to locked
//////                                    scannerRef.get().setStatus(ScannerControl.ENABLED_ACTIVE.name());
//////                                    // connect VLC to the tuner and then change status to ENABLED_ACTIVE_VLC_CONNECTED
//////                                }
//////                                if (!tcr.isEnableChan()) {
//////                                    logger.info("Tuner exiting because current channel is not enabled anymore.");
//////                                    break; // this will cause us to exit and let the scanner find the next channel.
//////                                }
//////                                // we are locked and ready to go so start the VLC up
//////                                if (!tunerStreamActive){
//////                                    int result = executeStartupForTunerVlc(appPath, audioDevice);
//////                                    tunerStreamActive = true;
//////                                }
//////                            }
//////                        }
//////
//////                    } catch (Exception e) {
//////                        logger.error("Got exception. " + e.getMessage());
//////                    }
//////                }
//////            }
////
////        } catch (Exception e) {
////            logger.error("Error occured. " + e.getMessage());
////        } finally {
////            try {
////                logger.info("Closing the fifo in the finally branch.");
////            } catch (Exception e2) {
////                logger.error("Could not close fifo upon exception.");
////            }
////        }
////
////        logger.info("we are done.");
////        // close the fifo path
////        try{
////            fio.close();
////        }catch (IOException e) {
////            logger.error("Exception when trying to close the input stream we are using.", e);
////        }
////
////        if (!tunerIsLocked){
//// //           shutdownTunerVlc();
////        }
////    }
//    private void doDvbtTunerV2(String audioDevice,
//                               UserConfigData userConfigData,
//                               String overlayPath,
//                               String fifoPath,
//                               String blankTsPath,
//                               String appPath) {
//
//        logger.info("Running New DvbtWorker");
//        boolean tunerIsLocked = false;
//        boolean tunerStreamActive = false;
//        boolean displayWanted = true;       // we want to display info on screen when VLC not running
//
//        List<String> fifoLines = new ArrayList<>();
//
//        FileInputStream fio = null;
//
//        // Get Current Scanner State
//
//        // this should ALWAYS return a record
//        Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
//        if (srRef.isEmpty()){
//            throw new RuntimeException("Could not create a scanner control record. Exiting.");
//        }
//        // we need the Scanner control record.
//        ScannerControlRecord scr = srRef.get();
//
//        String status = scr.getStatus();
//        if ( status.equals(ScannerControl.SLIDESHOW_RUNNING)){
//            doProcessTunerDuringSlideShow();
//        }else if (status.startsWith("ENABLED")){
//            if ( status.equals(ScannerControl.ENABLED_PENDING)){
//                displayWanted = true;
//            }else if (status.equals(ScannerControl.ENABLED_UNLOCKED.name())){
//                displayWanted = true;
//            }else if (status.equals(ScannerControl.ENABLED_NODATA.name())){
//                displayWanted = true;
//            }else if (status.equals(ScannerControl.ENABLED_SEARCH_FAILED.name())){
//                displayWanted = true;
//            }else{
//                displayWanted = false;
//            }
//            long channelId = scr.getChannelId();
//            // this starts up VLC and Combituner for DVBT
//            String frequency = String.valueOf(scr.getFrequency());
//            String symbolRate = String.valueOf(scr.getSymbolRate());
//            String bandwidth = String.valueOf(scr.getBandwidth());
//
//            int success = executeStartUpForCombituner(frequency,
//                    symbolRate,
//                    bandwidth,
//                    audioDevice,
//                    overlayPath,
//                    fifoPath,
//                    blankTsPath,
//                    appPath);
//            logger.info("Combituner Startup Script returned " + success);
//            doProcessTunerForTuner(channelId, scr, fio, displayWanted, overlayPath);
//
//            // if state changed, close fio
//        }
//
//    }
//
//    /*
//            Combituner should be running now and fifo should be open
//     */
//    private void doProcessTunerForTuner(long channelId,
//                                        ScannerControlRecord scr,
//                                        FileInputStream fio,
//                                        boolean displayWanted,
//                                        String overlayPath){
//        try
//        {
//            // CombiTuner has been started. It should be running.
//            TunerScanStatus tunerScanStatus = new TunerScanStatus(
//                    0,
//                    10,
//                    0,
//                    10,
//                    0,
//                    1,
//                    0,
//                    3);
//            while(true){
//                String status = scr.getStatus();
//                String newStatus = doScan(channelId, scr, tunerScanStatus, displayWanted, overlayPath, fio);
//                if (!newStatus.equals(status)){
//                    scr.setStatus(newStatus);
//                    scannerService.save(scr);
//                    break;
//                }
//            }
//
//        }catch(Exception e){
//
//        }finally{
//            try{
//                fio.close();
//            }catch (IOException e) {
//                logger.error("Exception when trying to close the input stream we are using.", e);
//            }
//        }
//
//    }
//
//    private String doProcessTunerDuringSlideShow(){
//        // get list of enabled channels
//        // for each enabled channel
//        //    doScan() -> scan to see if anything comes out
//        //    if channel is locked
//        //        updated scan control and break
//        //    next channel
//        // if no channel found exit
//        return "";
//    }
//
//    /*
//
//            Returns a good scanner control record.
//     */
//    private ScannerControlRecord  getScannerControlRecord(){
//        Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
//        if (srRef.isEmpty()){
//            throw new RuntimeException("Could not create a scanner control record. Exiting.");
//        }
//        return srRef.get();
//    }
//
//    /*
//
//            Enter with scanner in some state.
//            We read combituner signals and if any new data that would change state, then
//            return new state.
//    */
//    private String doScan(long channelId,
//                          ScannerControlRecord scr,
//                          TunerScanStatus tunerScanStatus,
//                          boolean displayWanted,
//                          String overlayPath,
//                          FileInputStream fio){
//
//        Optional<ScannerControlRecord> scrRef = scannerService.getActiveScanChannel();
//        boolean tunerIsLocked = false;  // we don't really know, so we'll assume not locked.
//        if (scrRef.isPresent()){
//            if (scr != scrRef.get()){
//                logger.error("Something is wrong. Scanner Control Record changed.");
//            }
//        }
//        // Combituner should be running on some channel. It might be the
//        // default channel if slide show is running
//        List<String> fifoLines = new ArrayList<>();
//        fifoLines.clear();
//        try {
//            fifoLines = getLinesFromFifo(fio);
//        }catch(ExecutionException ee){
//            logger.error("CompleteableFuture returned Execution Error. ", ee);
//        }catch(InterruptedException ie) {
//            logger.error("CompletableFuture returned InterruptedException ie. ", ie);
//        }catch(TimeoutException to) {
//            logger.error("CompletableFuture returned Timeout.");
//        }
//
//        int num = fifoLines.size();
//        StringBuffer line = new StringBuffer();
//
//        if (num < 0) {
//
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                return ScannerControl.ENABLED_SLEEP_INTERRUPTED.name();
//            }
//            tunerScanStatus.setCurrentPollCount(tunerScanStatus.getCurrentPollCount()+1);
//            if (logger.isTraceEnabled()) logger.trace("PollCount is: " + tunerScanStatus.getCurrentPollCount());
//            if (tunerScanStatus.getCurrentPollCount() > tunerScanStatus.getMaxPollCount()) {
//                logger.info("Several seconds since we saw any data. Exiting.");
////                scr.setStatus(ScannerControl.ENABLED_READ_ERROR.name());
////                scannerService.save(scr);
//                return ScannerControl.ENABLED_READ_ERROR.name();
//            }
//        } else if (num == 0) {
//            tunerScanStatus.setCurrentZeroCount(tunerScanStatus.getCurrentZeroCount()+1);
//            if (tunerScanStatus.getCurrentZeroCount() > tunerScanStatus.getMaxZeroCount()) {
////                scr.setStatus(ScannerControl.ENABLED_NODATA.name());
////                scannerService.save(scr);
//                logger.info("No Data for " + tunerScanStatus.getMaxSearchFailedCount() + " counts, so exiting.");
//                return ScannerControl.ENABLED_NODATA.name();
//            }
//        } else {
//            // reset current 0 count
//            tunerScanStatus.setCurrentZeroCount(0);
//            List<String> skipList = Arrays.asList("SSI", "SQI", "SNR", "PER");
//            try {
//
//                boolean unlockedFound = false;
//                // this flag will be true only if the last line in the current queue was locked.
//                for (String s : fifoLines) {
//                    line = new StringBuffer(s);
//                    if (!line.toString().isEmpty()) {
//                        if (line.toString().toLowerCase().startsWith("unlocked")) {
//                            if (tunerScanStatus.getUnlockedCount() == 0) {
//                                // just print when we first go unlocked.
//                                logger.info(line.toString());
//                            }
//                            tunerScanStatus.setUnlockedCount(tunerScanStatus.getUnlockedCount()+1);
//                            unlockedFound = true;
//                            // can't do anything so just continue
//                            if (tunerScanStatus.getUnlockedCount() > tunerScanStatus.getMaxUnlockedCount()){
//                                // change status of the Scanner Control record to ENABLED_UNLOCKED
//                                scr = getScannerControlRecord();
//                                scr.setStatus(ScannerControl.ENABLED_UNLOCKED.name());
//                                scannerService.save(scr);
//                                return ScannerControl.ENABLED_UNLOCKED.name();
//                            }
//                            continue;
//                        } else {
//                            if (line.toString().toLowerCase().startsWith("tuner locked") ||
//                                    line.toString().toLowerCase().startsWith("locked")){
//                                unlockedFound = false;
//                                // check current status of the running channel to see if we changed state.
//                                if (!scr.getStatus().equals(ScannerControl.ENABLED_ACTIVE.name())){
//                                    scr.setStatus(ScannerControl.ENABLED_ACTIVE.name());
//                                    scannerService.save(scr);
//                                    return ScannerControl.ENABLED_ACTIVE.name();
//                                }
//                            }
//                            if (!(line.toString().length() > 3 && skipList.contains(line.toString().substring(0, 3)))) {
//                                logger.info(line.toString());
//                            }
//                        }
//                    }
//
//                    int responseStatus = processResponse(line.toString(), displayWanted, overlayPath);
//                    // process this request.
//
//                    if (responseStatus == TunerStatus.SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH.ordinal()) {
//                        // it's going to reset, so we could get out and go to next tuner
//                        logger.info("Tuner returned SEARCH FAILED RESETTING FOR NEW SEARCH.");
//                        tunerScanStatus.setCurrentSearchFailedCount(tunerScanStatus.getCurrentSearchFailedCount()+1);
//                        if (tunerScanStatus.getCurrentSearchFailedCount() > tunerScanStatus.getMaxSearchFailedCount()) {
////                            scr = getScannerControlRecord();
////                            scr.setStatus(ScannerControl.ENABLED_READ_ERROR.name());
////                            scannerService.save(scr);
//                            return ScannerControl.ENABLED_READ_ERROR.name();
//                        }
//
//                    } else if (responseStatus == TunerStatus.TUNER_UNLOCKED.ordinal()) {
//                        // turn on the slide show for 10 seconds (old way)
//                        // new way, we can return to the scan. We can mark this as unlocked. we might want to go again
//                        // so that we remain unlocked for a few seconds. But if we stay unlocked, then exit the loop
//
//                        // lets allow a number of unlocks to go before deciding to get out...
//                        tunerScanStatus.setUnlockedCount(tunerScanStatus.getUnlockedCount()+1);
//                        if (unlockedFound && tunerScanStatus.getUnlockedCount() > tunerScanStatus.getMaxUnlockedCount()) {
////                            scr = getScannerControlRecord();
////                            scr.setStatus(ScannerControl.ENABLED_UNLOCKED.name());
////                            scannerService.save(scr);
//                            return ScannerControl.ENABLED_READ_ERROR.name();
//                        }
//
//                    } else if (responseStatus == TunerStatus.SIGNAL_LOCKED.ordinal()) {
//                        // this is the signal we are looking for we can change the active scanner record if we need to.
//                        tunerScanStatus.setUnlockedCount(0);
//                        scr = getScannerControlRecord();
//                        if (! scr.getStatus().equals(ScannerControl.ENABLED_ACTIVE.name())){
////                            scr.setStatus(ScannerControl.ENABLED_ACTIVE.name());
////                            scannerService.save(scr);
//                            return (ScannerControl.ENABLED_ACTIVE.name());
//                        }
//
//                    }else{
//                        // maybe if we are in display mode send response to the screen
//                        return scr.getStatus();
//                    }
//                }
//
//            } catch (Exception e) {
//                logger.error("Got exception. " + e.getMessage());
//            }
//        }
//        return scr.getStatus();
//    }
//    private void shutdownTunerVlc(){
//        // kill vlc
//        // delete any running vlc
//        List<String> cmdList = new ArrayList<>();
//        cmdList.clear();
//        cmdList.add("killall");
//        cmdList.add("-9");
//        cmdList.add("vlc");
//        int result = runCommand(cmdList, true);
//        logger.info("Result is "+ result + " after killing VLC.");
//    }
//
//    /*
//            This will start up the combituner (which will shut down the slide show if it is running)
//            We might have to update a file here instead of passing the args. I think I'm going to look
//            at trying a few other things.
//     */
//    private int executeStartUpForCombituner(String frequency,
//                                            String symbolRate,
//                                            String bandwidth,
//                                            String audioDevice,
//                                            String overlayPath,
//                                            String fifoPath,
//                                            String blankTsPath,
//                                            String appPath) {
//        List<String> commands = new ArrayList<>();
//        // just build the command with its args and send it out.
//
//        ProcessBuilder processBuilder = new ProcessBuilder();
//
////        Map<String, String> environmentVariables = processBuilder.environment();
////        environmentVariables.forEach((key, value) -> logger.info(key + value));
//
//
//        long startTime = System.currentTimeMillis();
//        logger.trace("creating and starting the process.");
//
//        ////////////////////////////  BELOW - New on Tuesday Nov 23 2022 /////////////////////////////////
//
//        int result;
//        // executed each of the commands that were in the script but do them here with commands.
//        String runPath = new StringBuffer(appPath).append("/scripts").toString();
//        List<String> cmdList = new ArrayList<>();
//
//        cmdList.clear();
//        cmdList.add("id");
//        cmdList.add("-u");
//        cmdList.add("-n");
//        result = runCommand(cmdList, true);
//
//
//        cmdList.clear();;
//        cmdList.add("bash");
//        cmdList.add("-c");
//        cmdList.add("cd");
//        cmdList.add(runPath);
//        result = runCommand(cmdList, true);
//
//        cmdList.clear();
//        cmdList.add("bash");
//        cmdList.add("-c");
//        cmdList.add("pwd");
//        result = runCommand(cmdList, true);
//
//        cmdList.clear();
//        cmdList.add("rm");
//        cmdList.add(overlayPath);
//        result = runCommand(cmdList, true);
//
//        cmdList.clear();
//        cmdList.add("cp");
//        String blankOverlayPath = new StringBuffer(appPath).append("/data/blank_vlc_overlay.txt").toString();
//        cmdList.add(blankOverlayPath);
//        cmdList.add(overlayPath);
//        result = runCommand(cmdList, true);
//
//        cmdList.clear();
//        cmdList.add("killall");
//        cmdList.add("-9");
//        cmdList.add("CombiTunerExpress");
//        result = runCommand(cmdList, true);
//
//        // remove old fifo path - hopefully it was closed
//        logger.trace("Removing fifo before starting a new combituner instance");
//        cmdList.clear();
//        cmdList.add("rm");
//        cmdList.add(fifoPath);
//        result = runCommand(cmdList, true);
//
//        // make new fifo
//        cmdList.clear();
//        cmdList.add("mkfifo");
//        cmdList.add(fifoPath);
//        result = runCommand(cmdList, true);
//
//        cmdList.clear();
//        cmdList.add("id");
//        cmdList.add("-u");
//        cmdList.add("-n");
//        result = runCommand(cmdList, true);
//
//        // change directory to the scripts directory
//        cmdList.clear();
//        cmdList.add("bash");
//        cmdList.add("-c");
//        cmdList.add("cd");
//        cmdList.add(String.format("%s/scripts", appPath));
//        result = runCommand(cmdList, true);
//
//        // show working directory now
//        cmdList.clear();
//        cmdList.add("bash");
//        cmdList.add("-c");
//        cmdList.add("pwd");
//        result = runCommand(cmdList, true);
//
//        // Now try to start the CombiTuner from the combituner shell script.
//
//        cmdList.clear();
//        // full path to shell script
//        cmdList.add(String.format("%s/scripts/runCombiTunerExpress.sh",appPath));
//        cmdList.add("dvbt");
//        cmdList.add(frequency);
//        cmdList.add(bandwidth);
//        // full path to CombiTunerExpress
//        cmdList.add(String.format("%s/data/CombiTunerExpress", appPath));
//        // full path to fifo
//        cmdList.add(String.format("%s/data/knucker_status_fifo", appPath));
//
//        ProcessBuilder pb = new ProcessBuilder(cmdList);
//        try{
//            Process process = pb.start();
//            result = process.waitFor();
//        }catch(Exception e){
//            logger.error("Could not start CombiTunerExpress.", e);
//        }finally{
//            logger.info("CombiTunerExpress should now be running.");
//        }
//
//        cmdList.clear();
//        cmdList.add("sleep");
//        cmdList.add("0.1");
//        result = runCommand(cmdList, true);
//
//
//        long elapsed = System.currentTimeMillis() - startTime;
//        logger.info("Elapsed Time to start Combituner iw "+elapsed+ " milliseconds.");
//        return result;
//    }
//
//    ////////////////////////////  ABOVE - New on Tuesday Nov 23 2022 /////////////////////////////////
//
//    ////////////////////////////  BELOW - New on Wed Nov 30 2022 /////////////////////////////////////
//
//    /*
//        Run shell commands to start up the VLC for the tuner
//    */
//    private int executeStartupForTunerVlc(String appPath, String audioDevice){
//        int result = -1;
//        List<String> cmdList = new ArrayList<>();
//
//        cmdList.clear();
//        cmdList.add("bash");
//        cmdList.add("-c");
//        cmdList.add("cd");
//        cmdList.add(String.format("%s/scripts", appPath));
//        result = runCommand(cmdList, true);
//
//        // print the new working directory
//        cmdList.clear();
//        cmdList.add("bash");
//        cmdList.add("-c");
//        cmdList.add("pwd");
//        result = runCommand(cmdList, true);
//
//        // delete any running vlc
//        cmdList.clear();
//        cmdList.add("killall");
//        cmdList.add("-9");
//        cmdList.add("vlc");
//        result = runCommand(cmdList, true);
//
//        // sleep a little
//        cmdList.clear();
//        cmdList.add("sleep");
//        cmdList.add("0.1");
//        result = runCommand(cmdList, true);
//
//        // Create dummy marquee overlay file - clear any old ones
//        String blankOverlayPath = new StringBuffer(appPath).append("/data/blank_vlc_overlay.txt").toString();
//        String overlayPath = new StringBuffer(appPath).append("/data/vlc_overlay.txt").toString();
//        String blankTsPath = new StringBuffer(appPath).append("/data/blank.ts").toString();
//
//        // deleted existing overlayPath
//        cmdList.clear();
//        cmdList.add("rm");
//        cmdList.add(overlayPath);
//        result = runCommand(cmdList, true);
//
//        // set it to a blank line
//        cmdList.clear();
//        cmdList.add("cp");
//        cmdList.add(blankOverlayPath);
//        cmdList.add(overlayPath);
//        result = runCommand(cmdList, true);
//
//        // play short dummy file to prime VLC (not sure why we need to actually do this)
//        StringBuffer sb = new StringBuffer("su -c 'cvlc --codec h264_v4l2m2m --no-video-title-show --codec h264_v412m2m --quiet ");
//        sb.append("--sub-filter marq --marq-size 20 --marq-x 25 --marq-position=8 --marq-file ");
//        sb.append("\"").append(overlayPath).append("\"");
//        sb.append(" --gain 3 --alsa-audio-device ").append(audioDevice);
//        sb.append(" ").append(blankTsPath).append(" vlc://quit -L' pi &\n");
//        cmdList.add(sb.toString());
//        try{
//            executeCommandList(cmdList, true);
//            result = 901;
//            logger.info("Successfully ran short VLC Prime Script.");
//        }catch(Exception e){
//            logger.error("ERROR: could not execute the script to  start the slide show.", e);
//            result = -1;
//        }
//
//        // sleep a little
//        cmdList.clear();
//        cmdList.add("sleep");
//        cmdList.add("0.1");
//        result = runCommand(cmdList, true);
//
//        // now get ready to send the hdmi output to the VLC
//        cmdList.clear();
//        sb = new StringBuffer("su -c 'cvlc --codec h264_v4l2m2m --video-title-timeout=100 --codec h264_v412m2m --quiet ");
//        sb.append("--sub-filter marq --marq-size 20 --marq-x 25 --marq-position=8 --marq-file ");
//        sb.append("\"").append(overlayPath).append("\"");
//        sb.append(" --gain 3 --alsa-audio-device ").append(audioDevice);
//        sb.append(" udp://@127.0.0.1:1314 -L' pi &\n");
//        cmdList.add(sb.toString());
//        logger.info("** STARTING TUNER:");
//        logger.info("   "+sb.toString());
//        logger.info("** STARTING TUNER:");
//        try{
//            executeCommandList(cmdList, true);
//            result = 902;
//            logger.info("Successfully started VLC for Tuner.");
//        }catch(Exception e){
//            logger.error("ERROR: could not execute the script to  start the tuner VLC.", e);
//            result = -1;
//        }
//
//        return result;
//    }
//    ////////////////////////////  ABOVE - new on Wed Nov 30 2022  /////////////////////////////////
//
//    ////////////////////////////  BELOW - new on Monday Nov 28 2022  /////////////////////////////////
//
//    /*
//            Run shell commands to start up the slide show when we don't have a tuner running
//     */
//    private int executeStartupForSlideShow(String appPath){
//        List<String> cmdList = new ArrayList<>();
//        int result = 0;
//        // change directory to the images directory
//        cmdList.clear();
//        cmdList.add("bash");
//        cmdList.add("-c");
//        cmdList.add("cd");
//        cmdList.add(String.format("%s/images", appPath));
//        result = runCommand(cmdList, true);
//
//        // show working directory now
//        cmdList.clear();
//        cmdList.add("bash");
//        cmdList.add("-c");
//        cmdList.add("pwd");
//        result = runCommand(cmdList, true);
//
//        // delete any running vlc
//        cmdList.clear();
//        cmdList.add("killall");
//        cmdList.add("vlc");
//        result = runCommand(cmdList, true);
//
//        // sleep a bit
//        cmdList.clear();
//        cmdList.add("sleep");
//        cmdList.add("0.1");
//        result = runCommand(cmdList, true);
//
//        cmdList.clear();
//
//        StringBuffer sb = new StringBuffer("su -c 'cvlc ffmpeg  --codec h264_v4l2m2m --no-video-title-show ");
//        File[] files = new File(String.format("%s/images", appPath)).listFiles();
//        for(File file : files){
//            if(file.isFile()){
//                // check to make sure file is one of the allowed extensions
//                String extension = FilenameUtils.getExtension(file.getName());
//                if (allowedExtensions.contains(extension.toLowerCase())){
//                    sb.append(String.format("file://")).append(appPath).append("/images/").append(file.getName()).append(" ");
//                }
//            }
//        }
//        if (null == files ||  files.length < 1){
//            sb.append("file://").append(appPath).append("/data/MyGrandPianoInItsNewHome.jpeg");
//
//        }
//        // to get current hdmi screen resolution
//        //  cat /sys/class/graphics/fb0/virtual_size
//        // ours is 1920 x 1080
//        //sb.append(" -L' pi &\n");
//        sb.append(" -L' pi &\n");
//        cmdList.add(sb.toString());
//        //result = runCommand(cmdList, true);
//
//        try{
//            executeCommandList(cmdList, false);
//            result = 900;
//        }catch(Exception e){
//            logger.error("ERROR: could not execute the script to  start the slide show.", e.getMessage());
//            result = -1;
//        }
//
//        return result;
//    }
//
//    ////////////////////////////  ABOVE - new on Monday Nov 28 2022  /////////////////////////////////
//
//    ////////////////////////////  BELOW - This can or should be replaced /////////////////////////////
//    private void executeCommandList(List<String> commandList, boolean logit) throws IOException {
//        File tempScript = createTempScriptFromCommandList(commandList, logit);
//        logger.info("creating and starting the process to execute a commandlist.");
//
//        logger.info("Executing script: " + tempScript.toString());
//        try {
//            ProcessBuilder pb = new ProcessBuilder("bash", tempScript.toString());
//            pb.inheritIO();
//            Process process = pb.start();
//            logger.info("Process started... " + process.info());
//            int exitCode = process.waitFor();
//            if (exitCode != 0){
//                logger.info("*** Script did NOT run properly.");
//                logger.info("   ERRORCODE = exitCode");
//            }
//        }catch (InterruptedException ie){
//            logger.error("Caught interrupted exception. Not sure what to do. " + ie.getMessage());
//        } finally {
//            tempScript.delete();
//        }
//
//    }
//
//    private File createTempScriptFromCommandList(List<String> commands, boolean logit) throws IOException {
//        logger.info("creating the temporary script file from command list.");
//        File tempScript = File.createTempFile("scriptX", null);
//        Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
//                tempScript));
//        PrintWriter printWriter = new PrintWriter(streamWriter);
//        if (logit)
//            logger.info("##########################################################################################");
//        for (String s : commands){
//            printWriter.println(s);
//            if(logit)
//                logger.info(s);
//        }
//        if (logit)
//            logger.info("##########################################################################################");
//        printWriter.close();
//        return tempScript;
//    }
//
//    ////////////////////////////  ABOVE - This can or should be replaced /////////////////////////////
//
//
//    /*
//
//
//     */
//
//    public int runCommand(List<String> commands, boolean destroy) {
//        ProcessBuilder processBuilder = new ProcessBuilder().command(commands);
//        int result = -1;
//        try {
//            Process process = processBuilder.start();
//
//            //read the output
//            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
//            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//            String output = null;
//            while ((output = bufferedReader.readLine()) != null) {
//                System.out.println(output);
//            }
//
//            //wait for the process to complete
//            result = process.waitFor();
//
//            //close the resources
//            bufferedReader.close();
//            // don't want to destroy process that was created in for the background
//            if (destroy)
//                process.destroy();
//
//        } catch (IOException | InterruptedException e) {
//            logger.error("Theres was an error. ", e);
//        }
//        return result;
//    }
//
//
//
//
//    /*
//            Need this to be a future and have the future return true when the stream is running
//            then we can set the stream active to true.
//     */
//    private void startTunerStream2() {
//        StartTunerStreamTask t = new StartTunerStreamTask();
//        executor.execute(t);
//    }
//
//
//    private void startSlideShow2() {
//        StartSlideShowTask t = new StartSlideShowTask();
//        executor.execute(t);
//    }
//
//    private int processResponse(String response, boolean displayWanted, String overlaytextPath) {
////        if (logger.isTraceEnabled()) logger.trace("processResponse: ENTERED.");
//        int status = -1;
//        String messageText = "";
//        if (response.equals("[GetFamilyId] Family ID:0x4955")) {
//            if (logger.isTraceEnabled()) logger.trace("Initializing Tuner, Please Wait.");
//            messageText = "Initializing Tuner, Please Wait.";
//            status = TunerStatus.INITIALIZING_TUNER.ordinal();
//        } else if (response.equals("[GetChipId] chip id:AVL6862")) {
//            if (logger.isTraceEnabled()) logger.trace("Found Knucker Tuner");
//            messageText = "Found Versatune.";
//            status = TunerStatus.TUNER_FOUND.ordinal();
//        } else if (response.equals("[AVL_Init] AVL_Initialize Failed!")) {
//            if (logger.isTraceEnabled()) logger.trace("Failed to initialize tuner. Change USB Cable.");
//            messageText = "Failed to iniialize tuner. Change USB Cable.";
//            status = TunerStatus.INITIALIZE_FAILED.ordinal();
//        } else if (response.equals("[AVL_Init] ok")) {
//            if (logger.isTraceEnabled()) logger.trace("Tuner Initialized.");
//            messageText = "Tuner Initialized.";
//            status = TunerStatus.TUNER_INITIALIZED.ordinal();
//        } else if (response.equals("Tuner not found")) {
//            if (logger.isTraceEnabled()) logger.trace("Please connect a Versatune Tuner");
//            messageText = "Please connect a Versatune Tuner.";
//            status = TunerStatus.NO_TUNER_FOUND.ordinal();
//        } else if (response.equals("locked")) {
//            if (logger.isTraceEnabled()) logger.trace("Tuner Locked");
//            messageText = "Signal locked";
//            status = TunerStatus.SIGNAL_LOCKED.ordinal();
//        } else if (response.contains("=== Freq")) {
//            String freq = response.substring(11);
//            if (logger.isTraceEnabled()) logger.trace("Tuner Frequency is: " + freq);
//            messageText = "Tuner Frequency is: " + freq;
//            displayWanted = true;
//            status = TunerStatus.NEW_FREQ_DATA.ordinal();
//        } else if (response.contains("=== Bandwidth")) {
//            String bandwidth = response.substring(17);
//            if (logger.isTraceEnabled()) logger.trace("Tuner Bandwidth is: " + bandwidth);
//            messageText = "Tuner Bandwidth is: " + bandwidth;
//            displayWanted = true;
//            status = TunerStatus.NEW_BW_DATA.ordinal();
//        } else if (response.contains("[AVL_ChannelScan_Tx] Freq")) {
//            String bandwidth = response.substring(17);
//            if (logger.isTraceEnabled()) logger.trace("Searching for signal");
//            messageText = "Searching for signal";
//            status = TunerStatus.SEARCHING_FOR_SIGNAL.ordinal();
//        } else if (response.contains("[DVBTx_Channel_ScanLock_Example] DVBTx channel scan is fail,Err.")) {
//            String bandwidth = response.substring(17);
//            if (logger.isTraceEnabled()) logger.trace("Search failed, resetting for another search");
//            messageText = "Search failed, resetting for another search";
//            status = TunerStatus.SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH.ordinal();
//        } else if (response.contains("[AVL_LockChannel_T] Freq ")) {
//            if (logger.isTraceEnabled()) logger.trace("Signal detected, attempting to lock");
//            messageText = "Signal detected, attempting to lock";
//            displayWanted = true;
//            status = TunerStatus.SIGNAL_DETECTED_ATTEMPTING_LOCK.ordinal();
//        } else if (response.contains("Unlocked")) {
//            if (logger.isTraceEnabled()) logger.trace("Tuner Unlocked");
//            messageText = "Tuner Unlocked";
//            status = TunerStatus.TUNER_UNLOCKED.ordinal();
//        } else if (response.contains("[AVL_LockChannel_T] Freq is") && response.contains("MHz, Bandwidth is")) {
//            messageText = response;
//            displayWanted = true;
//            status = TunerStatus.TUNER_ONLINE.ordinal();
//        } else if (response.contains("[AVL_ChannelScan_Tx]")){
//            messageText = response;
//            displayWanted = true;
//            status = TunerStatus.NEW_FREQ_DATA.ordinal();
//        }else if (response.contains("MOD  :")) {
//            messageText = response;
//            displayWanted = true;
//            status = TunerStatus.MOD_FOUND.ordinal();
//        } else if (response.contains("FFT  :")) {
//            messageText = response;
//            displayWanted = true;
//            status = TunerStatus.MOD_FOUND.ordinal();
//        } else if (response.contains("Const:")) {
//            messageText = response;
//            displayWanted = true;
//            status = TunerStatus.MOD_FOUND.ordinal();
//        } else if (response.contains("FEC  :")) {
//            messageText = response;
//            status = TunerStatus.FEC_FOUND.ordinal();
//        } else if (response.contains("SSI is")) {
//            messageText = response;
//            status = TunerStatus.SSI_FOUND.ordinal();
//        } else if (response.contains("SQI is")) {
//            messageText = response;
//            status = TunerStatus.SQI_FOUND.ordinal();
//        } else if (response.contains("SNR is")) {
//            messageText = response;
//            status = TunerStatus.SNR_FOUND.ordinal();
//        }
//        // we do not want to use the displayMessage as it is only happening when VLC is not in use
//
//        //displayWanted = false; // see if this is causing us delay
//        if (displayWanted && !messageText.isEmpty()) {
//            displayMessage.dsiplayMessageText(messageText);
//        }
//        if (response.length() > 3) {
//            String result = processTunerInputData(response);
//            if (result.length() > 3 && result.startsWith("-> ")) {
//                updateVlcOverlayText(result, overlaytextPath);
//            }
//        }
//        return status;
//    }
//
//    public void updateVlcOverlayText(String text, String overlaytextPath) {
//        int len = text.length();
//        if (logger.isTraceEnabled()) logger.trace("In updateVlcOverlayText with text: " + text);
//        if (text.startsWith("-> ")) {
//            String overlayText = text.substring(3);
//            try{
//                FileWriter writeObj = new FileWriter(overlaytextPath, false);
//                writeObj.write(overlayText);
//                writeObj.close();
//            }catch(IOException ioe){
//                logger.error("Failed to update the VLC overlay text file.", ioe);
//            }
//        }
//    }
//
//
//    private String processTunerInputData(String iModel) {
//
//        RcvrSignal signal = signalService.getSignalFromPool();
//        List<RcvrSignal> recs = signalService.listall();
//
//        String test = iModel.toUpperCase().substring(0, 3);
//
//        switch (test) {
//            case "SSI":
//                if (signal.getSsi().isEmpty() || signal.getSsi().isBlank()) {
//                    signal.setSsi(iModel);
//                }
//                break;
//            case "SQI":
//                signal.setSqi(iModel);
//                if (signal.getSqi().isEmpty() || signal.getSqi().isBlank()) {
//                    signal.setSqi(iModel);
//                }
//                break;
//            case "SNR":
//                signal.setSnr(iModel);
//                if (signal.getSnr().isEmpty() || signal.getSnr().isBlank()) {
//                    signal.setSnr(iModel);
//                }
//                break;
//            case "PER":
//                signal.setPer(iModel);
//                if (signal.getPer().isEmpty() || signal.getPer().isBlank()) {
//                    signal.setPer(iModel);
//                }
//                break;
//            default:
//        }
//        // now check if all 4 parts are present
//        if (signal.getSsi().isEmpty() || signal.getSsi().isBlank())
//            signal.setReady(false);
//        else if (signal.getSqi().isEmpty() || signal.getSqi().isBlank())
//            signal.setReady(false);
//        else if (signal.getSnr().isEmpty() || signal.getSnr().isBlank())
//            signal.setReady(false);
//        else if (signal.getPer().isEmpty() || signal.getPer().isBlank())
//            signal.setReady(false);
//        else
//            signal.setReady(true);
//
//        signalService.save(signal);
//
//        String result = "NO DATA";
//        if (signal.isReady()) {
//            result = "-> SSI=" + signal.getSsi().substring(7) + "  SQI=" + signal.getSqi().substring(7) + "  SNR=" + signal.getSnr().substring(7) + "  PER=" + signal.getPer().substring(7);
//            long currentTime = System.currentTimeMillis();
//            List<RcvrSignal> batch = new ArrayList<>();
//            for (RcvrSignal rs : recs) {
//                if (rs.getTimestamp() < currentTime - 30000l) {
//                    batch.add(rs);
//                }
//            }
//            if (batch.size() > 0) {
//                signalService.deleteBatch(batch);
//            }
//        }
//
//        return result;
//    }
//
//    /*
//            TunerScan V2
//
//    */
////    private String tunerScanV2(
////            List<String> fifoLines,
////            String audioDevice,
////            UserConfigData userConfigData,
////            String overlayPath,
////            String fifoPath,
////            String blankTsPath,
////            long unlockedCount,
////            long searchFailedCount,
////            long maxSearchFailedCount,
////            int currentZeroCount,
////            int maxZeroCount,
////            String appPath){
////        while (true) {
////
////            // get current Scanner Control Record
////            // It will let us know where we are in the scan process
////            Optional<ScannerControlRecord> srRef = scannerService.getOrCreateScannerControlRecord();
////            if (srRef.isEmpty()){
////                throw new RuntimeException("Could not create a scanner control record. Exiting.");
////            }
////            // we need the Scanner control record.
////            ScannerControlRecord scr = srRef.get();
////
////            fifoLines.clear();
////            try {
////                fifoLines = getLinesFromFifo(fio);
////            }catch(ExecutionException ee){
////                logger.error("CompleteableFuture returned Execution Error. ", ee);
////            }catch(InterruptedException ie) {
////                logger.error("CompletableFuture returne InterruptedException ie. ", ie);
////            }catch(TimeoutException to) {
////                logger.error("CompletableFuture returned Timeout.");
////            }
////
////            int num = fifoLines.size();
////
////            if (num < 0) {
////                Thread.sleep(500);
////                PollCount++;
////                if (logger.isTraceEnabled()) logger.trace("PollCount is: " + PollCount);
////                if (PollCount > 120) {
////                    logger.info("60 seconds since we saw any data. Exiting.");
////                    PollCount = 0;
////                    break;
////                }
////            } else if (num == 0) {
////                if (currentZeroCount++ > maxZeroCount) {
//////                      let's disable this channel and break
//////                      no data for a while, so lets make this channel
////                    // tcrRef = scannerService.getCurrentActiveEnabledTunerRecord();
////                    scannerRef = scannerService.getActiveScanChannel();
////                    if (scannerRef.isPresent()) {
////                        // change status to enabled but not running...
////                        scannerRef.get().setStatus(ScannerControl.ENABLED_NODATA.name());
////                        scannerService.save(scannerRef.get());
////                    }
////                    logger.info("No Data for " + maxZeroCount + " counts, so breaking.");
////                    // reset the currentZeroCount
////                    currentZeroCount = 0;
////                    break;
////                }
////            } else {
////                currentZeroCount = 0;
////                List<String> skipList = Arrays.asList("SSI", "SQI", "SNR", "PER");
////                try {
////
////                    boolean unlockedFound = false;
////                    // this flag will be true only if the last line in the current queue was locked.
////                    for (String s : fifoLines) {
////                        line = new StringBuffer(s);
////                        if (!line.toString().isEmpty()) {
////                            if (line.toString().toLowerCase().startsWith("unlocked")) {
////                                if (unlockedCount == 0) {
////                                    logger.info(line.toString());
////                                }
////                                unlockedCount++;
////                                unlockedFound = true;
////                                // can't do anything so just continue
////                                if (unlockedCount > 3){
////                                    // get out because we cannot do anything else as long as we
////                                    // are unlocked.
////                                    // let's get the active channel
////                                    long activeChannelId = scannerService.getTunerIdFromActiveScanner();
////                                    // if no active channel then just break out
////                                    if (activeChannelId == 0) {
////                                        // shutdown vlc for tuner
////                                        break;
////                                    }
////                                    TunerConfigRecord tcr = channelService.get(activeChannelId);
////                                    long id = tcr.getId();
////
////                                    // we need to tell the scannerService that our active channel is no longer
////                                    // active
////                                    Optional<ScannerControlRecord> scrRef = scannerService.getOrCreateScannerControlRecord();
////                                    if ( scrRef.isPresent()){
////                                        if ( scrRef.get().getChannelId() == id){
////                                            scrRef.get().setChannelId(0);
////                                            scrRef.get().setStatus(ScannerControl.EMPTY.name());
////                                            scannerService.save(scrRef.get());
////                                        }
////                                    }
////                                    // shutdown vlc for tumer
////                                    break;
////                                }
////
////                                continue;
////                            } else {
////                                if (line.toString().toLowerCase().startsWith("tuner locked") ||
////                                        line.toString().toLowerCase().startsWith("locked")){
////                                    unlockedFound=false;
////                                    tunerIsLocked = true;
////                                }
////                                if (!(line.toString().length() > 3 && skipList.contains(line.toString().substring(0, 3)))) {
////                                    logger.info(line.toString());
////                                }
////                            }
////                        }
////                        // we don't want to check for locked here because we need to go through the processResponse
////                        // with the last item that came through. If we had a whole series of unlocked it will be the last
////                        // and if anything else come in at the end of the list of lines then we'll process it.
////                        if (tunerIsLocked)
////                            displayWanted = false;
////                        int responseStatus = processResponse(line.toString(), displayWanted, overlayPath);
////                        // process this request.
////
////                        if (responseStatus == TunerStatus.SSI_FOUND.ordinal() ||
////                                responseStatus == TunerStatus.PER_FOUND.ordinal() ||
////                                responseStatus == TunerStatus.SQI_FOUND.ordinal() ||
////                                responseStatus == TunerStatus.SNR_FOUND.ordinal() ||
////                                responseStatus == TunerStatus.MOD_FOUND.ordinal()
////                        ) {
////                            if ( !unlockedFound){
////                                if (!tunerStreamActive){
////                                    int result = executeStartupForTunerVlc(appPath, audioDevice);
////                                    tunerStreamActive = true;
////                                    tunerIsLocked = true;
////                                }
////                            }
////                        } else if (responseStatus == TunerStatus.SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH.ordinal()) {
////                            // it's going to reset, so we could get out and go to next tuner
////                            logger.info("Tuner returned SEARCH FAILED RESETTING FOR NEW SEARCH.");
////                            searchFailedCount++;
////                            if (searchFailedCount > maxSearchFailedCount) {
////                                long activeChannelId = scannerService.getTunerIdFromActiveScanner();
////                                if (activeChannelId == 0) {
////                                    searchFailedCount = 0;
////                                    tunerIsLocked = false;
////                                    break;
////                                }
////                                TunerConfigRecord tcr = channelService.get(activeChannelId);
////                                if (tcr.isEnableChan()) {
////                                    scannerRef = scannerService.getActiveScanChannel();
////                                    if (scannerRef.isPresent()) {
////                                        if (scannerRef.get().getStatus().equals(ScannerControl.ENABLED_ACTIVE.name())) {
////                                            scannerRef.get().setStatus(ScannerControl.ENABLED_SEARCH_FAILED.name());
////                                            scannerService.save(scannerRef.get());
////                                        }
////                                    }
////                                }
////                                searchFailedCount = 0;
////                                tunerIsLocked = false;
////                                break;
////                            }
////
////                        } else if (responseStatus == TunerStatus.TUNER_UNLOCKED.ordinal()) {
////                            // turn on the slide show for 10 seconds (old way)
////                            // new way, we can return to the scan. We can mark this as unlocked. we might want to go again
////                            // so that we remain unlocked for a few seconds. But if we stay unlocked, then exit the loop
////
////                            // lets allow a number of unlocks to go before deciding to get out...
////                            if (unlockedFound && unlockedCount > 3) {
////                                // let's get the active channel
////                                long activeChannelId = scannerService.getTunerIdFromActiveScanner();
////                                // if no active channel then just break out
////                                if (activeChannelId == 0) {
////
////                                    tunerIsLocked = false;
////                                    shutdownTunerVlc();
////                                    break;
////                                }
////                                TunerConfigRecord tcr = channelService.get(activeChannelId);
////                                if (tcr.isEnableChan()) {
////                                    tcr.setEnableChan(false);
////                                    channelService.save(tcr);
////                                }
////                                // we need to tell the scannerService that our active channel is no longer
////                                // active
////                                tunerIsLocked = false;
////                                shutdownTunerVlc();
////                                break;
////                            }
////
////                        } else if (responseStatus == TunerStatus.SIGNAL_LOCKED.ordinal()) {
////                            // this is the signal we are looking for we can change the active scanner record if we need to.
////                            unlockedCount = 0;
////
////                            long activeChannelId = scannerService.getTunerIdFromActiveScanner();
////                            if (activeChannelId == 0) {
////                                logger.info("*** what the heck is going on...");
////                            }
////                            TunerConfigRecord tcr = channelService.get(activeChannelId);
////                            //channelService.get(scannerService.getActiveChannelDbId());
////                            scannerRef = scannerService.getActiveScanChannel();
////                            if (scannerRef.isPresent()) {
////                                // change status to locked
////                                scannerRef.get().setStatus(ScannerControl.ENABLED_ACTIVE.name());
////                                // connect VLC to the tuner and then change status to ENABLED_ACTIVE_VLC_CONNECTED
////                            }
////                            if (!tcr.isEnableChan()) {
////                                logger.info("Tuner exiting because current channel is not enabled anymore.");
////                                break; // this will cause us to exit and let the scanner find the next channel.
////                            }
////                            // we are locked and ready to go so start the VLC up
////                            if (!tunerStreamActive){
////                                int result = executeStartupForTunerVlc(appPath, audioDevice);
////                                tunerStreamActive = true;
////                            }
////                        }
////                    }
////
////                } catch (Exception e) {
////                    logger.error("Got exception. " + e.getMessage());
////                }
////            }
////        }
////    }
//}
//
////