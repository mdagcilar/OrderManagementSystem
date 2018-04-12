package com.m3c.md.OrderManager;

import java.io.Serializable;
import java.util.ArrayList;

import com.m3c.md.Ref.Instrument;

//TODO: add unique order ID through the constructor
public class Order implements Serializable {

    private long uniqueOrderID;
    private double initialMarketPrice;
    private Instrument instrument;
    private char orderStatus = 'A'; //orderStatus is Fix 39, 'A' is 'Pending New'

    public ArrayList<Order> slices;
    public ArrayList<Fill> fills;

    private int quantity;
    public int clientId, clientOrderID, bestPriceCount;

    double[] bestPrices;

    short orderRouter;

    public Order(int clientId, int clientOrderID, Instrument instrument, int quantity) {
        this.clientOrderID = clientOrderID;
        this.quantity = quantity;
        this.clientId = clientId;
        this.instrument = instrument;
        fills = new ArrayList<>();
        slices = new ArrayList<>();
    }

    public Instrument getInstrument() {
        return instrument;
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

    public int getQuantity() {
        return quantity;
    }

    public int getQuantityRemaining() {
        return getQuantity() - sizeFilled();
    }

    private int sizeFilled() {
        int filledSoFar = 0;
        for (Fill f : fills) {
            filledSoFar += f.getSize();
        }
        for (Order c : slices) {
            filledSoFar += c.sizeFilled();
        }
        return filledSoFar;
    }

    public int totalSliceQuantity() {
        int totalSizeOfSlices = 0;

        for (Order order : slices) {
            totalSizeOfSlices += order.getQuantity();
        }
        return totalSizeOfSlices;
    }

    public void newSlice(int sliceSize) {
        Order slice = new Order(clientId, clientOrderID, instrument, sliceSize);
        slice.setInitialMarketPrice(initialMarketPrice);
        slices.add(slice);
    }


    //Status state;
    float price() {
        //TODO this is buggy as it doesn't take account of slices. Let them fix it
        float sum = 0;
        for (Fill fill : fills) {
            sum += fill.getPrice();
        }
        return sum / fills.size();
    }

    public void createFill(int size, double price) {
        fills.add(new Fill(size, price));
        if (getQuantityRemaining() == 0) {
            setOrderStatus('2'); // order Filled
        } else {
            setOrderStatus('1'); // order Partially filled
        }
    }

    void cross(Order matchingOrder) {
        //pair slices first and then parent
        for (Order slice : slices) {
            if (slice.getQuantityRemaining() == 0) continue;
            //TODO could optimise this to not start at the beginning every time
            for (Order matchingSlice : matchingOrder.slices) {
                int matchingSize = matchingSlice.getQuantityRemaining();
                if (matchingSize == 0) continue;
                int remainingSize = slice.getQuantityRemaining();
                if (remainingSize <= matchingSize) {
                    slice.createFill(remainingSize, initialMarketPrice);
                    matchingSlice.createFill(remainingSize, initialMarketPrice);
                    break;
                }
                //sze>msze
                slice.createFill(matchingSize, initialMarketPrice);
                matchingSlice.createFill(matchingSize, initialMarketPrice);
            }
            int remainingSize = slice.getQuantityRemaining();
            int mParent = matchingOrder.getQuantityRemaining() - matchingOrder.totalSliceQuantity();
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
            if (slice.getQuantityRemaining() > 0) break;
        }

        if (getQuantityRemaining() > 0) {
            for (Order matchingSlice : matchingOrder.slices) {
                int matchingSize = matchingSlice.getQuantityRemaining();
                if (matchingSize == 0) continue;
                int remainingSize = getQuantityRemaining();
                if (remainingSize <= matchingSize) {
                    createFill(remainingSize, initialMarketPrice);
                    matchingSlice.createFill(remainingSize, initialMarketPrice);
                    break;
                }
                //sze>msze
                createFill(matchingSize, initialMarketPrice);
                matchingSlice.createFill(matchingSize, initialMarketPrice);
            }
            int remainingSize = getQuantityRemaining();
            int matchingParentSize = matchingOrder.getQuantityRemaining() - matchingOrder.totalSliceQuantity();

            // if current slice > matching order size, then complete fill on matching parent
            if (remainingSize >= matchingParentSize) {
                createFill(matchingParentSize, initialMarketPrice);
                matchingOrder.createFill(matchingParentSize, initialMarketPrice);
            } else { // complete fill on the remaining size
                createFill(remainingSize, initialMarketPrice);
                matchingOrder.createFill(remainingSize, initialMarketPrice);
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
