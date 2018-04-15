package com.m3c.md.OrderClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.m3c.md.OrderManager.Order;
import com.m3c.md.Ref.Instrument;
import com.m3c.md.Ref.Ric;

public class SampleClient extends Mock implements Client {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SampleClient.class);

    private static final Random RANDOM_NUM_GENERATOR = new Random();
    private static final Instrument[] INSTRUMENTS = {new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};
    private final Map<Integer, NewOrderSingle> clientsOutgoingOrder = new HashMap();
    private int id = 0; //message id number
    private Socket omConn; //connection to order manager

    public SampleClient(int port) throws IOException {
        //OM will connect to us
        omConn = new ServerSocket(port).accept();
    }

    @Override
    public int sendOrder(NewOrderSingle newOrderSingle) throws IOException {
        Mock.show("sendOrder: id=" + id + " quantity=" + newOrderSingle.getSize() + " instrument=" + newOrderSingle.getInstrument().toString());

        clientsOutgoingOrder.put(id, newOrderSingle);
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

    private int allOrdersComplete(int orderId) throws IOException {
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
                                    clientsOutgoingOrder.remove(id);

                                    if (clientsOutgoingOrder.size() == 0) {
                                        allOrdersComplete(id);
                                    }
                                }
                                break;
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Error ClassNotFoundException " + e.getMessage());
        }
    }

    private void newOrderAck(int OrderId) {
        //System.out.println((Thread.currentThread().getName() + " called newOrderAck, with OrderID: " + OrderId));
    }

    private void acceptOrderAck(int OrderId) {
        //System.out.println((Thread.currentThread().getName() + " called acceptOrderAck, with OrderID: " + OrderId));
    }

    private void completeOrderAck(int OrderId, String fixMessage) {
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
        clientsOutgoingOrder.remove(order.getClientOrderID());
    }

    @Override
    public void cancelled(Order order) {
        Mock.show("" + order);
        clientsOutgoingOrder.remove(order.getClientOrderID());
    }
}