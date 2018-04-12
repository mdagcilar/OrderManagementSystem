package com.m3c.md.OrderManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private Map<Integer, Order> orders = new HashMap<>();   //debugger will do this line as it gives state to the object
    //currently recording the number of new order messages we get. TODO why? use it for more?
    private int orderID = 0; //debugger will do this line as it gives state to the object
    private Socket[] orderRouters; //debugger will skip these lines as they dissapear at compile time into 'the object'/stack
    private Socket[] clients;
    private Socket trader;


    //@param args the command line arguments
    public OrderManager(InetSocketAddress[] orderRoutersAddresses,
                        InetSocketAddress[] clientsInetAddresses,
                        InetSocketAddress traderInetAddress,
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

        ObjectInputStream objectInputStream;

        //main loop, wait for a message, then process it
        while (true) {
            //TODO this is pretty cpu intensive, use a more modern polling/interrupt/select approach
            //we want to use the arrayindex as the clientId, so use traditional for loop instead of foreach
            for (int clientIndex = 0; clientIndex < clientsInetAddresses.length; clientIndex++) { //check if we have data on any of the sockets
                Socket client = clients[clientIndex];

                // if no socket available, create new stream
                if (0 < client.getInputStream().available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages
                    objectInputStream = new ObjectInputStream(client.getInputStream()); //create an object inputstream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop

                    String method = (String) objectInputStream.readObject();
                    int orderID = objectInputStream.readInt();
                    NewOrderSingle newOrderSingle = (NewOrderSingle) objectInputStream.readObject();

                    System.out.println(Thread.currentThread().getName() + " calling " + method + ", with OrderID: " + orderID);

                    switch (method) { //determine the type of message and process it
                        //call the newOrder message with the clientId and the message (clientMessageId,NewOrderSingle)
                        case "newOrderSingle":
                            newOrder(clientIndex, orderID, newOrderSingle);
                            break;
                        //TODO create a default case which errors with "Unknown message type"+...
                    }
                }
            }

            for (int routerIndex = 0; routerIndex < orderRoutersAddresses.length; routerIndex++) { //check if we have data on any of the sockets
                Socket router = orderRouters[routerIndex];

                if (0 < router.getInputStream().available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages

                    objectInputStream = new ObjectInputStream(router.getInputStream()); //create an object inputstream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop

                    String method = (String) objectInputStream.readObject();
                    int OrderId = objectInputStream.readInt();
                    int SliceId = objectInputStream.readInt();

                    System.out.println(Thread.currentThread().getName() + " calling " + method + ", with OrderID: " + OrderId);

                    switch (method) { //determine the type of message and process it
                        case "bestPrice":
                            //TODO: define bestPrice() method
                            Order slice = orders.get(OrderId).slices.get(SliceId);
                            slice.bestPrices[routerIndex] = objectInputStream.readDouble();
                            slice.bestPriceCount += 1;
                            if (slice.bestPriceCount == slice.bestPrices.length)
                                reallyRouteOrder(SliceId, slice);
                            break;
                        case "newFill":
                            newFill(OrderId, SliceId, objectInputStream.readInt(), objectInputStream.readDouble());
                            break;
                    }
                }
            }

            if (0 < trader.getInputStream().available()) {
                objectInputStream = new ObjectInputStream(this.trader.getInputStream());

                // stored orderID and Order objects in variables, opposed to calling each time
                String method = (String) objectInputStream.readObject();
                int orderID = objectInputStream.readInt();

//                logger.info(Thread.currentThread().getName() + " calling " + method + ", with OrderID: " + orderID);
                System.out.println((Thread.currentThread().getName() + " calling " + method + ", with OrderID: " + orderID));
                switch (method) {
                    case "acceptOrder":
                        acceptOrder(orderID);
                        break;
                    case "sliceOrder":
                        sliceOrder(orderID, objectInputStream.readInt());
                }
            }
        }
    }

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

    private void sendOrderToTrader(int id, Order order, Object method) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(trader.getOutputStream());
        objectOutputStream.writeObject(method);
        objectOutputStream.writeInt(id);
        objectOutputStream.writeObject(order);
        objectOutputStream.flush();
    }

    private void newOrder(int clientId, int clientOrderId, NewOrderSingle newOrderSingle) throws IOException {

        orders.put(orderID, new Order(clientId, clientOrderId, newOrderSingle.getInstrument(), newOrderSingle.getSize()));

        //send a message to the client with 39=A; //OrdStatus is Fix 39, 'A' is 'Pending New'
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(clients[clientId].getOutputStream());
        //newOrderSingle acknowledgement
        //ClOrdId is 11=
        objectOutputStream.writeObject("11=" + clientOrderId + ";35=A;39=A;");
        objectOutputStream.flush();
        sendOrderToTrader(orderID, orders.get(orderID), TradeScreen.api.newOrder);
        //send the new order to the trading screen
        //don't do anything else with the order, as we are simulating high touch orders and so need to wait for the trader to accept the order
        orderID++;
    }


    public void acceptOrder(int id) throws IOException {
        Order order = orders.get(id);
        if (order.getOrderStatus() != 'A') { //Pending New
            logger.error("Error accepting order that has already been accepted");
            return;
        }
        //accept order change status to 0 and Message Type to 0
        order.setOrderStatus('0'); //Not Pending new order

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(clients[order.clientId].getOutputStream());
        //newOrderSingle acknowledgement
        //ClOrdId is 11=
        objectOutputStream.writeObject("11=" + order.clientOrderID + ";35=0;39=" + order.getOrderStatus());
        objectOutputStream.flush();

        price(id, order);
        internalCross(id, order);

    }

    public void sliceOrder(int id, int sliceSize) throws IOException {
        Order order = orders.get(id);
        //slice the order. We have to check this is a valid quantity.
        //Order has a list of slices, and a list of fills, each slice is a childorder and each fill is associated with either a child order or the original order
        if (sliceSize > order.getQuantityRemaining() - order.totalSliceQuantity()) {
            logger.error("error sliceSize is bigger than remaining quantity to be filled on the order");
            return;
        }
        order.newSlice(sliceSize);
        int sliceId = order.slices.size() - 1;
        Order slice = order.slices.get(sliceId);

        internalCross(id, slice);
        int sizeRemaining = order.slices.get(sliceId).getQuantityRemaining();

        // if slice remaining is not satisfied by internalCross, route order outside.
        if (sizeRemaining > 0) {
            routeOrder(id, sliceId, sizeRemaining, slice);
        }
    }

    private void internalCross(int orderID, Order order) throws IOException {
        for (Map.Entry<Integer, Order> entry : orders.entrySet()) {
            // if not the same order
            if (!(entry.getKey() == orderID)) {
                Order matchingOrder = entry.getValue();
                // if instrument matches and price is equal or less than client's order
                if ((matchingOrder.getInstrument().toString().equals(order.getInstrument().toString()))
                        && (matchingOrder.getInitialMarketPrice() <= order.getInitialMarketPrice())) {
                    int sizeBefore = order.getQuantityRemaining();
                    order.cross(matchingOrder);
                    if (sizeBefore != order.getQuantityRemaining()) {
                        sendOrderToTrader(orderID, order, TradeScreen.api.cross);
                    }
                }
            }
        }
    }

    private void newFill(int id, int sliceId, int size, double price) throws IOException {
        Order order = orders.get(id);
        order.slices.get(sliceId).createFill(size, price);
        if (order.getQuantityRemaining() == 0) {
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
            objectOutputStream.writeObject(order.getInstrument());
            objectOutputStream.writeInt(order.getQuantityRemaining());
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
        objectOutputStream.writeInt(order.clientId);
        objectOutputStream.writeInt(sliceId);
        objectOutputStream.writeInt(order.getQuantityRemaining());
        objectOutputStream.writeObject(order.getInstrument());
        objectOutputStream.flush();
    }

    private void price(int id, Order order) throws IOException {
        liveMarketData.setPrice(order);
        sendOrderToTrader(id, order, TradeScreen.api.price);
    }

    private void cancelOrder() {

    }

    private void sendCancel(Order order, Router orderRouter) {
        //orderRouter.sendCancel(order);
        //order.orderRouter.writeObject(order);
    }
}