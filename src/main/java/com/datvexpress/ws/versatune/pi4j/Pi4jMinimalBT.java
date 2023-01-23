package com.datvexpress.ws.versatune.pi4j;


import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.util.Console;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * <p>This example fully describes the base usage of Pi4J by providing extensive comments in each step.</p>
 *
 * @author Frank Delporte (<a href="https://www.webtechie.be">https://www.webtechie.be</a>)
 * @version $Id: $Id
 */
@Component
@Service
@Getter
@Setter
public class Pi4jMinimalBT {

    Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * added by bob
     */



    // These are the pins we'll be using for the VERSATUNE tuner
    private static final int PIN_IN_FREEZE = 19;        // PIN 35 = BCM 19
    private static final int PIN_OUT_SIGNAL_LOCK = 13;   // pin 33 = BCM 13
    private static final int PIN_IN_SHUTDOWN = 14;       // pin 08 = BCM 14
    private static final int PIN_OUT_CHANNEL1 = 26;     // pin 37 = BCM 26
    private static final int PIN_OUT_CHANNEL2 = 06;     // PIN 31 = BCM 06
    private static final int PIN_OUT_CHANNEL4 = 12;     // PIN 32 = BCM 12

    private DigitalOutput channelBit1;
    private DigitalOutput channelBit2;
    private DigitalOutput channelBit4;

    private DigitalOutput signalLock;

    private DigitalInput freeze;
    private DigitalInput shutdown;

    private static int pressCount = 0;

    private static Context pi4j;


    private Console console;
    @Autowired
    PrintInfo printInfo;


    /*
            I tried using raspberrypi-digital-input and raspberrypi-digital-output
            but, they did nothing. Probably these are not yet implmeneted in pi4j-2.2.1
     */
    private final String inputProvider = "pigpio-digital-input";
    private final String outputProvider = "pigpio-digital-output";



    @PreDestroy
    public void destroy(){
        logger.info("Shutting down the GPIO service.");
        try{
            if(null != pi4j){
                pi4j.shutdown();
                logger.info("GPIO service successfully shut down.");
            }
        }catch(Exception e){
            logger.error("Could not shutdown PI4J", e);
        }

    }

    /*
            Wait until Spring has completed all configuraton then go ahead and create the
            PI4J Context.
     */
    @PostConstruct
    private void postConstruct() {
        try {
            pi4j = Pi4J.newAutoContext();
            console = new Console();

            printInfo.printLoadedPlatforms(console, pi4j);
            printInfo.printDefaultPlatform(console, pi4j);
            printInfo.printProviders(console, pi4j);

            logger.info("PI4J context created.");
        }catch(Exception e){
            logger.error("Could not create PI4J Context. ", e);
        }
    }

    /*
            Tried using @Value to be able to configure the input and output digital providers
            but got exceptions when doing that so have removed. This will probably mean that
            we cannot run tests when building this on windows.
     */
//    public Pi4jMinimalBT(@Value("${pi4j.inputprovider}") final String inputProvider,
//                         @Value("${pi4j.outputprovider}")final String outputProvider){
//        this.inputProvider = inputProvider;
//        this.outputProvider = outputProvider;
//
//    }

    public Pi4jMinimalBT(){

    }



    public void initializeService(){


        // print out the providers that we are using
        // initialize freeze input.
        // We'll add a listener so that when the signal is received it can send
        // a web request to our waiting signal controller endpoint.
        var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("freeze")
                .name("freeze input")
                .address(PIN_IN_FREEZE)
                .pull(PullResistance.PULL_UP)
                .debounce(3000L)
                .provider("pigpio-digital-input");
        freeze = pi4j.create(buttonConfig);
        freeze.addListener(e -> {
            if (e.state() == DigitalState.LOW) {
                logger.info("FREEZE went low.");
            }else{
                logger.info("FREEZE went high.");
            }
        });

        var ledConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id("PIN_OUT_CHANNEL1")
                .name("ChannelBit1")
                .address(PIN_OUT_CHANNEL1)
                .shutdown(DigitalState.LOW)
                .initial(DigitalState.LOW)
                .provider("pigpio-digital-output");
        channelBit1 = pi4j.create(ledConfig);
        if (channelBit1.equals(DigitalState.LOW))
            channelBit1.high();
        else
            channelBit1.low();

        ledConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id("PIN_OUT_CHANNEL2")
                .name("ChannelBit2")
                .address(PIN_OUT_CHANNEL2)
                .shutdown(DigitalState.LOW)
                .initial(DigitalState.LOW)
                .provider("pigpio-digital-output");
        channelBit2 = pi4j.create(ledConfig);
        if (channelBit2.equals(DigitalState.LOW))
            channelBit2.high();
        else
            channelBit2.low();


        ledConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id("PIN_OUT_CHANNEL4")
                .name("ChannelBit4")
                .address(PIN_OUT_CHANNEL4)
                .shutdown(DigitalState.LOW)
                .initial(DigitalState.LOW)
                .provider("pigpio-digital-output");
        channelBit4 = pi4j.create(ledConfig);

        if (channelBit4.equals(DigitalState.LOW))
            channelBit4.high();
        else
            channelBit4.low();

        ledConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id("PIN_OUT_SIGNAL_LOCK")
                .name("SignalLock")
                .address(PIN_OUT_SIGNAL_LOCK)
                .shutdown(DigitalState.LOW)
                .initial(DigitalState.LOW)
                .provider("pigpio-digital-output");
        signalLock = pi4j.create(ledConfig);
        if (signalLock.equals(DigitalState.LOW))
            signalLock.high();
        else
            signalLock.low();

        buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("shutdown")
                .name("ShutDown Signal")
                .address(PIN_IN_SHUTDOWN)
                .pull(PullResistance.PULL_UP)
                .debounce(3000L)
                .provider("pigpio-digital-input");
        shutdown = pi4j.create(buttonConfig);
        shutdown.addListener(e -> {
            if (e.state() == DigitalState.LOW) {
                logger.info("SHUTDOWN signal went low.");
            }else{
                logger.info("SHUTDOWN signal went high.");
            }
        });

        console = new Console();
        logger.info("Printing out the registry for GPIO.");

        printInfo.printRegistry(console, pi4j);
        logger.info("Initialization complete.");
    }

    public boolean TurnLockOn(){
        if(signalLock.equals(DigitalState.LOW)){
            signalLock.high();
            logger.info("SIGNAL LOCK ON.");
            return true;
        }
        return false;
    }

    public boolean TurnLockOff(){
        if (signalLock.equals(DigitalState.HIGH)) {
            signalLock.low();
            logger.info("SIGNAL LOCK LOW.");
            return true;
        }
        return false;
    }

    public boolean TurnChan1On(){
        if(channelBit1.equals(DigitalState.LOW)){
            channelBit1.high();
            logger.info("CHAN1 ON.");
            return true;
        }
        return false;
    }

    public boolean TurnChan1Off(){
        if (channelBit1.equals(DigitalState.HIGH)) {
            channelBit1.low();
            logger.info("CHAN1 OFF.");
            return true;
        }
        return false;
    }

    public boolean TurnChan2On(){
        if(channelBit2.equals(DigitalState.LOW)){
            channelBit2.high();
            logger.info("CHAN2 ON.");
            return true;
        }
        return false;
    }

    public boolean TurnChan2Off(){
        if (channelBit2.equals(DigitalState.HIGH)) {
            channelBit2.low();
            logger.info("CHAN2 OFF.");
            return true;
        }
        return false;
    }

    public boolean TurnChan4On(){
        if(channelBit4.equals(DigitalState.LOW)){
            channelBit4.high();
            logger.info("CHAN4 ON.");
            return true;
        }
        return false;
    }

    public boolean TurnChan4Off(){
        if (channelBit4.equals(DigitalState.HIGH)) {
            channelBit4.low();
            logger.info("CHAN4 OFF.");
            return true;
        }
        return false;
    }

}

