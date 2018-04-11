package com.m3c.md.OrderManager;

import java.io.Serializable;
import java.util.ArrayList;

import com.m3c.md.Ref.Instrument;

//TODO: add unique order ID through the constructor
public class Order implements Serializable {

    private long uniqueOrderID, quantity;
    private double initialMarketPrice;
    private char orderStatus = 'A'; //orderStatus is Fix 39, 'A' is 'Pending New'

    public ArrayList<Order> slices;
    public ArrayList<Fill> fills;

    public int clientId, clientOrderID, size, bestPriceCount;

    double[] bestPrices;

    public Instrument instrument;
    short orderRouter;

    public Order(int clientId, int clientOrderID, Instrument instrument, int size) {
        this.clientOrderID = clientOrderID;
        this.size = size;
        this.clientId = clientId;
        this.instrument = instrument;
        fills = new ArrayList<>();
        slices = new ArrayList<>();
    }

    public char getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(char orderStatus) {
        this.orderStatus = orderStatus;
    }

    public double getInitialMarketPrice() {
        return initialMarketPrice;
    }

    public void setInitialMarketPrice(double initialMarketPrice) {
        this.initialMarketPrice = initialMarketPrice;
    }

    public int sliceSizes() {
        int totalSizeOfSlices = 0;
        for (Order c : slices) totalSizeOfSlices += c.size;
        return totalSizeOfSlices;
    }

    public int newSlice(int sliceSize) {
        slices.add(new Order(clientId, clientOrderID, instrument, sliceSize));
        return slices.size() - 1;
    }

    public int sizeFilled() {
        int filledSoFar = 0;
        for (Fill f : fills) {
            filledSoFar += f.size;
        }
        for (Order c : slices) {
            filledSoFar += c.sizeFilled();
        }
        return filledSoFar;
    }

    public int sizeRemaining() {
        return size - sizeFilled();
    }


    //Status state;
    float price() {
        //TODO this is buggy as it doesn't take account of slices. Let them fix it
        float sum = 0;
        for (Fill fill : fills) {
            sum += fill.price;
        }
        return sum / fills.size();
    }

    void createFill(int size, double price) {
        fills.add(new Fill(size, price));
        if (sizeRemaining() == 0) {
            orderStatus = '2';
        } else {
            orderStatus = '1';
        }
    }

    void cross(Order matchingOrder) {
        //pair slices first and then parent
        for (Order slice : slices) {
            if (slice.sizeRemaining() == 0) continue;
            //TODO could optimise this to not start at the beginning every time
            for (Order matchingSlice : matchingOrder.slices) {
                int matchingSize = matchingSlice.sizeRemaining();
                if (matchingSize == 0) continue;
                int remainingSize = slice.sizeRemaining();
                if (remainingSize <= matchingSize) {
                    slice.createFill(remainingSize, initialMarketPrice);
                    matchingSlice.createFill(remainingSize, initialMarketPrice);
                    break;
                }
                //sze>msze
                slice.createFill(matchingSize, initialMarketPrice);
                matchingSlice.createFill(matchingSize, initialMarketPrice);
            }
            int remainingSize = slice.sizeRemaining();
            int mParent = matchingOrder.sizeRemaining() - matchingOrder.sliceSizes();
            if (remainingSize > 0 && mParent > 0) {
                if (remainingSize >= mParent) {
                    slice.createFill(remainingSize, initialMarketPrice);
                    matchingOrder.createFill(remainingSize, initialMarketPrice);
                } else {
                    slice.createFill(mParent, initialMarketPrice);
                    matchingOrder.createFill(mParent, initialMarketPrice);
                }
            }
            //no point continuing if we didn't fill this slice, as we must already have fully filled the matchingOrder
            if (slice.sizeRemaining() > 0) break;
        }
        if (sizeRemaining() > 0) {
            for (Order matchingSlice : matchingOrder.slices) {
                int matchingSize = matchingSlice.sizeRemaining();
                if (matchingSize == 0) continue;
                int remainingSize = sizeRemaining();
                if (remainingSize <= matchingSize) {
                    createFill(remainingSize, initialMarketPrice);
                    matchingSlice.createFill(remainingSize, initialMarketPrice);
                    break;
                }
                //sze>msze
                createFill(matchingSize, initialMarketPrice);
                matchingSlice.createFill(matchingSize, initialMarketPrice);
            }
            int remainingSize = sizeRemaining();
            int mParent = matchingOrder.sizeRemaining() - matchingOrder.sliceSizes();
            if (remainingSize > 0 && mParent > 0) {
                if (remainingSize >= mParent) {
                    createFill(remainingSize, initialMarketPrice);
                    matchingOrder.createFill(remainingSize, initialMarketPrice);
                } else {
                    createFill(mParent, initialMarketPrice);
                    matchingOrder.createFill(mParent, initialMarketPrice);
                }
            }
        }
    }

    void cancel() {
        //state=cancelled
    }


}

class Basket {
    Order[] orders;
}

class Fill implements Serializable {
    //long id;
    int size;
    double price;

    Fill(int size, double price) {
        this.size = size;
        this.price = price;
    }
}
