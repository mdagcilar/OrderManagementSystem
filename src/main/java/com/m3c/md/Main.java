package com.m3c.md;

import com.m3c.md.LiveMarketData.LiveMarketData;
import com.m3c.md.LiveMarketData.SampleLiveMarketData;
import com.m3c.md.OrderClient.SampleClient;
import com.m3c.md.OrderManager.OrderManager;
import com.m3c.md.OrderRouter.SampleRouter;
import com.m3c.md.TradeScreen.Trader;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Main {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Main.class);

    public static void main(String[] args) {
        PropertyConfigurator.configure("src/main/resources/log4j.properties");

        logger.info("TEST: this program tests ordermanager");

        //start sample clients
        MockClient mockClient = new MockClient("Client 1", 2000);
        mockClient.start();
        //(new MockClient("Client 2", 2001)).start();

        //start sample routers
        (new SampleRouter("Router LSE", 2010)).start();
        //(new SampleRouter("Router BATE", 2011)).start();

        (new Trader("Trader James", 2020)).start();
        //start order manager
        InetSocketAddress[] clients = {new InetSocketAddress("localhost", 2000)};
//        InetSocketAddress[] clients = {new InetSocketAddress("localhost", 2000),
//                new InetSocketAddress("localhost", 2001)};
        InetSocketAddress[] routers = {new InetSocketAddress("localhost", 2010)};
//        InetSocketAddress[] routers = {new InetSocketAddress("localhost", 2010),
//                new InetSocketAddress("localhost", 2011)};
        InetSocketAddress trader = new InetSocketAddress("localhost", 2020);
        LiveMarketData liveMarketData = new SampleLiveMarketData();
        (new MockOrderManager("Order Manager", routers, clients, trader, liveMarketData)).start();
    }
}

class MockClient extends Thread {
    private int port;

    MockClient(String name, int port) {
        this.port = port;
        this.setName(name);
    }

    public void run() {
        try {
            SampleClient sampleClient = new SampleClient(port);
            if (port == 2000) {
                //TODO why does this take an arg?
                sampleClient.sendOrder(null);
                //int id = sampleClient.sendOrder(null);
                //TODO client.sendCancel(id);
                sampleClient.messageHandler();
            } else {
                sampleClient.sendOrder(null);
                sampleClient.messageHandler();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}

class MockOrderManager extends Thread {
    InetSocketAddress[] clients;
    InetSocketAddress[] routers;
    InetSocketAddress trader;
    LiveMarketData liveMarketData;

    MockOrderManager(String name, InetSocketAddress[] routers, InetSocketAddress[] clients, InetSocketAddress trader, LiveMarketData liveMarketData) {
        this.clients = clients;
        this.routers = routers;
        this.trader = trader;
        this.liveMarketData = liveMarketData;
        this.setName(name);
    }

    @Override
    public void run() {
        try {
            //In order to debug constructors you can do F5 F7 F5
            new OrderManager(routers, clients, trader, liveMarketData);
        } catch (IOException | ClassNotFoundException | InterruptedException ex) {
            Logger.getLogger(MockOrderManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}