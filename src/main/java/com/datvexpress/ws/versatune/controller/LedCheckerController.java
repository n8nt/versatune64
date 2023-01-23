package com.datvexpress.ws.versatune.controller;

import com.datvexpress.ws.versatune.pi4j.Pi4jMinimalBT;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class LedCheckerController {

    Logger logger = LoggerFactory.getLogger(getClass());
    private final Pi4jMinimalBT gpioService;
    public LedCheckerController(Pi4jMinimalBT gpipService){
        this.gpioService = gpipService;
    }


    @RequestMapping(value="/blinkLeds/{id}", method=RequestMethod.GET)
    public String runSlideShow(@PathVariable String id, @RequestHeader HttpHeaders headers, HttpServletRequest servlet){

        // loop for as many times as the id says
        if ( null != gpioService){
            DigitalOutput oLock = gpioService.getSignalLock();
            DigitalOutput chan1 = gpioService.getChannelBit1();
            DigitalOutput chan2 = gpioService.getChannelBit2();
            DigitalOutput chan4 = gpioService.getChannelBit4();

            String oLockID = oLock.getId();
            String chan1Id = chan1.getId();
            String chan2Id = chan2.getId();
            String chan4Id = chan4.getId();

            int loopCount = Integer.parseInt(id);

            while(loopCount-- > 0) {
                try {
                    if(oLock.equals(DigitalState.HIGH)){
                        oLock.low();
                        logger.info("LOCKSIGNAL OFF.");
                    }else{
                        oLock.high();
                        logger.info("LOCKSIGNAL ON.");
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("RATS. ", e);
                }

                try {
                    if (chan1.equals(DigitalState.HIGH)){
                        chan1.low();
                        logger.info("CHAN1 OFF.");
                    }else{
                        chan1.high();
                        logger.info("CHAN1 ON.");
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("RATS. ", e);
                }
                try {
                    if ( chan2.equals(DigitalState.HIGH)){
                        chan2.low();
                        logger.info("CHAN2 OFF.");
                    }else{
                        chan2.on();
                        logger.info("CHAN2 ON.");
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("RATS. ", e);
                }
                try {
                    if(chan4.equals(DigitalState.HIGH)){
                        chan4.low();
                        logger.info("CHAN4 OFF.");
                    }else{
                        chan4.high();
                        logger.info("CHAN4 ON.");
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("RATS. ", e);
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ie) {
                    logger.info("Sleep over. exiting.");
                    break;
                }

            }

        }
        return "Finished.";
    }
}


