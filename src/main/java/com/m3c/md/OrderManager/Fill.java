package com.m3c.md.OrderManager;

import java.io.Serializable;

public class Fill implements Serializable {
    //long id;
    private int size;
    private double price;

    Fill(int size, double price) {
        this.size = size;
        this.price = price;
    }

    public int getSize() {
        return size;
    }

    public double getPrice() {
        return price;
    }
}