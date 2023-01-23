package com.datvexpress.ws.versatune.model;

import com.datvexpress.ws.versatune.enums.TunerStateMachineStates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TunerEventMapTest {

    @BeforeEach
    void setUp() {

    }

    @Test
    public void testMappingResponseToEventForEventWithParameterw(){
        String line = "===  Freq is 423.000000 MHz";
        TunerEventMap map = new TunerEventMap();
        StateTransitionIO transitionIO = new StateTransitionIO();
        transitionIO.setCurrentState(TunerStateMachineStates.TUNER_STARTING);
        transitionIO.setTunerEvent(map.fetchTunerEventFromInputData(line));
        String x = transitionIO.toString();
    }
    @AfterEach
    void tearDown() {
    }
}