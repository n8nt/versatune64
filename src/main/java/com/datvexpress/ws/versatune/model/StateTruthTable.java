package com.datvexpress.ws.versatune.model;

import com.datvexpress.ws.versatune.enums.TunerStateMachineEvents;
import com.datvexpress.ws.versatune.enums.TunerStateMachineStates;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
        This class builds a state transition table when the closs is instantiated.
        The scanning logic will use the table to determine what action to take while the tuner is
        returning data from the fifo.

        The class has methods to determine from the state table what the new TUNER State will be and
        whether or not the tuner should change the channel.
 */
@Setter
@Getter
@Component
public class StateTruthTable {
    private List<StateTruthTableRow> truthTable = new ArrayList<>();

    Logger logger = LoggerFactory.getLogger(getClass());

    public StateTruthTable(){


        StateTruthTableRow row = new StateTruthTableRow( TunerStateMachineStates.TUNER_STARTING,TunerStateMachineEvents.TUNER_NEW_SCAN, TunerStateMachineStates.TUNER_STARTING,false);
        truthTable.add(row);

        // looking for "[GetChipId] chip id:AVL6862", TunerStateMachineEvents.TUNER_FOUND)
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_STARTING, TunerStateMachineEvents.TUNER_FOUND, TunerStateMachineStates.TUNER_READY,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_STARTING,TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_STARTING,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_STARTING,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_STARTING,TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_STARTING,TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_STARTING,TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_STARTING,   TunerStateMachineEvents.TUNER_UNABLE_TO_CLAIM_INTERFACE,TunerStateMachineStates.TUNER_STARTING,false);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_STARTING,   TunerStateMachineEvents.TUNER_INDETERMINANT,TunerStateMachineStates.TUNER_STARTING,false);
        truthTable.add(row);


        // looking for "[GetFamilyId] Family ID:0x4955", TunerStateMachineEvents.TUNER_INTIALIZING)
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_READY,TunerStateMachineEvents.TUNER_INTIALIZING, TunerStateMachineStates.TUNER_INITIALIZING,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_READY,TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_READY,TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_READY,TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_READY,TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_READY,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_READY,false);
        truthTable.add(row);

        // looking for "[AVL_Init] AVL_Initialize Booted!", TunerStateMachineEvents.TUNER_BOOTED)
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_BOOTED, TunerStateMachineStates.TUNER_BOOTED,false);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_DEMOD_MODE_NOT_DVBTX,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_FAILED_TO_SET_WORK_MODE,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZING,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_INITIALIZING,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_CONST_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_MOD_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_SNR_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_SSI_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZING, TunerStateMachineEvents.TUNER_SQI_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);

        //Looking for "[AVL_Init] ok", TunerStateMachineEvents.TUNER_INTIAZLIZED);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_BOOTED, TunerStateMachineEvents.TUNER_INTIAZLIZED, TunerStateMachineStates.TUNER_INITIALIZED,false);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_BOOTED, TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_BOOTED, TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_BOOTED, TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_BOOTED, TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_BOOTED,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_BOOTED,false);
        truthTable.add(row);

        // looking for "[AVL_ChannelScan_Tx] Lock Tuner :", TunerStateMachineEvents.TUNER_ATTEMPTING_LOCK)
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_ATTEMPTING_LOCK, TunerStateMachineStates.TUNER_ATTEMPTING_LOCK,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_SDK_VERSION_FOUND, TunerStateMachineStates.TUNER_INITIALIZED,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_PATCH_VERSION_FOUND, TunerStateMachineStates.TUNER_INITIALIZED,false);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_DEMOD_MODE_NOT_DVBTX,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_FAILED_TO_SET_WORK_MODE,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow(TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZED,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_INITIALIZED,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_CONST_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_MOD_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_SNR_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_SSI_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_INITIALIZED, TunerStateMachineEvents.TUNER_SQI_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);

        // Looking for ("[DVB_Tx_tuner_Lock] Tuner locked!", TunerStateMachineEvents.TUNER_SIGNAL_LOCKED)
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_ATTEMPTING_LOCK, TunerStateMachineEvents.TUNER_SIGNAL_LOCKED, TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_ATTEMPTING_LOCK, TunerStateMachineEvents.TUNER_SEARCHING_FOR_SIGNAL, TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_ATTEMPTING_LOCK, TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_ATTEMPTING_LOCK, TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_ATTEMPTING_LOCK, TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_ATTEMPTING_LOCK, TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_ATTEMPTING_LOCK,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_ATTEMPTING_LOCK,false);
        truthTable.add(row);

        // Looking for [AVL_ChannelScan_Tx] Freq is", TunerStateMachineEvents.TUNER_SEARCHING_FOR_SIGNAL)
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL, TunerStateMachineEvents.TUNER_SEARCHING_FOR_SIGNAL, TunerStateMachineStates.TUNER_DETECTED_SIGNAL,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL, TunerStateMachineEvents.TUNER_SIGNAL_LOCKED, TunerStateMachineStates.TUNER_SIGNAL_LOCKED,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL, TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL, TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL, TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL, TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL,false);
        truthTable.add(row);


        // Looking for ("[DVB_Tx_tuner_Lock] Tuner locked!", TunerStateMachineEvents.TUNER_SIGNAL_LOCKED)
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_DETECTED_SIGNAL, TunerStateMachineEvents.TUNER_SIGNAL_LOCKED, TunerStateMachineStates.TUNER_SIGNAL_LOCKED,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_DETECTED_SIGNAL, TunerStateMachineEvents.TUNER_SEARCHING_FOR_SIGNAL, TunerStateMachineStates.TUNER_SEARCHING_FOR_SIGNAL,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_DETECTED_SIGNAL, TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_DETECTED_SIGNAL, TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_DETECTED_SIGNAL, TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_DETECTED_SIGNAL, TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_DETECTED_SIGNAL,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_DETECTED_SIGNAL,false);
        truthTable.add(row);

        // looking for SSI, SNR, PER, SQI, GUARD, CONST
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_CONST_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_MOD_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_SNR_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_SSI_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_SQI_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_GUARD_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_FEC_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_FFT_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_PER_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_LOCKED_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);

        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED, TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_SIGNAL_LOCKED,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_SIGNAL_LOCKED,false);
        truthTable.add(row);

        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_CONST_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_MOD_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_SNR_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_SSI_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_SQI_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_PER_FOUND, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_LOCK_FAILED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_SEARCH_FAILED_RESETTING_FOR_NEW_SEARCH,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_UNLOCKED,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA, TunerStateMachineEvents.TUNER_FIFO_THREAD_FAILED_TO_START,TunerStateMachineStates.TUNER_RESET,true);
        truthTable.add(row);
        row = new StateTruthTableRow( TunerStateMachineStates.TUNER_RECEIVING_DATA,TunerStateMachineEvents.TUNER_TRANSPORT_SYNC, TunerStateMachineStates.TUNER_RECEIVING_DATA,false);
        truthTable.add(row);

    }

    /*
            Given a specific currentState and event use the table to determin the next state and
            whether the channel should be changed.

     */
    public StateTransitionIO processCurrentEvent(StateTransitionIO ioData) {
        Optional<StateTruthTableRow> ref = truthTable.stream()
                .filter(p -> p.getCurrentState() == ioData.getCurrentState() && p.getEventOrdinal() == ioData.getTunerEvent())
                .findFirst();
        if (ref.isPresent()) {
            ioData.setNextState(ref.get().getNextState());
            ioData.setChangeChannelRequired(ref.get().isChangeChannel());
        } else {
            ioData.setNextState(ioData.getCurrentState());
            ioData.setChangeChannelRequired(false);
        }
        logger.info("============ INPUT STATE: " + ioData.getCurrentState().name() + " NEXT STATE: " + ioData.getNextState().name());
        return ioData;
    }
}
