package com.m3c.md.LiveMarketData;

import java.util.Random;

import com.m3c.md.LiveMarketData.LiveMarketData;
import com.m3c.md.OrderManager.Order;

//TODO this should really be in its own thread
public class SampleLiveMarketData implements LiveMarketData {
    private static final Random RANDOM_NUM_GENERATOR = new Random();

    public void setPrice(Order o) {
        o.setInitialMarketPrice(199 * RANDOM_NUM_GENERATOR.nextDouble());
    }

}
