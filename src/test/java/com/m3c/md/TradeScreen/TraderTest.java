package com.m3c.md.TradeScreen;

import com.m3c.md.OrderManager.Order;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ServerSocketFactory;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TraderTest {

    public Trader testTrader = new Trader("MegaWhale", 2000);
    private Map<Integer, Order> orders = new HashMap<>();
    private Order testOrder = mock(Order.class);
    private ObjectOutputStream mockObjectOutputStream = mock(ObjectOutputStream.class);

//    @Test
//    public void acceptOrderTest() throws IOException, InterruptedException {
//        testTrader.setOmconn(ServerSocketFactory.getDefault().createServerSocket(2000).accept());
//        //when(testTrader.getOmconn().getOutputStream()).thenReturn(mockObjectOutputStream);
//        testTrader.acceptOrder(0, testOrder);
//        verify(mockObjectOutputStream, times(1)).writeObject("acceptOrder");
//    }


}