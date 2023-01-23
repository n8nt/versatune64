package com.datvexpress.ws.versatune.model;

import lombok.Getter;
import lombok.Setter;

/*
    This is a configuration group for the DISeQC hardware.

    The following combinations only are permitted. The validation method in the class will make sure that only
    legal combinations are selected.  Exclusive OR of the two lnb voltage signals. Can only be one or the other.
    

    lnb_13v    lnb_18v    khz_22
       T          F         F
       T          F         T
       F          T         F
       F          T         T

*/
@Setter
@Getter
public class DISeQCGroup {

    private boolean lnb_13v;
    private boolean lnb_18v;
    private boolean khz_22;

    public boolean isValid(){
        return  (lnb_13v ^ lnb_18v);
    }
}
