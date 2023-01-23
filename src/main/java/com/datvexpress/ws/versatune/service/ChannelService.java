package com.datvexpress.ws.versatune.service;

import com.datvexpress.ws.versatune.model.TunerConfigRecord;
import com.datvexpress.ws.versatune.repo.TunerSetupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
//@Transactional
public class ChannelService {

    Logger myLog = LoggerFactory.getLogger(getClass());
    final private TunerSetupRepository repo;

    public ChannelService( TunerSetupRepository repo){
        this.repo = repo;
    }

    public List<TunerConfigRecord> listall(){
        List<TunerConfigRecord> alist = new ArrayList<>();
        try{
            alist = repo.findAll();
            return alist;
        }catch(Exception e){
            myLog.error("Could not get list from database. ", e);
        }
        return repo.findAll();
    }

    /*
            NOTE:
            Saving this record could change another record if another record
            already has the same SCAN CHAN and is ENABLED.
     */
    public void save(TunerConfigRecord record){
        // make sure only one record with given channel.
        // If user specified a different channel, then change it.

        long channelId = getEnabledChannelForScanChan(record.getChannel());
        if (channelId > 0 && channelId != record.getId()){
            Optional<TunerConfigRecord> oldEnabledChannelRef = repo.findById(channelId);
            if (oldEnabledChannelRef.isPresent()){
                oldEnabledChannelRef.get().setEnableChan(false);
                repo.save(oldEnabledChannelRef.get());
            }
        }
        repo.save(record);
    }

    /*
        Get all channels with same channel number. We allow this, but only
        one can be enabled.
    */
    public List<TunerConfigRecord>  getAllChannelsWithScanChannel( int chan){

        return repo.findAll()
                .stream()
                .filter(p -> p.getChannel() == chan)
                .collect(Collectors.toList());
    }

    public void updateEnabledChannel(int chan){

    }

    /*
            return id of channel with specified SCAN CHAN that
            is enabled. (Can only have one channel of giving SCNA CHAN
            that is enabled.)

            Returns 0 if there is none.
     */
    public long getEnabledChannelForScanChan( int chan){
        List<TunerConfigRecord> channels = getEnabledChannels();
        for (TunerConfigRecord rec : channels){
            if (rec.getChannel() == chan){
                return rec.getId();
            }
        }
        return 0L;
    }

    public TunerConfigRecord get(long id){
        return repo.findById(id).get();
    }

    /*
           Get Optional<TunerConfigRecord> from id.
    */
    public Optional<TunerConfigRecord> getOptonal(long id){
        Optional<TunerConfigRecord> tcrRef = repo.findById(id);
        return tcrRef;
    }

    public void delete(long id){
        repo.deleteById(id);
    }


    /*
            Gets id of next SCAN CHAN to run. The current channel is passed in
            the entity id of the found record is returned.
     */

    public long getNextScanChannelFromEnabledChannels(int currentScanChan){
        List<TunerConfigRecord> enabledChannels = getEnabledChannels();
        if (enabledChannels.size() > 0)
        {
        int nextChannelTarget = getNextEnabledChannel(enabledChannels, currentScanChan);
        //  verify that next channel is present and in the list. If it is only one
        // in the list, it will stay active.

            // now we have to find the id so it will be the record that matches the scan chan
            int tempChannel = nextChannelTarget;
            Optional<TunerConfigRecord> resultRef = enabledChannels
                    .stream()
                    .filter ( p -> p.getChannel() == tempChannel )
                    .findFirst();
            if ( resultRef.isPresent()){
                return resultRef.get().getId();
            }
        }
        return 0L;
    }

    /*

            Given the channel of the current enabled channel find the channel number of the next
            enabled channel.
     */
    private int getNextEnabledChannel(List<TunerConfigRecord> enabledChannels, int startChannel){

        List<Integer> channels = new ArrayList<>();
        TunerConfigRecord startRecord;
        for ( TunerConfigRecord r : enabledChannels){
            channels.add(r.getChannel());
            if ( r.getChannel() == startChannel){
                startRecord = r;
            }
        }
        int start=0;
        for (int i=0; i < channels.size(); i++){
            if (channels.get(i) == startChannel) {
                start = i;
                break;
            }
        }
        int nextChannelIndex = start+1;
        if ( nextChannelIndex >= channels.size()){
            nextChannelIndex  = 0;
        }
        return channels.get(nextChannelIndex);
    }
    public int getNextChannelRecord(List<TunerConfigRecord> enabledChannels, int nextChannelTarget) {
        if (enabledChannels.size() > 0) {
            // find highest numbered channel
            int hiChan = Integer.MIN_VALUE;
            int loChan = Integer.MAX_VALUE;
            for (TunerConfigRecord r : enabledChannels) {
                if (r.getChannel() > hiChan)
                    hiChan = r.getChannel();
                if (r.getChannel() < loChan)
                    loChan = r.getChannel();
            }
            // now figure out where we are
            if (nextChannelTarget > hiChan) {
                nextChannelTarget = loChan;
            }
            return nextChannelTarget;
        } else {
            // no enabled channels so cannot scan.
            return 0;
        }

    }

    public List<TunerConfigRecord> getEnabledChannels(){
        return repo.findAll()
                .stream()
                .filter(p -> p.isEnableChan())
                .collect(Collectors.toList());
    }

    public int getEnabledChannelCount(){
        return getEnabledChannels().size();
    }

    public int getFirstReadyChannel() {
        List<TunerConfigRecord> enabledChannels = getEnabledChannels();

        if (enabledChannels.size() > 0) {
            // find lowest numbered channel
            int loChan = Integer.MAX_VALUE;
            for (TunerConfigRecord r : enabledChannels) {
                if (r.getChannel() < loChan)
                    loChan = r.getChannel();
            }
            // now figure out where we are
            return loChan;
        } else {
            // no enabled channels so cannot scan.
            return 0;
        }
    }

    public long getFirstReadyChannelsId() {
        List<TunerConfigRecord> enabledChannels = getEnabledChannels();
        long id=0L;
        if (enabledChannels.size() > 0) {
            // find lowest numbered channel
            int loChan = Integer.MAX_VALUE;
            for (TunerConfigRecord r : enabledChannels) {
                if (r.getChannel() < loChan) {
                    loChan = r.getChannel();
                    id = r.getId();
                }
            }

        } return id;
    }

    public int getNextReadyChannel(int currentChannel){
        List<TunerConfigRecord> enabledChannels = getEnabledChannels();
        int nextChannel = currentChannel + 1;
        if (enabledChannels.size() > 0) {
            // find highest numbered channel
            int hiChan = Integer.MIN_VALUE;
            int loChan = Integer.MAX_VALUE;
            for (TunerConfigRecord r : enabledChannels) {
                if (r.getChannel() > hiChan)
                    hiChan = r.getChannel();
                if (r.getChannel() < loChan)
                    loChan = r.getChannel();
            }
            // now figure out where we are and if new channel is outside range,
            // then go back to first channel.
            if (nextChannel > hiChan) {
                nextChannel = loChan;
            }
            return nextChannel;
        } else {
            // no enabled channels so cannot scan.
            return 0;
        }
    }

    /*
            Get next channel that is marked as enabled.
            Given: Id of channel that was last active
            returned: Id of next enabled channel
     */

    public long getNextReadyId(long currentId){
        try{
            // get channel number from currentId
            int currentChannel = get(currentId).getChannel();
            List<TunerConfigRecord> enabledChannels = getEnabledChannels();
            int nextChannel = currentChannel + 1;
            long nextHiId = 0L;
            long nextLoId = 0L;
            long nextId = 0L;
            if (enabledChannels.size() > 0) {
                // find highest numbered channel
                int hiChan = Integer.MIN_VALUE;
                int loChan = Integer.MAX_VALUE;
                for (TunerConfigRecord r : enabledChannels) {
                    if (r.getChannel() > hiChan) {
                        hiChan = r.getChannel();
                        nextHiId = r.getId();
                    }
                    if (r.getChannel() < loChan) {
                        loChan = r.getChannel();
                        nextLoId = r.getId();
                    }
                    if ( r.getChannel()== nextChannel){
                        nextId = r.getId();
                    }
                }
                // now figure out where we are and if new channel is outside range,
                // then go back to first channel.
                if (nextChannel > hiChan) {
                    nextChannel = loChan;
                    nextId = nextLoId;
                }
                return nextId;
            } else {
                // no enabled channels so cannot scan.
                return 0;
            }
        }catch(Exception e){
            myLog.error("Check chennelId. It cannot be 0. ",e);
            return 0;
        }

    }

    /*
            Check if specified channel is still enabled in the TunerConfiguration list
            Returns true if enabled or false if not.
     */
    public Boolean checkIfChannelIsEnabled(int channel){
        Optional<TunerConfigRecord> recRef = getEnabledChannels()
                .stream()
                .filter(p -> p.isEnableChan() && p.getChannel()==channel)
                .findFirst();
        return recRef.isPresent();
    }

    /*
            Check if specified ID record (this is the DB id for the TunerConfigurationRecord) is
            enabled.
            Returns true if enabled and false otherwise.
     */
    public Boolean checkIfChannelIdIsEnabled(long channelId){
        Optional<TunerConfigRecord> recRef = getEnabledChannels()
                .stream()
                .filter(p -> p.getId()==channelId && p.isEnableChan())
                .findFirst();
        return recRef.isPresent();
    }

    /*
            Disable all enabled tuner configs for specified ScanChan
    */
    public void disableAllEnabledChannelsForSelectedScanChannel(int selectedScanChan){
        List<TunerConfigRecord> enabledChannels = getEnabledChannels();


        getEnabledChannels().stream()
                .filter(p -> p.getChannel() == selectedScanChan)
                .forEach(p -> p.setEnableChan(false));
    }

    /*
            Get Currently active Tuner Channel

    */
    /*
            Get TunerConfigRecord(s) for specified SCAN CHANNEL
            Only looks at ENABLED channels
            Should ONLY be ONE ENABLED channel with the specified SCAN CHANNEL

     */
}


