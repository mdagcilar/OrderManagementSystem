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


    private Order testOrder;
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

    @Test
    public void createFillTestFull() {
        testOrder.createFill(1000, 0);
        Assert.assertEquals('2', testOrder.getOrderStatus());
    }
}
