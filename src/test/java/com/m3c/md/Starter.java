package com.m3c.md;

import com.m3c.md.LiveMarketData.LiveMarketData;
import com.m3c.md.LiveMarketData.SampleLiveMarketData;
import com.m3c.md.OrderClient.NewOrderSingle;
import com.m3c.md.OrderClient.SampleClient;
import com.m3c.md.OrderManager.OrderManager;
import com.m3c.md.OrderRouter.SampleRouter;
import com.m3c.md.Ref.Instrument;
import com.m3c.md.Ref.Ric;
import com.m3c.md.TradeScreen.Trader;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Starter {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Starter.class);

    public static void main(String[] args) {
        PropertyConfigurator.configure("src/main/resources/log4j.properties");
        logger.info("Start");

        InetSocketAddress[] clients = createClients(2);

        //start sample routers
        (new SampleRouter("Router LSE", 2010)).start();
        (new SampleRouter("Router BATE", 2011)).start();

        (new Trader("Trader James", 2020)).start();
        //start order manager

        InetSocketAddress[] routers = {new InetSocketAddress("localhost", 2010)};
//        InetSocketAddress[] routers = {new InetSocketAddress("localhost", 2010),
//                new InetSocketAddress("localhost", 2011)};
        InetSocketAddress trader = new InetSocketAddress("localhost", 2020);

        LiveMarketData liveMarketData = new SampleLiveMarketData();
        (new MockOrderManager("Order Manager", routers, clients, trader, liveMarketData)).start();
    }

    /**
     * starts clients and returns a InetSocketAddress array
     *
     * @param numberOfClients - The number of clients to create
     * @return netSocketAddress array
     */
    private static InetSocketAddress[] createClients(int numberOfClients) {
        InetSocketAddress[] clients = new InetSocketAddress[numberOfClients];

        for (int i = 0; i < numberOfClients; i++) {
            new MockClient("Client " + i, 2021 + i).start();
            clients[i] = new InetSocketAddress("localhost", 2021 + i);
        }
        return clients;
    }
}

class MockClient extends Thread {
    private int port;

    MockClient(String name, int port) {
        this.port = port;
        this.setName(name);
    }

    private NewOrderSingle[] createOrders(int numberOfOrders) {
        NewOrderSingle[] orderSingles = new NewOrderSingle[numberOfOrders];
        Random random = new Random();
        Instrument[] instruments = {new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};

        for (int i = 0; i < numberOfOrders; i++) {
            orderSingles[i] = new NewOrderSingle(random.nextInt(100), random.nextInt(500), (instruments[random.nextInt(instruments.length)]));
        }
        return orderSingles;
    }

    public void run() {
        try {
            SampleClient sampleClient = new SampleClient(port);

            NewOrderSingle[] orders = createOrders(50);

            for (NewOrderSingle newOrderSingle : orders) {
                sampleClient.sendOrder(newOrderSingle);
            }

            sampleClient.messageHandler();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

class MockOrderManager extends Thread {
    private InetSocketAddress[] clients, routers;
    private InetSocketAddress trader;
    private LiveMarketData liveMarketData;

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