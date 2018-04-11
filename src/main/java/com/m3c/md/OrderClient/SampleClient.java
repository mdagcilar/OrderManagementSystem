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
//import com.sun.tools.corba.se.idl.constExpr.Or;

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
    public int sendOrder(Object par0) throws IOException {
        int size = RANDOM_NUM_GENERATOR.nextInt(5000);
        int instrumentID = RANDOM_NUM_GENERATOR.nextInt(3);
        Instrument instrument = INSTRUMENTS[instrumentID];
        NewOrderSingle newOrderSingle = new NewOrderSingle(size, instrumentID, instrument);

        Mock.show("sendOrder: id=" + id + " size=" + size + " instrument=" + instrument.toString());
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
        OUTGOING_ORDERS.remove(order.ClientOrderID);
    }

    @Override
    public void cancelled(Order order) {
        Mock.show("" + order);
        OUTGOING_ORDERS.remove(order.ClientOrderID);
    }


    @Override
    public void messageHandler() {

        ObjectInputStream objectInputStream;
        try {
            while (true) {
                //objectInputStream.wait(); //this throws an exception!!
                while (0 < omConn.getInputStream().available()) {
                    objectInputStream = new ObjectInputStream(omConn.getInputStream());

                    String fix = (String) objectInputStream.readObject();
                    System.out.println(Thread.currentThread().getName() + " received fix message: " + fix);
                    String[] fixTags = fix.split(";");
                    int OrderId = -1;
                    char MsgType;

                    //String[][] fixTagsValues=new String[fixTags.length][2];
                    for (int i = 0; i < fixTags.length; i++) {
                        String[] tag_value = fixTags[i].split("=");
                        switch (tag_value[0]) {
                            case "11":
                                OrderId = Integer.parseInt(tag_value[1]);
                                break;
                            case "35":
                                MsgType = tag_value[1].charAt(0);
                                if (MsgType == 'A') {
                                    newOrderSingleAcknowledgement(OrderId);
                                }
                                break;
                            case "39":
                                //TODO: use ordStatus?
                                int OrdStatus = tag_value[1].charAt(0);
                                break;
                        }
                    }

					/*message=connection.getMessage();
					char type;
					switch(type){
						case 'C':cancelled(message);break;
						case 'P':partialFill(message);break;
						case 'F':fullyFilled(message);
					}*/
                    Mock.show("");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void newOrderSingleAcknowledgement(int OrderId) {
        logger.info(Thread.currentThread().getName() + " called newOrderSingleAcknowledgement, with OrderID: " + OrderId);
        //do nothing, as not recording so much state in the NOS class at present
    }
/*listen for connections
once order manager has connected, then send and cancel orders randomly
listen for messages from order manager and print them to stdout.*/
}