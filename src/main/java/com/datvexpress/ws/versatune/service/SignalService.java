package com.datvexpress.ws.versatune.service;

import com.datvexpress.ws.versatune.model.RcvrSignal;
import com.datvexpress.ws.versatune.repo.RcvrSignalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SignalService {

    Logger myLog = LoggerFactory.getLogger(getClass());
    final private RcvrSignalRepository repo;

    public SignalService( RcvrSignalRepository repo){
        this.repo = repo;
    }

    public List<RcvrSignal> listall(){
        List<RcvrSignal> alist = new ArrayList<>();
        try{
            alist = repo.findAll();
            return alist;
        }catch(Exception e){
            myLog.error("Could not get list from database. ", e);
        }
        return repo.findAll();
    }

    public void save(RcvrSignal record){
        repo.save(record);
    }

    public RcvrSignal get(long id){
        return repo.findById(id).get();
    }

    public void delete(long id){
        repo.deleteById(id);
    }

    public void deleteBatch(List<RcvrSignal> batch ){
        repo.deleteAllInBatch(batch);
    }

    /*
            Get first available signal that is not in a ready state.
            Ready is when the signal has all 4 parts ssi,snr,sqi and per
     */
    public RcvrSignal getSignalFromPool(){
        RcvrSignal signal = null;
        List<RcvrSignal> recs = repo.findAll();

        Optional<RcvrSignal> recRef = repo
                .findAll()
                .stream()
                .filter(p -> !p.isReady())
                .findFirst();
        if ( recRef.isEmpty()){
            signal = new RcvrSignal("","","","");
            signal.setReady(false);
            signal.setTimestamp(System.currentTimeMillis());
        }else{
            signal = recRef.get();
        }
        return signal;
    }
}

