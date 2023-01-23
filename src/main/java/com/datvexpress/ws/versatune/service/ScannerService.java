package com.datvexpress.ws.versatune.service;

import com.datvexpress.ws.versatune.enums.ScannerControl;
import com.datvexpress.ws.versatune.model.ScannerControlRecord;
import com.datvexpress.ws.versatune.model.TunerConfigRecord;
import com.datvexpress.ws.versatune.repo.ScannerControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ScannerService {

    Logger myLog = LoggerFactory.getLogger(getClass());
    final private ScannerControlRepository repo;

    public ScannerService( ScannerControlRepository repo){
        this.repo = repo;
    }

    public List<ScannerControlRecord> listall(){
        List<ScannerControlRecord> alist = new ArrayList<>();
        try{
            alist = repo.findAll();
            return alist;
        }catch(Exception e){
            myLog.error("Could not get list from database. ", e);
        }
        return repo.findAll();
    }

    public void save(ScannerControlRecord record){

        repo.save(record);
    }

    public ScannerControlRecord get(long id){
        return repo.findById(id).get();
    }

    public void delete(long id){
        repo.deleteById(id);
    }

    /*
            Get current ready channel from scanner control
            Currently, there is only one record in this table. It is used to track the
            channel that is in use. It returns the active scan channel (that is the
            channel number defined by the user in the TunerConfig database)

            If there is no active channel, then 0 is returned.

    */
    public int getActiveChannelNumber(){
        Optional<ScannerControlRecord>  scannerControlRef = repo.findAll().stream().findFirst();
        int readyChannel = 0;
        if (scannerControlRef.isPresent()){
            if (scannerControlRef.get().getStatus().equals(ScannerControl.ENABLED_ACTIVE.name())){
                readyChannel = scannerControlRef.get().getScanChannel();
            }
        }
        return readyChannel;

    }

    /*
            Gets SCAN CHAN from SCANNER RECORD
     */
    public int getScanChannelNumber(){
        Optional<ScannerControlRecord>  scannerControlRef = repo.findAll().stream().findFirst();
        int readyScanChannel = 0;
        if (scannerControlRef.isPresent()){
            readyScanChannel = scannerControlRef.get().getScanChannel();
        }
        return readyScanChannel;
    }
    /*
            Gets DB ID for the enabled and active Scanner Control Record.
            NOTE:
            Initial product release will only have one record in the DB for the ACTIVE Channel
     */
    public long getActiveChannelDbId(){
        Optional<ScannerControlRecord>  scannerControlRef = repo.findAll().stream().findFirst();

        long readyChannelId = 0L;
        if (scannerControlRef.isPresent()){
            String scannerState = scannerControlRef.get().getStatus();
            if ( scannerState.equals(ScannerControl.ENABLED_PENDING.name()) ||
                 scannerState.equals(ScannerControl.ENABLED_ACTIVE.name()) ||
                 scannerState.equals(ScannerControl.ENABLED_IDLE.name()) ||
                 scannerState.equals(ScannerControl.ENABLED_ACTIVE_VLC_CONNECTED.name()) ||
                 scannerState.equals(ScannerControl.ENABLED_SEARCH_FAILED.name()) ||
                 scannerState.equals(ScannerControl.ENABLED_NODATA.name()) ){
                 readyChannelId = scannerControlRef.get().getId();
            }
        }
        return readyChannelId;
    }

    public String getScannerControlStatus(){
        Optional<ScannerControlRecord>  scannerControlRef = repo.findAll().stream().findFirst();
        if (scannerControlRef.isPresent()){
            return scannerControlRef.get().getStatus();
        }else{
            // return empty so upper layer will go get next scan channel
            return ScannerControl.EMPTY.name();
        }
    }

    /*
            Gets the DB ID for the Enabled and Idle channel
     */
    public long getEnabledIdleChannelId(){
        Optional<ScannerControlRecord>  scannerControlRef = repo.findAll().stream().findFirst();

        long readyChannelId = 0L;
        if (scannerControlRef.isPresent()){
            if (scannerControlRef.get().getStatus().equals(ScannerControl.ENABLED_IDLE.name())){
                readyChannelId = scannerControlRef.get().getId();
            }
        }
        return readyChannelId;
    }

    /*
            In the case where there are no active channels, just get the first record available
            and use it.
     */
    public long getFirstScannerRecordId(){
        Optional<ScannerControlRecord>  scannerControlRef = repo.findAll().stream().findFirst();

        long readyChannelId = 0L;
        if (scannerControlRef.isPresent()){
            readyChannelId = scannerControlRef.get().getId();
        }
        return readyChannelId;
    }

    /*
            This just returns first Scanner Record and there should only be one
            at least in the initial release
     */
    public Optional<ScannerControlRecord> getFirstScannerRecord(){
        Optional<ScannerControlRecord>  scannerControlRef = repo.findAll().stream().findFirst();
        return scannerControlRef;
    }

    /*
            Get reference to the active scanner channel.
            in the initial version, there will never be more than one active channel record.
     */
    public Optional<ScannerControlRecord> getActiveScanChannel(){
        if ( getActiveChannelDbId() == 0){
            return Optional.empty();
        }else{
            return Optional.of(get(getActiveChannelDbId()));
        }
    }

    /*
            Get the TunerConfigRecord ID found in the Active Channel.
            NOTE: There should be only one of these records ever in the database.
            If more than one, then the FIRST match will be returned.
     */
    public long  getTunerIdFromActiveScanner(){
        List<ScannerControlRecord> activeChannels = repo.findAll();
        Optional<ScannerControlRecord> recRef = repo.findAll()
                .stream()
                .filter( p -> p.isEnableChan())
                .findFirst();
        if( recRef.isPresent()){
            return recRef.get().getChannelId();
        }
        return 0L;
    }

    /*
            Get current Controller Status. This returns status of last emabled and active channel. So if the channel was
            active but say got unlocked then the status would change to ENABLED_UNLOCKED. But we could drop the channel all the way
            out and have current Scanner COntrol status with and EMPTY controlled channel. For this we should get search for a new
            channel.
    */
    public String getScanControllerStatus(){
        List<ScannerControlRecord> activeChannels = repo.findAll();
        Optional<ScannerControlRecord> recRef = repo.findAll()
                .stream()
                .filter( p -> p.isEnableChan())
                .findFirst();
        if( recRef.isPresent()){
            return recRef.get().getStatus();
        }else{
            return ScannerControl.UNDEFINED.name();
        }
    }
    /*
            copy values from TunerConfig record into the active channel
     */
    public void copyTunerConfigData(TunerConfigRecord tr){
        Optional<ScannerControlRecord> scrRef = getFirstScannerRecord();
        if ( scrRef.isPresent()){
            ScannerControlRecord scr = scrRef.get();
            scr.updateWithTunerConfiguration(tr);
        }

    }
    /*
            Create a new EMPTY Scanner Control record unless there is one present
     */
    public Optional<ScannerControlRecord> getOrCreateScannerControlRecord(){

        if (getFirstScannerRecord().isPresent()){
            return getFirstScannerRecord();
        }
        ScannerControlRecord scr = new ScannerControlRecord();
        scr.setStatus(ScannerControl.EMPTY.name());
        scr.setChannelId(0);
        save(scr);
        return getFirstScannerRecord();
    }
}
