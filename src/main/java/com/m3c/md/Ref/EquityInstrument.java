package com.m3c.md.Ref;

import java.util.Date;

class EquityInstrument extends Instrument {
    Date exDividend;

    public EquityInstrument(Ric ric) {
        super(ric);
    }
}