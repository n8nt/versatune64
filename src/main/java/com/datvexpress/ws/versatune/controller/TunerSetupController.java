package com.datvexpress.ws.versatune.controller;

import com.datvexpress.ws.versatune.enums.FECoptions;
import com.datvexpress.ws.versatune.enums.InputDevices;
import com.datvexpress.ws.versatune.enums.ScannerControl;
import com.datvexpress.ws.versatune.model.ScannerControlRecord;
import com.datvexpress.ws.versatune.model.TunerConfigRecord;
import com.datvexpress.ws.versatune.service.ChannelService;
import com.datvexpress.ws.versatune.service.ScannerService;
import com.datvexpress.ws.versatune.utils.TunerConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class TunerSetupController {

    final private BuildProperties buildProperties;

    final private TunerConfiguration tunerConfig;
    final private ChannelService channelService;
    final private ScannerService scannerService;

    public TunerSetupController(BuildProperties buildProperties,
                                ChannelService channelService,
                                ScannerService scannerService,
                                TunerConfiguration tunerConfig){
        this.buildProperties = buildProperties;
        this.tunerConfig = tunerConfig;
        this.channelService = channelService;
        this.scannerService = scannerService;
    }

    @RequestMapping(value="/setup", method= RequestMethod.GET)
    public String setup(Model model, HttpServletRequest request, HttpServletResponse response){

        // initialize the database for now
        List<TunerConfigRecord> setupRecords = tunerConfig.initTunerSetup();

        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date date = new Date();
        String currentDateTime = formatter.format(date);

        String sb = "Demo is running at " + currentDateTime + "</br>" + "Artifact:     " + buildProperties.getArtifact() + "</br" +
                "GroupId:      " + buildProperties.getGroup() + "</br>" +
                "Name:         " + buildProperties.getName() + "</br>" +
                "Version:      " + buildProperties.getVersion() + "</br>" +
                "Build Time:   " + buildProperties.getTime() + "</br>";
        model.addAttribute("topText", sb);
        model.addAttribute("middleText", "Something for the Middle.");
        model.addAttribute("bottomText", "This can go at the bottom.");
        return "tunerSetupPage";

    }

    @RequestMapping(value = "/", method=RequestMethod.GET)
    public String viewHomePage(Model model, HttpServletRequest request, HttpServletResponse response){
        List<TunerConfigRecord> listChannels = channelService.listall();
        model.addAttribute("listChannels", listChannels);
        model.addAttribute("topText", "List of Tuner Channels");
        model.addAttribute("bottomText", "============ VERSATUNE ===============");
        model.addAttribute("middleText", "==== Table of Configured Channels ===");
        return "index";
    }

    @RequestMapping(value = "/new", method=RequestMethod.GET)
    public String showNewChannelPage(Model model, HttpServletRequest request, HttpServletResponse response) {
        TunerConfigRecord channel = new TunerConfigRecord();
        model.addAttribute("channel", channel);
        model.addAttribute("topText", "Create new Tuner Configuration Channel");
        model.addAttribute("bottomText", "============ VERSATUNE ===============");
        List<String> inputs = Arrays.asList(
                InputDevices.FTM4762_DVB_S_S2.name(),
                InputDevices.FTM4762_DVB_T_T2.name(),
                InputDevices.FTS3261_DVB_S_S2.name(),
                InputDevices.FTS3261_DVB_S_S2X.name(),
                InputDevices.FTS4334_DVB_S_S2A.name(),
                InputDevices.FTS4334_DVB_S_S2B.name(),
                InputDevices.USB.name());
        model.addAttribute("inputs",inputs);

        List<Boolean> flags = Arrays.asList(
                true,
                false
        );
        model.addAttribute("flags", flags);
        List<String> fecList = Arrays.asList(
                FECoptions.DVBS_1x2.name(),
                FECoptions.DVBS_2x3.name(),
                FECoptions.DVBS_3x4.name(),
                FECoptions.DVBS_5x6.name(),
                FECoptions.DVBS_6x7.name(),
                FECoptions.DVBS_7x8.name(),
                FECoptions.DVBT_1x2.name(),
                FECoptions.DVBT_2x3.name(),
                FECoptions.DVBT_3x4.name(),
                FECoptions.DVBT_5x6.name(),
                FECoptions.DVBT_7x8.name(),
                FECoptions.EMPTY.name()

        );
        model.addAttribute("fecList",fecList);

        List<Long> frequencies = new ArrayList<>();
        for ( long i = 50000; i < 2001000; i+= 1000){
            frequencies.add(i);
        }
        List<Long> symbolRates = new ArrayList<>();
        for ( long i = 1000; i < 6000; i+=1000){
            symbolRates.add(i);
        }
        List<Long> bandwidths = new ArrayList<>();
        for ( long i = 1000; i < 11000; i+=1000){
            bandwidths.add(i);
        }
        model.addAttribute("frequencies", frequencies);
        model.addAttribute("bandwidths", bandwidths);
        model.addAttribute("symbolRates", symbolRates);


        List<Long> pidas = new ArrayList<>();
        for ( long i = 1; i < 8192; i+=1){
            pidas.add(i);
        }
        List<Long> pidvs = new ArrayList<>();
        for ( long i = 1; i < 8192; i+=1){
            pidvs.add(i);
        }
        List<Long> tunerchannels = new ArrayList<>();
        for ( long i = 1; i < 9; i+=1){
            tunerchannels.add(i);
        }
        model.addAttribute("tunerchannels",tunerchannels);
        model.addAttribute("pidas", pidas);
        model.addAttribute("pidvs", pidvs);
        return "new_channel";
    }

    /*
            Update the channel
            We will need to also update the database with the latest status
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public String saveChannel(@ModelAttribute("channel") TunerConfigRecord channel) {
        channelService.save(channel);
        // check to see if we need to modify the channel status
        long id = channel.getId();
        // get currently active Scanner Control id
        long currentScanChannelDbId = scannerService.getTunerIdFromActiveScanner();
        int currentScanChan = scannerService.getScanChannelNumber();
        int currentSelectedChan = channel.getChannel();
        if (channel.isEnableChan()) {
            if (currentScanChan == currentSelectedChan) {
                if (currentScanChannelDbId != id) {
                    // cannot have multiple configs with same channel be enabled, so disable
                    // the old one. The easiest way to do this is to just reset all enabled records with same
                    // ScanChan and enabled to disabled then set the new one to enabled.
                    channelService.disableAllEnabledChannelsForSelectedScanChannel(currentScanChan);
                    // enable this channel so it is only one enabled.
                    channel.setEnableChan(true);
                    // Now save it.
                    channelService.save(channel);
                    // Now we need to update the ScannerControlRecord. We need to change the channelId of the
                    // owning TunerConfigRecord and change status to ENABLED_PENDING so that next scan it will
                    // update the Scanner Control with the new Tuner
                    scannerService.copyTunerConfigData(channel);
                    Optional<ScannerControlRecord> scrRef = scannerService.getFirstScannerRecord();
                    if (scrRef.isPresent()){
                        // update the status
                        scrRef.get().setStatus(ScannerControl.ENABLED_PENDING.name());
                    }
                }
            }
        }
        return "redirect:/";
    }

    @RequestMapping("/edit/{id}")
    public ModelAndView showEditChannelPage(@PathVariable(name = "id") int id) {
        ModelAndView mav = new ModelAndView("edit_channel");
        TunerConfigRecord channel = channelService.get(id);

        mav.addObject("channel", channel);

        List<String> inputs = Arrays.asList(
                InputDevices.FTM4762_DVB_S_S2.name(),
                InputDevices.FTM4762_DVB_T_T2.name(),
                InputDevices.FTS3261_DVB_S_S2.name(),
                InputDevices.FTS3261_DVB_S_S2X.name(),
                InputDevices.FTS4334_DVB_S_S2A.name(),
                InputDevices.FTS4334_DVB_S_S2B.name(),
                InputDevices.USB.name());
        mav.addObject("inputs", inputs);

        List<Boolean> flags = Arrays.asList(
                true,
                false
        );
        List<String> fecList = Arrays.asList(
                FECoptions.DVBS_1x2.name(),
                FECoptions.DVBS_2x3.name(),
                FECoptions.DVBS_3x4.name(),
                FECoptions.DVBS_5x6.name(),
                FECoptions.DVBS_6x7.name(),
                FECoptions.DVBS_7x8.name(),
                FECoptions.DVBT_1x2.name(),
                FECoptions.DVBT_2x3.name(),
                FECoptions.DVBT_3x4.name(),
                FECoptions.DVBT_5x6.name(),
                FECoptions.DVBT_7x8.name(),
                FECoptions.EMPTY.name()

        );

        List<Long> frequencies = new ArrayList<>();
        for ( long i = 50000; i < 2001000; i+= 1000){
            frequencies.add(i);
        }
        List<Long> symbolRates = new ArrayList<>();
        for ( long i = 1000; i < 6000; i+=1000){
            symbolRates.add(i);
        }
        List<Long> bandwidths = new ArrayList<>();
        for ( long i = 1000; i < 11000; i+=1000){
            bandwidths.add(i);
        }
        List<Long> pidas = new ArrayList<>();
        for ( long i = 1; i < 8192; i+=1){
            pidas.add(i);
        }
        List<Long> pidvs = new ArrayList<>();
        for ( long i = 1; i < 8192; i+=1){
            pidvs.add(i);
        }
        List<Long> tunerchannels = new ArrayList<>();
        for ( long i = 1; i < 9; i+=1){
            tunerchannels.add(i);
        }
        mav.addObject("pidas", pidas);
        mav.addObject("pidvs", pidvs);
        mav.addObject("tunerchannels", tunerchannels);

        mav.addObject("frequencies", frequencies);
        mav.addObject("bandwidths", bandwidths);
        mav.addObject("symbolRates", symbolRates);
        mav.addObject("fecList", fecList);
        mav.addObject("flags", flags);

        return mav;
    }

    @RequestMapping("/delete/{id}")
    public String deleteChannel(@PathVariable(name = "id") int id) {
        // update the scanner service if this channel was enabled and active
        Optional<ScannerControlRecord> scrRef = scannerService.getOrCreateScannerControlRecord();
        boolean okToResetScanner = false;
        if ( scrRef.isPresent()){
            if ( scrRef.get().getChannelId() == id){
                okToResetScanner = true;
            }
        }
        channelService.delete(id);
        if ( scrRef.isPresent()){
            scrRef.get().setChannelId(0);
            scrRef.get().setStatus(ScannerControl.EMPTY.name());
            scannerService.save(scrRef.get());
        }

        return "redirect:/";
    }

}
