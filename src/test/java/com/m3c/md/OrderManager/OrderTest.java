package com.m3c.md.OrderManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.m3c.md.OrderManager.Fill;
import com.m3c.md.OrderManager.Order;
import com.m3c.md.Ref.Instrument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;

public class OrderTest {

    private Order testOrder, testOrder2, testMatchingOrder;
    private Fill testFill = mock(Fill.class);
    private Fill testFill2 = mock(Fill.class);
    public Instrument testInstrument = mock(Instrument.class);
    private Order testSlice = new Order(0,0, testInstrument, 100);
    private Order testSlice2 = new Order(0,0, testInstrument, 200);

    @Before
    public void setup() {
        testOrder = new Order(0, 0, testInstrument, 1000);
        testOrder.getSlices().add(testSlice);
        testOrder.getSlices().add(testSlice2);
        testOrder.fills.add(testFill);
        when(testFill.getSize()).thenReturn(100);
    }

    @Test
    public void getInstrument() {
        assertEquals(testInstrument, testOrder.getInstrument());
    }

    @Test
    public void getOrderStatus() {
        //testOrder = new Order(0,0,testInstrument, 1000);
        assertEquals('A', testOrder.getOrderStatus());
    }



    @Test
    public void getInitialMarketPrice() {
        Assert.assertNotEquals(null, testOrder.getInitialMarketPrice());
    }

    @Test
    public void setInitialMarketPrice() {
        testOrder.setInitialMarketPrice(200);
        assertEquals(200, testOrder.getInitialMarketPrice(), 0.005);
    }

    @Test
    public void getClientId() {
        assertEquals(0, testOrder.getClientId());
    }

    @Test
    public void getClientOrderID() {
        assertEquals(0, testOrder.getClientOrderID());
    }

    @Test
    public void getQuantity() {
        testOrder = new Order(0,0,testInstrument, 1000);
        assertEquals(1000, testOrder.getQuantity());
    }

    @Test
    public void getQuantityRemaining() {
        assertEquals(900, testOrder.getQuantityRemaining());
    }

    @Test
    public void isOrderSatisfied() {
        testOrder = new Order(0, 0, testInstrument, 1000);
        testOrder.fills.add(testFill);
        when(testFill.getSize()).thenReturn(1000);
        Assert.assertTrue(testOrder.isOrderSatisfied());
    }

    @Test
    public void isOrderPartiallySatisfied() {
            testOrder = new Order(0, 0, testInstrument, 1000);
            testOrder.fills.add(testFill);
            when(testFill.getSize()).thenReturn(800);
            Assert.assertTrue(testOrder.isOrderPartiallySatisfied());
    }

    @Test
    public void sizeFilled() {
        testOrder = new Order(0, 0, testInstrument, 1000);
        testOrder.fills.add(testFill);
        testOrder.getSlices().add(testSlice);
        when(testFill.getSize()).thenReturn(800);
        Assert.assertEquals(800, testOrder.sizeFilled());
    }

    @Test
    public void totalSliceQuantity() {
        assertEquals(300, testOrder.totalSliceQuantity());
    }

    @Test
    public void newSlice() {
        testOrder2.createNewSlice(500);
        int newSliceIndex = testOrder2.getSlices().size() - 1;
        assertEquals(500, testOrder2.getSlices().get(newSliceIndex).getQuantity());
    }

    @Test
    public void price() {
        testOrder = new Order(0,0,testInstrument, 1000);
        testOrder.fills.add(testFill);
        testOrder.fills.add(testFill2);
        when(testFill.getPrice()).thenReturn((double) 200);
        when(testFill2.getPrice()).thenReturn((double) 600);
        Assert.assertEquals(400, testOrder.price(), 0.005);

    }

    @Before
    public void newSetup() {
        testOrder2 = new Order(0,0, testInstrument, 1000);
    }

    @Test
    public void createFill() {
        testOrder2.createFill(300, 0);
        assertEquals('1', testOrder2.getOrderStatus());
        testOrder2.createFill(700, 0);
        assertEquals('2', testOrder2.getOrderStatus());
    }

    @Test
    public void cross() {
    }

    @Test
    public void cancel() {
    }

    @Test
    public void setOrderStatus() {
        testOrder.setOrderStatus('1');
        assertEquals('1', testOrder.getOrderStatus());
    }

}