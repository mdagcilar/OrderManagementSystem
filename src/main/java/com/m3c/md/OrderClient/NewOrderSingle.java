package com.m3c.md.OrderClient;

import java.io.Serializable;

import com.m3c.md.Ref.Instrument;

public class NewOrderSingle implements Serializable{
	public int size;
	public float price;
	public Instrument instrument;
	public NewOrderSingle(int size,float price,Instrument instrument){
		this.size=size;
		this.price=price;
		this.instrument=instrument;
	}
}