package com.m3c.md;
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
        testOrder.slices.add(testSlice);
        testOrder.slices.add(testSlice2);
        testOrder.fills.add(testFill);
        when(testFill.getSize()).thenReturn(100);
    }

    @Test
    public void getSizeRemainingTest() {
        Assert.assertEquals(900, testOrder.getQuantityRemaining());
    }

    @Test
    public void totalSliceQuantityTest() { Assert.assertEquals(300, testOrder.totalSliceQuantity()); }

    @Test
    public void createFillTestPartial() {
        testOrder.createFill(300, 0);
        Assert.assertEquals('1', testOrder.getOrderStatus());
    }

    @Before
    public void newSetup() {
        testOrder2 = new Order(0,0, testInstrument, 1000);
    }

    @Test
    public void createFillTestFull() {
        testOrder2.createFill(1000, 0);
        Assert.assertEquals('2', testOrder2.getOrderStatus());
    }

    @Test
    public void newSliceTest() {
        testOrder2.newSlice(500);
        int newSliceIndex = testOrder2.slices.size() - 1;
        Assert.assertEquals(500, testOrder2.slices.get(newSliceIndex).getQuantity());
    }

//    @Before
//    public void setupCrossTest1() {
//        testOrder2 = new Order(0, 0, testInstrument, 1000);
//        testMatchingOrder = new Order(0, 0, testInstrument, 1000);
//
//    }
//
//    @Test
//    public void crossTest1() {
//
//    }
    @Test
    public void isOrderSatisfiedTest() {
        testOrder = new Order(0, 0, testInstrument, 1000);
        testOrder.fills.add(testFill);
        when(testFill.getSize()).thenReturn(1000);
        Assert.assertTrue(testOrder.isOrderSatisfied());
    }

    @Test
    public void isOrderPartiallySatisfiedTest() {
        testOrder = new Order(0, 0, testInstrument, 1000);
        testOrder.fills.add(testFill);
        when(testFill.getSize()).thenReturn(800);
        Assert.assertTrue(testOrder.isOrderPartiallySatisfied());
    }

}
