package com.m3c.md.Ref;

import java.io.Serializable;
import java.util.Date;

public class Instrument implements Serializable {
    private Ric ric;

//    private String name;
//    long id;
//    String isin;
//    String sedol;
//    String bbid;

    public Instrument(Ric ric) {
        this.ric = ric;
    }

    public String toString() {
        return ric.getRic();
    }
}

/*TODO
Index
bond
methods
*/