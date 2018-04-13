package com.m3c.md.TradeScreen;

import com.m3c.md.OrderManager.Order;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TraderTest {

    private Trader testTrader = new Trader("MegaWhale", 2000);
    private Map<Integer, Order> orders = new HashMap<>();
    private Order testOrder = mock(Order.class);
    private static Socket omComm;
    //private ObjectOutputStream testObjectOutputStream = new ObjectOutputStream(omComm)

    @Test
    public void newOrderTest() throws IOException, InterruptedException {
        //when(omComm.getOutputStream()).thenReturn(testObjectOutputStream);
        //testTrader.newOrder(0, testOrder);
        //verify(testTrader, times(1)).acceptOrder(0);
        //Assert.assertEquals(orders.get(0), 0);
    }


}