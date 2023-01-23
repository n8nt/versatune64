package com.datvexpress.ws.versatune.controller;

import com.datvexpress.ws.versatune.enums.ScannerControl;
import com.datvexpress.ws.versatune.model.ScannerControlRecord;
import com.datvexpress.ws.versatune.model.TunerConfigRecord;
import com.datvexpress.ws.versatune.service.ChannelService;
import com.datvexpress.ws.versatune.service.ScannerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

//import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
public class ScannerController {

    ///////////////////////////////////////////////////////////////////////////
    //                                                                       //
    //  This controller will host the endpoints for doing the scanning       //
    //  control. It will monitor all enabled input sources to find a source  //
    //  that is able to receive on the channel's specification. Once an      //
    //  active source is found it will enter a loop where it will stay       //
    //  until the signal is lost or until it is manually switched by         //
    //  one of the endpoints at this controller.
    //                                                                       //
    //                                                                       //
    //                                                                       //
    //                                                                       //
    ///////////////////////////////////////////////////////////////////////////


    final ScannerService scannerService;
    final ChannelService channelService;

    public ScannerController(ScannerService scannerService, ChannelService channelService){
        this.scannerService = scannerService;
        this.channelService = channelService;
    }

    /*
            Get id of active channel if any
            Returns long - this is the id of the active channel
            returns 0 if no active channel
    */
    @RequestMapping(value="/activeScan", method= RequestMethod.GET)
    public long getActiveScannedChannel(@PathVariable String id, @RequestHeader HttpHeaders headers, HttpServletRequest servlet){
        return scannerService.getActiveChannelDbId();
    }

    /*
            Sets the active channel
            NOTE:
            Will need to check if something is already running with the currently active channel.
            That should not happen since all the changes to the active channel will happen outside the
            control loop the scans the channels.
     */
    @RequestMapping(value="/runScan", method=RequestMethod.POST,consumes = "application/json", produces = "application/json")
    public ScannerControlRecord postRcvStatus(@RequestBody TunerConfigRecord iModel){
        ScannerControlRecord response = new ScannerControlRecord(iModel);
        // get channel id form the input
        long channelId = iModel.getId();
        // see if this channel exists in the Scanner list
        Optional<ScannerControlRecord> responseRef = scannerService.getActiveScanChannel();

        if (responseRef.isPresent()){
            // just change this one to use the new info from out scanner
            response.updateWithTunerConfiguration(iModel);

        }else{
            // create a new record from the iModel
            ScannerControlRecord newRec = new ScannerControlRecord(iModel);
        }
        // set it active
        response.setStatus(ScannerControl.ENABLED_ACTIVE.name());
        return response;
    }

}
