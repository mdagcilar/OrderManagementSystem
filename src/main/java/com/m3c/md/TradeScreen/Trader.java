package com.m3c.md.TradeScreen;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.net.ServerSocketFactory;

import com.m3c.md.OrderManager.Order;

public class Trader extends Thread implements TradeScreen {
    private Map<Integer, Order> orders = new HashMap<>();
    private static Socket omConn;
    private int port;
    private ObjectOutputStream objectOutputStream;

    public Trader(String name, int port) {
        this.setName(name);
        this.port = port;
    }


    public void run() {
        ObjectInputStream objectInputStream;

        //OM will connect to us
        try {
            omConn = ServerSocketFactory.getDefault().createServerSocket(port).accept();

            //objectInputStream=new ObjectInputStream( omConn.getInputStream());
            InputStream inputStream = omConn.getInputStream(); //if i try to create an objectinputstream before we have data it will block

            while (true) {
                if (0 < inputStream.available()) {
                    // TODO: find a way to not create the object everytime
                    objectInputStream = new ObjectInputStream(inputStream);
                    // stored orderID and Order objects in variables, opposed to calling each time
                    api method = (api) objectInputStream.readObject();
                    int orderID = objectInputStream.readInt();
                    Order order = (Order) objectInputStream.readObject();

                    System.out.println(Thread.currentThread().getName() + " calling: " + method + ", with OrderID: " + orderID);
                    switch (method) {
                        case newOrder:
                            newOrder(orderID, order);
                            break;
                        case price:
                            price(orderID, order);
                            break;
                        case cross:
//                            objectInputStream.readInt();
//                            objectInputStream.readObject();
                            break; //TODO
                        case fill:
//                            objectInputStream.readInt();
//                            objectInputStream.readObject();
                            break; //TODO
                    }
                } else {
                    //System.out.println("Trader Waiting for data to be available - sleep 1s");
                    Thread.sleep(1000);
                }
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void newOrder(int id, Order order) throws IOException, InterruptedException {
        //TODO the order should go in a visual grid, but not needed for test purposes
        Thread.sleep(2134);
        orders.put(id, order);
        acceptOrder(id);
    }

    @Override
    public void acceptOrder(int id) throws IOException {
        objectOutputStream = new ObjectOutputStream(omConn.getOutputStream());
        objectOutputStream.writeObject("acceptOrder");
        objectOutputStream.writeInt(id);
        objectOutputStream.flush();
        //TODO: introduce a threshold to slice an order.
    }

    @Override
    public void sliceOrder(int id, int sliceSize) throws IOException {
        objectOutputStream = new ObjectOutputStream(omConn.getOutputStream());
        objectOutputStream.writeObject("sliceOrder");
        objectOutputStream.writeInt(id);
        objectOutputStream.writeInt(sliceSize);
        objectOutputStream.flush();
    }

    @Override
    public void price(int id, Order o) throws InterruptedException, IOException {
        //TODO should update the trade screen
        Thread.sleep(2134);

        //TODO: Send price to the OutputStream. And remove slice from price()
        // either
        sliceOrder(id, orders.get(id).getSizeRemaining() / 2);
        // or
    }
}
