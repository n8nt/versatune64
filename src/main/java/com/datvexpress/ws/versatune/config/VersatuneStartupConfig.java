package com.datvexpress.ws.versatune.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;


@Configuration
@ComponentScan(basePackages = { "com.datvexpress.*" })
@PropertySource("classpath:application.yml")
public class VersatuneStartupConfig {

    @Value("${vlcoverlay}")
    private String versatuneOverlayPath;

    @Value("${dvbconfig}")
    private String versatuneDvbConfigPath;

    @Value("${knuckerfifo}")
    private String versatuneKnuckerFifo;

    @Value("${blanktspath}")
    private String blankTsPath;

    @Value("${combitunerpath}")
    private String combitunerPath;

    @Value("${scriptspath}")
    private String scriptsPath;

    @Value("${datapath}")
    private String dataPath;

    @Value("${appspath}")
    private String appPath;



    //To resolve ${} in @Value
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public String getVersatuneOverlayPath() {
        return versatuneOverlayPath;
    }

    public void setVersatuneOverlayPath(String versatuneOverlayPath) {
        this.versatuneOverlayPath = versatuneOverlayPath;
    }

    public String getVersatuneDvbConfigPath() {
        return versatuneDvbConfigPath;
    }

    public void setVersatuneDvbConfigPath(String versatuneDvbConfigPath) {
        this.versatuneDvbConfigPath = versatuneDvbConfigPath;
    }

    public String getVersatuneKnuckerFifo() {
        return versatuneKnuckerFifo;
    }

    public void setVersatuneKnuckerFifo(String versatuneKnuckerFifo) {
        this.versatuneKnuckerFifo = versatuneKnuckerFifo;
    }

    public String getBlankTsPath() {
        return blankTsPath;
    }

    public void setBlankTsPath(String blankTsPath) {
        this.blankTsPath = blankTsPath;
    }

    public String getCombitunerPath() {
        return combitunerPath;
    }

    public void setCombitunerPath(String combitunerPath) {
        this.combitunerPath = combitunerPath;
    }

    public String getScriptsPath() {
        return scriptsPath;
    }

    public void setScriptsPath(String scriptsPath) {
        this.scriptsPath = scriptsPath;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getAppPath() {
        return appPath;
    }

    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }
}
