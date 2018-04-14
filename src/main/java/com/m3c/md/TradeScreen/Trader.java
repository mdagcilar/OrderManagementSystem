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

                    System.out.println(Thread.currentThread().getName() + " calling:" +
                            " " + method + ", with Order: [" + order.getClientId() + "," + order.getClientOrderID() + "]");
                    switch (method) {
                        case newOrder:
                            newOrder(orderID, order);
                            break;
                        case price:
                            price(orderID, order);
                            break;
                        case cross:
                            //TODO: link to internal cross
                            cross(orderID, order);
//                            objectInputStream.readInt();
//                            objectInputStream.readObject();
                            break;
                        case fill:
//                            objectInputStream.readInt();
//                            objectInputStream.readObject();
                            break;
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

    private void cross(int orderID, Order order) throws IOException {
        objectOutputStream = new ObjectOutputStream(omConn.getOutputStream());
        objectOutputStream.writeObject("cross");
        objectOutputStream.writeInt(orderID);
        objectOutputStream.writeObject(order);
    }

    //TODO the order should go in a visual grid, but not needed for test purposes
    @Override
    public void newOrder(int id, Order order) throws IOException, InterruptedException {
        Thread.sleep(200);

        orders.put(id, order);
        acceptOrder(id, order);
    }

    @Override
    public void acceptOrder(int id, Order order) throws IOException {
        objectOutputStream = new ObjectOutputStream(omConn.getOutputStream());
        objectOutputStream.writeObject("acceptOrder");
        objectOutputStream.writeInt(id);
        objectOutputStream.writeObject(order);
        objectOutputStream.flush();
    }

    @Override
    public void price(int id, Order order) throws InterruptedException, IOException {
        Thread.sleep(200);

        if (order.getQuantity() > 100000) {     // Current threshold for spiting an order is 100,000.
            // slice order
            sliceOrder(id, orders.get(id).getQuantityRemaining() / 2, order);
        } else {
            // just accept Order as a single Order.
            sliceOrder(id, orders.get(id).getQuantityRemaining(), order);
        }
    }

    @Override
    public void sliceOrder(int id, int sliceSize, Order order) throws IOException {
        objectOutputStream = new ObjectOutputStream(omConn.getOutputStream());
        objectOutputStream.writeObject("sliceOrder");
        objectOutputStream.writeInt(id);
        objectOutputStream.writeInt(sliceSize);
        objectOutputStream.writeInt(order.getClientId());
        objectOutputStream.writeInt(order.getClientOrderID());
        objectOutputStream.flush();
    }
}
