package com.m3c.md.OrderManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
//import java.util.IntSummaryStatistics;
import java.util.Map;

import com.m3c.md.Database.Database;
import com.m3c.md.LiveMarketData.LiveMarketData;
import com.m3c.md.Main;
import com.m3c.md.OrderClient.NewOrderSingle;
import com.m3c.md.OrderRouter.Router;
import com.m3c.md.TradeScreen.TradeScreen;

public class OrderManager {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Main.class);

    private static LiveMarketData liveMarketData;
    private HashMap<Integer, Order> orders = new HashMap<Integer, Order>(); //debugger will do this line as it gives state to the object
    //currently recording the number of new order messages we get. TODO why? use it for more?
    private int id = 0; //debugger will do this line as it gives state to the object
    private Socket[] orderRouters; //debugger will skip these lines as they dissapear at compile time into 'the object'/stack
    private Socket[] clients;
    private Socket trader;

    private Socket connect(InetSocketAddress location) throws InterruptedException {
        //TODO: connected is always false
        boolean connected = false;
        int tryCounter = 0;
        while (!connected && tryCounter < 600) {
            try {
                Socket socket = new Socket(location.getHostName(), location.getPort());
                socket.setKeepAlive(true);
                logger.info("Socket connection successful - " + location);
                return socket;
            } catch (IOException e) {
                Thread.sleep(1000);
                tryCounter++;
            }
        }
        logger.error("Failed to connect to " + location.toString());
        return null;
    }

    //@param args the command line arguments
    public OrderManager(InetSocketAddress[] orderRoutersAddresses, InetSocketAddress[] clientsInetAddresses, InetSocketAddress traderInetAddress,
                        LiveMarketData liveMarketData) throws IOException, ClassNotFoundException, InterruptedException {
        this.liveMarketData = liveMarketData;
        this.trader = connect(traderInetAddress);


        //for the router connections, copy the input array into our object field.
        //but rather than taking the address we create a socket+ephemeral port and connect it to the address
        this.orderRouters = new Socket[orderRoutersAddresses.length];
        for (int i = 0; i < orderRoutersAddresses.length; i++) {
            this.orderRouters[i] = connect(orderRoutersAddresses[i]);
        }

        //repeat for the client connections
        this.clients = new Socket[clientsInetAddresses.length];
        for (int j = 0; j < clientsInetAddresses.length; j++) {
            this.clients[j] = connect(clientsInetAddresses[j]);
        }

        //main loop, wait for a message, then process it
        while (true) {
            //TODO this is pretty cpu intensive, use a more modern polling/interrupt/select approach
            //we want to use the arrayindex as the clientId, so use traditional for loop instead of foreach
            for (int clientIndex = 0; clientIndex < clientsInetAddresses.length; clientIndex++) { //check if we have data on any of the sockets
                Socket client = clients[clientIndex];

                // if no socket available, create new stream
                if (0 < client.getInputStream().available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages
                    ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream()); //create an object inputstream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop
                    String method = (String) objectInputStream.readObject();
                    System.out.println(Thread.currentThread().getName() + " calling " + method);

                    switch (method) { //determine the type of message and process it
                        //call the newOrder message with the clientId and the message (clientMessageId,NewOrderSingle)
                        case "newOrderSingle":
                            newOrder(clientIndex, objectInputStream.readInt(), (NewOrderSingle) objectInputStream.readObject());
                            break;
                        //TODO create a default case which errors with "Unknown message type"+...
                    }
                }
            }

            for (int routerIndex = 0; routerIndex < orderRoutersAddresses.length; routerIndex++) { //check if we have data on any of the sockets
                Socket router = orderRouters[routerIndex];

                if (0 < router.getInputStream().available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages
                    ObjectInputStream objectInputStream = new ObjectInputStream(router.getInputStream()); //create an object inputstream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop
                    String method = (String) objectInputStream.readObject();
                    System.out.println(Thread.currentThread().getName() + " calling " + method);
                    switch (method) { //determine the type of message and process it
                        case "bestPrice":
                            int OrderId = objectInputStream.readInt();
                            int SliceId = objectInputStream.readInt();
                            Order slice = orders.get(OrderId).slices.get(SliceId);
                            slice.bestPrices[routerIndex] = objectInputStream.readDouble();
                            slice.bestPriceCount += 1;
                            if (slice.bestPriceCount == slice.bestPrices.length)
                                reallyRouteOrder(SliceId, slice);
                            break;
                        case "newFill":
                            newFill(objectInputStream.readInt(), objectInputStream.readInt(), objectInputStream.readInt(), objectInputStream.readDouble());
                            break;
                    }
                }
            }

            if (0 < trader.getInputStream().available()) {
                ObjectInputStream objectInputStream = new ObjectInputStream(this.trader.getInputStream());
                String method = (String) objectInputStream.readObject();
                System.out.println(Thread.currentThread().getName() + " calling " + method);
                switch (method) {
                    case "acceptOrder":
                        acceptOrder(objectInputStream.readInt());
                        break;
                    case "sliceOrder":
                        sliceOrder(objectInputStream.readInt(), objectInputStream.readInt());
                }
            }
        }
    }

    private void newOrder(int clientId, int clientOrderId, NewOrderSingle nos) throws IOException {
        orders.put(id, new Order(clientId, clientOrderId, nos.instrument, nos.size));
        //send a message to the client with 39=A; //OrdStatus is Fix 39, 'A' is 'Pending New'
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(clients[clientId].getOutputStream());
        //newOrderSingle acknowledgement
        //ClOrdId is 11=
        objectOutputStream.writeObject("11=" + clientOrderId + ";35=A;39=A;");
        objectOutputStream.flush();
        sendOrderToTrader(id, orders.get(id), TradeScreen.api.newOrder);
        //send the new order to the trading screen
        //don't do anything else with the order, as we are simulating high touch orders and so need to wait for the trader to accept the order
        id++;
    }

    private void sendOrderToTrader(int id, Order order, Object method) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(trader.getOutputStream());
        objectOutputStream.writeObject(method);
        objectOutputStream.writeInt(id);
        objectOutputStream.writeObject(order);
        objectOutputStream.flush();
    }

    public void acceptOrder(int id) throws IOException {
        Order order = orders.get(id);
        if (order.OrdStatus != 'A') { //Pending New
            logger.error("Error accepting order that has already been accepted");
            return;
        }
        order.OrdStatus = '0'; //New
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(clients[order.clientid].getOutputStream());
        //newOrderSingle acknowledgement
        //ClOrdId is 11=
        objectOutputStream.writeObject("11=" + order.ClientOrderID + ";35=A;39=0");
        objectOutputStream.flush();

        price(id, order);
    }

    public void sliceOrder(int id, int sliceSize) throws IOException {
        Order order = orders.get(id);
        //slice the order. We have to check this is a valid size.
        //Order has a list of slices, and a list of fills, each slice is a childorder and each fill is associated with either a child order or the original order
        if (sliceSize > order.sizeRemaining() - order.sliceSizes()) {
            logger.error("error sliceSize is bigger than remaining size to be filled on the order");
            return;
        }
        int sliceId = order.newSlice(sliceSize);
        Order slice = order.slices.get(sliceId);
        internalCross(id, slice);
        int sizeRemaining = order.slices.get(sliceId).sizeRemaining();
        if (sizeRemaining > 0) {
            routeOrder(id, sliceId, sizeRemaining, slice);
        }
    }

    private void internalCross(int id, Order order) throws IOException {
        for (Map.Entry<Integer, Order> entry : orders.entrySet()) {
            if (entry.getKey().intValue() == id) continue;
            Order matchingOrder = entry.getValue();
            if (!(matchingOrder.instrument.equals(order.instrument) && matchingOrder.initialMarketPrice == order.initialMarketPrice))
                continue;
            //TODO add support here and in Order for limit orders
            int sizeBefore = order.sizeRemaining();
            order.cross(matchingOrder);
            if (sizeBefore != order.sizeRemaining()) {
                sendOrderToTrader(id, order, TradeScreen.api.cross);
            }
        }
    }

    private void cancelOrder() {

    }

    private void newFill(int id, int sliceId, int size, double price) throws IOException {
        Order order = orders.get(id);
        order.slices.get(sliceId).createFill(size, price);
        if (order.sizeRemaining() == 0) {
            Database.write(order);
        }
        sendOrderToTrader(id, order, TradeScreen.api.fill);
    }

    private void routeOrder(int id, int sliceId, int size, Order order) throws IOException {
        for (Socket r : orderRouters) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(r.getOutputStream());
            objectOutputStream.writeObject(Router.api.priceAtSize);
            objectOutputStream.writeInt(id);
            objectOutputStream.writeInt(sliceId);
            objectOutputStream.writeObject(order.instrument);
            objectOutputStream.writeInt(order.sizeRemaining());
            objectOutputStream.flush();
        }
        //need to wait for these prices to come back before routing
        order.bestPrices = new double[orderRouters.length];
        order.bestPriceCount = 0;
    }

    private void reallyRouteOrder(int sliceId, Order order) throws IOException {
        //TODO this assumes we are buying rather than selling
        int minIndex = 0;
        double min = order.bestPrices[0];
        for (int i = 1; i < order.bestPrices.length; i++) {
            if (min > order.bestPrices[i]) {
                minIndex = i;
                min = order.bestPrices[i];
            }
        }
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(orderRouters[minIndex].getOutputStream());
        objectOutputStream.writeObject(Router.api.routeOrder);
        objectOutputStream.writeInt(order.id);
        objectOutputStream.writeInt(sliceId);
        objectOutputStream.writeInt(order.sizeRemaining());
        objectOutputStream.writeObject(order.instrument);
        objectOutputStream.flush();
    }

    private void sendCancel(Order order, Router orderRouter) {
        //orderRouter.sendCancel(order);
        //order.orderRouter.writeObject(order);
    }

    private void price(int id, Order order) throws IOException {
        liveMarketData.setPrice(order);
        sendOrderToTrader(id, order, TradeScreen.api.price);
    }
}