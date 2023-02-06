package com.datvexpress.ws.versatune.controller;

import com.datvexpress.ws.versatune.config.VersatuneStartupConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

//import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class HelloController {

    Logger logger = LoggerFactory.getLogger(getClass());
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

    @RequestMapping(value="/status", method= RequestMethod.GET)
    public String status(){
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date date = new Date();
        String currentDateTime = formatter.format(date);

        List<String> cmdList = new ArrayList<>();

        cmdList.clear();
        cmdList.add("bash");
        cmdList.add("-c");
        cmdList.add("/usr/local/apps/versatune/scripts/checkCpuStatus.sh");
        String status = runCommand(cmdList, true);
        return status;
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

    public String runCommand(List<String> commands, boolean destroy) {
        ProcessBuilder processBuilder = new ProcessBuilder().command(commands);
        int result = -1;
        StringBuilder builder = new StringBuilder();

        try {
            Process process = processBuilder.start();

            //read the output
            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String output = null;
            while ((output = bufferedReader.readLine()) != null) {
                builder.append(output);
                builder.append(System.getProperty("line.separator"));
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
        if ( result == 0){
            return builder.toString();
        }else{
            return "Could not get status.\n";
        }
    }
}
