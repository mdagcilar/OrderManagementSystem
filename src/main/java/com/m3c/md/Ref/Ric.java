package com.m3c.md.Ref;

import java.io.Serializable;

public class Ric implements Serializable {
    private String ric;

    public Ric(String ric) {
        this.ric = ric;
    }

    public String getRic(){
        return ric;
    }

    public String getEx() {
        return ric.split(".")[1];
    }

    public String getCompany() {
        return ric.split(".")[0];
    }
}