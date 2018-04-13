package com.m3c.md.OrderRouter;

import java.io.IOException;

import com.m3c.md.Ref.Instrument;

public interface Router {
    enum api {routeOrder, sendCancel, priceAtSize};

    void routeOrder(int id, int clientId, int clientOrderId, int sliceId, int size, Instrument i) throws IOException, InterruptedException;

    void sendCancel(int id, int sliceId, int size, Instrument i);

    void priceAtSize(int id, int clientId, int clientOrderId, int sliceId, int size, Instrument i) throws IOException;

}
