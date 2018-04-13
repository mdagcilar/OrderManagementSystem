package com.m3c.md.TradeScreen;

import java.io.IOException;

import com.m3c.md.OrderManager.Order;

public interface TradeScreen {
    enum api {newOrder, price, fill, cross};

    void newOrder(int id, Order order) throws IOException, InterruptedException;

    void acceptOrder(int id, Order order) throws IOException;

    void sliceOrder(int id, int sliceSize, Order order) throws IOException;

    void price(int id, Order order) throws InterruptedException, IOException;
}
