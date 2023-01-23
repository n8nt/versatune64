package com.datvexpress.ws.versatune.model;

import com.datvexpress.ws.versatune.enums.TunerStateMachineEvents;
import com.datvexpress.ws.versatune.enums.TunerStateMachineStates;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class StateTruthTableRow {
    private TunerStateMachineStates currentState;
    private TunerStateMachineEvents eventOrdinal;
    private TunerStateMachineStates nextState;
    private boolean changeChannel = false;
}
