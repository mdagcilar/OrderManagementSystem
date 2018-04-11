package com.m3c.md.OrderClient;

import java.io.Serializable;

import com.m3c.md.Ref.Instrument;

public class NewOrderSingle implements Serializable {
    private int size;
    private double price;
    private Instrument instrument;

    public NewOrderSingle(int size, double price, Instrument instrument) {
        this.size = size;
        this.price = price;
        this.instrument = instrument;
    }

    public int getSize() {
        return size;
    }

    public double getPrice() {
        return price;
    }

    public Instrument getInstrument() {
        return instrument;
    }
}