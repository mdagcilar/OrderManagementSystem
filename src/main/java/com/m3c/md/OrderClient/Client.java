package com.m3c.md.OrderClient;

import java.io.IOException;

import com.m3c.md.OrderManager.Order;

public interface Client{
	//Outgoing messages
	int sendOrder(Object object)throws IOException;
	void sendCancel(int id);
	
	//Incoming messages
	void partialFill(Order order);
	void fullyFilled(Order order);
	void cancelled(Order order);
	
	void messageHandler();
}