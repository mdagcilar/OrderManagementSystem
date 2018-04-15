package com.m3c.md.OrderClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.m3c.md.Main;
import com.m3c.md.OrderManager.Order;
import com.m3c.md.Ref.Instrument;
import com.m3c.md.Ref.Ric;

public class SampleClient extends Mock implements Client {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Main.class);

    private static final Random RANDOM_NUM_GENERATOR = new Random();
    private static final Instrument[] INSTRUMENTS = {new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};
    private static final Map<Integer, NewOrderSingle> OUTGOING_ORDERS = new HashMap(); //queue for outgoing orders
    private int id = 0; //message id number
    private Socket omConn; //connection to order manager

    public SampleClient(int port) throws IOException {
        //OM will connect to us
        omConn = new ServerSocket(port).accept();
        System.out.println("OM connected to client port " + port);
    }

    @Override
    public int sendOrder(NewOrderSingle newOrderSingle) throws IOException {
        Mock.show("sendOrder: id=" + id + " quantity=" + newOrderSingle.getSize() + " instrument=" + newOrderSingle.getInstrument().toString());
        OUTGOING_ORDERS.put(id, newOrderSingle);
        if (omConn.isConnected()) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(omConn.getOutputStream());
            objectOutputStream.writeObject("newOrderSingle");
            //objectOutputStream.writeObject("35=D;");
            objectOutputStream.writeInt(id);
            objectOutputStream.writeObject(newOrderSingle);
            objectOutputStream.flush();
        }
        return id++;
    }

    public int allOrdersComplete(int orderId) throws IOException {
        if (omConn.isConnected()) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(omConn.getOutputStream());
            objectOutputStream.writeObject("allOrdersComplete");
            objectOutputStream.writeInt(orderId);
            objectOutputStream.flush();
        }
        return id++;
    }


    @Override
    public void messageHandler() {

        ObjectInputStream objectInputStream;
        try {
            while (true) {
                while (0 < omConn.getInputStream().available()) {
                    objectInputStream = new ObjectInputStream(omConn.getInputStream());

                    String fix = (String) objectInputStream.readObject();

                    String[] fixTags = fix.split(";");
                    int orderId = -1;
                    char MsgType;

                    for (String fixTag : fixTags) {
                        String[] tag_value = fixTag.split("=");
                        switch (tag_value[0]) {
                            case "11":
                                orderId = Integer.parseInt(tag_value[1]);
                                break;
                            case "35":
                                MsgType = tag_value[1].charAt(0);
                                if (MsgType == 'A') {
                                    newOrderAck(orderId);
                                }
                                break;
                            case "39":
                                int orderStatus = tag_value[1].charAt(0);
                                if (orderStatus == '0') {
                                    acceptOrderAck(orderId);
                                } else if (orderStatus == '2') {
                                    completeOrderAck(orderId, fix);
                                    OUTGOING_ORDERS.remove(id);

                                    if (OUTGOING_ORDERS.size() == 0) {
                                        allOrdersComplete(id);
                                    }
                                }
                                break;
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void newOrderAck(int OrderId) {
        //System.out.println((Thread.currentThread().getName() + " called newOrderAck, with OrderID: " + OrderId));
    }

    void acceptOrderAck(int OrderId) {
        //System.out.println((Thread.currentThread().getName() + " called acceptOrderAck, with OrderID: " + OrderId));
    }

    void completeOrderAck(int OrderId, String fixMessage) {
        System.out.println((Thread.currentThread().getName() +
                " called completeOrderAck, with OrderID: " + OrderId +
                " (FIX:" + fixMessage + ")"));
    }


    @Override
    public void sendCancel(int idToCancel) {
        Mock.show("sendCancel: id=" + idToCancel);
        if (omConn.isConnected()) {
            //OMconnection.sendMessage("cancel",idToCancel);
        }
    }

    @Override
    public void partialFill(Order order) {
        Mock.show("" + order);
    }

    @Override
    public void fullyFilled(Order order) {
        Mock.show("" + order);
        OUTGOING_ORDERS.remove(order.getClientOrderID());
    }

    @Override
    public void cancelled(Order order) {
        Mock.show("" + order);
        OUTGOING_ORDERS.remove(order.getClientOrderID());
    }
}