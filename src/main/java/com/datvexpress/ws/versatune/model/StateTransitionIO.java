package com.datvexpress.ws.versatune.model;

import com.datvexpress.ws.versatune.enums.TunerStateMachineEvents;
import com.datvexpress.ws.versatune.enums.TunerStateMachineStates;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class StateTransitionIO {
    // input values
    private TunerStateMachineStates currentState;
    private TunerStateMachineEvents tunerEvent;
    private TunerStateMachineStates nextState;
    private boolean changeChannelRequired = false;
}
