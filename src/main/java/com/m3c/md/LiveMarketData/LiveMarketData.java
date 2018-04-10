package com.m3c.md.LiveMarketData;

import com.m3c.md.OrderManager.Order;

public interface LiveMarketData {
	void setPrice(Order order);
}
