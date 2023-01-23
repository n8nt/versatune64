package com.datvexpress.ws.versatune.controller;

import com.datvexpress.ws.versatune.config.VersatuneStartupConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

//import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class HelloController {

    private final VersatuneStartupConfig config;

    final private BuildProperties buildProperties;

    public HelloController(BuildProperties buildProperties,
                           VersatuneStartupConfig config){
        this.buildProperties = buildProperties;
        this.config = config;
    }

    @RequestMapping(value="/checkMe", method= RequestMethod.GET)
    public String checkMe(){
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date date = new Date();
        String currentDateTime = formatter.format(date);

        String sb = "Demo is running at " + currentDateTime + "</br>" + "Artifact:     " + buildProperties.getArtifact() + "</br" +
                "GroupId:      " + buildProperties.getGroup() + "</br>" +
                "Name:         " + buildProperties.getName() + "</br>" +
                "Version:      " + buildProperties.getVersion() + "</br>" +
                "Build Time:   " + buildProperties.getTime() + "</br>" +
                "VLC OVERLAY:  " + config.getVersatuneOverlayPath() + "</vr>";

        return sb;

    }

    @RequestMapping(value="/runSlideShow/{id}", method=RequestMethod.GET)
    public long runSlideShow(@PathVariable String id, @RequestHeader HttpHeaders headers, HttpServletRequest servlet){
       // I want to test a couple of process builder scripts to see if I can start the slide show and get it's PID so
       // I can stop it later. And same for the tuner.

        return 0L;
    }

    @RequestMapping(value="/runTuner/{id}", method= RequestMethod.GET)
    public long getActiveScannedChannel(@PathVariable String id, @RequestHeader HttpHeaders headers, HttpServletRequest servlet){

        return 0L;
    }

}
