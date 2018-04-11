package com.m3c.md.Ref;

import java.util.Date;


class FutureInstrument extends Instrument {
    Date expiry;
    Instrument underlier;

    public FutureInstrument(Ric ric) {
        super(ric);
    }
}
