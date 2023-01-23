package com.datvexpress.ws.versatune;

import com.datvexpress.ws.versatune.background.DvbtWorker;
import com.datvexpress.ws.versatune.pi4j.Pi4jMinimalBT;
import com.datvexpress.ws.versatune.utils.TunerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;

@SpringBootApplication
public class VersatuneApplication extends SpringBootServletInitializer {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TaskExecutor taskExecutor;


    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TunerConfiguration tunerConfig;

    @Autowired
    private Pi4jMinimalBT gpioService;



    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(VersatuneApplication.class);
    }

    public static void main(String[] args) {

        ApplicationContext context = new AnnotationConfigApplicationContext(VersatuneApplication.class);
        SpringApplication app = new SpringApplication(VersatuneApplication.class);
        app.addListeners(new ApplicationPidFileWriter());
//        app.setAdditionalProfiles("ssl");
        app.run(args);
    }


    @Bean
    CommandLineRunner init() {

        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {

                gpioService.initializeService();

                try{
                    // flash LOCK LED a few times
                    for (int i=0; i < 10; i++){
                        gpioService.TurnLockOn();
                        Thread.sleep(500);
                        gpioService.TurnLockOff();
                        Thread.sleep(500);
                    }
                }catch(Exception e){
                    logger.error("Caught exception. Will ignore this.", e);
                }


                DvbtWorker dvbtWorkeer = applicationContext.getBean(DvbtWorker.class);
                //GpioWorker gpioWorker = applicationContext.getBean(GpioWorker.class);
                try{
                    taskExecutor.execute(dvbtWorkeer);
               //     taskExecutor.execute(gpioWorker);

                    logger.info("Made it back from tuner database initialization.");
                }catch(Exception e){
                    logger.error("Got an error. ",e);
                }
            }

        };
    }




}
