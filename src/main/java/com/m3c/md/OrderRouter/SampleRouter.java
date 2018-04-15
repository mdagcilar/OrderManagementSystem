package com.m3c.md.OrderRouter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

import javax.net.ServerSocketFactory;

import com.m3c.md.Ref.Instrument;
import com.m3c.md.Ref.Ric;

public class SampleRouter extends Thread implements Router {
    private static final Random RANDOM_NUM_GENERATOR = new Random();
    private static final Instrument[] INSTRUMENTS = {new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};
    private Socket omConn;
    private int port;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    public SampleRouter(String name, int port) {
        this.setName(name);
        this.port = port;
    }

    public void run() {
        //OM will connect to us
        try {
            omConn = ServerSocketFactory.getDefault().createServerSocket(port).accept();
            while (true) {
                if (0 < omConn.getInputStream().available()) {
                    objectInputStream = new ObjectInputStream(omConn.getInputStream());

                    Router.api methodName = (Router.api) objectInputStream.readObject();
                    int orderId = objectInputStream.readInt();
                    int clientId = objectInputStream.readInt();
                    int clientIdOrder = objectInputStream.readInt();
                    int quantityRemaining = objectInputStream.readInt();
                    Instrument instrument = (Instrument) objectInputStream.readObject();

                    // System.out.println("Order Router received method call for:" + methodName);
                    switch (methodName) {
                        case priceAtSize:
                            priceAtSize(orderId, clientId, clientIdOrder, quantityRemaining, instrument);
                            break;
                        case routeOrder:
                            routeOrder(orderId, clientId, clientIdOrder, quantityRemaining, instrument);
                            break;
                    }
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void priceAtSize(int orderId, int clientId, int clientOrderId, int size, Instrument i) throws IOException {
        objectOutputStream = new ObjectOutputStream(omConn.getOutputStream());
        objectOutputStream.writeObject("bestPrice");
        objectOutputStream.writeInt(orderId);
        objectOutputStream.writeInt(clientId);
        objectOutputStream.writeInt(clientOrderId);
        objectOutputStream.writeDouble(199 * RANDOM_NUM_GENERATOR.nextDouble());
        objectOutputStream.flush();
    }

    @Override
    public void routeOrder(int orderId, int clientId, int clientOrderId, int size, Instrument i) throws IOException, InterruptedException { //MockI.show(""+order);
        Thread.sleep(42);

        objectOutputStream = new ObjectOutputStream(omConn.getOutputStream());
        objectOutputStream.writeObject("newFill");
        objectOutputStream.writeInt(orderId);
        objectOutputStream.writeInt(clientId);
        objectOutputStream.writeInt(clientOrderId);
        objectOutputStream.writeInt(RANDOM_NUM_GENERATOR.nextInt(size + 1));
        objectOutputStream.writeDouble(199 * RANDOM_NUM_GENERATOR.nextDouble());
        objectOutputStream.flush();
    }

    @Override
    public void sendCancel(int id, int sliceId, int size, Instrument i) { //MockI.show(""+order);
    }
}
