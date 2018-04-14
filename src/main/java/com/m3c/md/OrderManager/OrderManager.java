package com.m3c.md.OrderManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
//import java.util.IntSummaryStatistics;

import com.m3c.md.Database.Database;
import com.m3c.md.LiveMarketData.LiveMarketData;
import com.m3c.md.Main;
import com.m3c.md.OrderClient.NewOrderSingle;
import com.m3c.md.OrderRouter.Router;
import com.m3c.md.TradeScreen.TradeScreen;
//import com.sun.tools.corba.se.idl.constExpr.Or;

public class OrderManager {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Main.class);

    private static LiveMarketData liveMarketData;
    private Map<Integer, Map<Integer, Order>> ordersHashMap;

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

        ordersHashMap = new HashMap<>();
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
                    int orderId = objectInputStream.readInt();
                    int clientId = objectInputStream.readInt();
                    int clientOrderId = objectInputStream.readInt();

                    System.out.println(Thread.currentThread().getName() + " calling " + method +
                            ", with Order: [" + clientId + "," + clientOrderId + "]");

                    switch (method) {
                        case "bestPrice":
                            //TODO: define bestPrice() method

                            Order slice = ordersHashMap.get(clientId).get(clientOrderId);

                            slice.bestPrices[routerIndex] = objectInputStream.readDouble();
                            slice.bestPriceCount += 1;
                            if (slice.bestPriceCount == slice.bestPrices.length)
                                reallyRouteOrder(clientOrderId, slice);
                            break;
                        case "newFill":
                            newFill(orderId, clientId, clientOrderId, objectInputStream.readInt(), objectInputStream.readDouble());
                            break;
                    }
                }
            }

            if (0 < trader.getInputStream().available()) {
                objectInputStream = new ObjectInputStream(this.trader.getInputStream());

                String method = (String) objectInputStream.readObject();
                int orderID = objectInputStream.readInt();

                System.out.println((Thread.currentThread().getName() + " calling " + method + ", with OrderID: " + orderID));
                switch (method) {
                    case "acceptOrder":
                        acceptOrder(orderID, (Order) objectInputStream.readObject());
                        break;
                    case "sliceOrder":
                        sliceOrder(orderID, objectInputStream.readInt(), objectInputStream.readInt(), objectInputStream.readInt());
                        break;
                    case "cross":
                        crossComplete(orderID, (Order) objectInputStream.readObject());
                        break;
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
                logger.debug("Socket connection successful - " + location);
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
        Order order = new Order(clientId, clientOrderId, newOrderSingle.getInstrument(), newOrderSingle.getSize());

        if (ordersHashMap.containsKey(clientId)) {
            ordersHashMap.get(clientId).put(clientOrderId, order);
        } else {
            Map<Integer, Order> innerMap = new HashMap<>();
            innerMap.put(clientOrderId, order);
            ordersHashMap.put(clientId, innerMap);
        }

        // send newOrderAck to client
        String message = "11=" + clientOrderId + ";35=A;39=A;";
        sendMessageToClient(clientId, message);

        sendOrderToTrader(orderID, order, TradeScreen.api.newOrder);
        orderID++;  // increase order number
    }


    public void acceptOrder(int orderId, Order order) throws IOException {
        if (order.getOrderStatus() != 'A') { //Pending New
            logger.error("Error accepting order that has already been accepted");
            return;
        }
        order.setOrderStatus('0'); // Change from new order to accepted order status

        // send acceptOrderAck to clients
        String message = "11=" + order.getClientOrderID() + ";35=0;39=" + order.getOrderStatus();
        sendMessageToClient(order.getClientId(), message);

        price(orderId, order);
    }

    // Sends a message to the Client
    private void sendMessageToClient(int clientId, String message) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(clients[clientId].getOutputStream());
        objectOutputStream.writeObject(message);
        objectOutputStream.flush();
    }

    public void sliceOrder(int orderId, int sliceSize, int clientId, int clientOrderId) throws IOException {
        Order order = ordersHashMap.get(clientId).get(clientOrderId);

        if (sliceSize > order.getQuantityRemaining()) {
            logger.error("Error sliceSize is bigger than remaining quantity to be filled on the order");
            return;
        }
        order.createNewSlice(sliceSize);
        routeOrder(orderId, clientId, clientOrderId);

        // Commenting out internal cross
//        internalCross(clientId, slice, clientOrderId);
//        int sizeRemaining = slice.getQuantityRemaining();
//
//        // if slice remaining is not satisfied by internalCross, route order outside.
//        if (sizeRemaining > 0) {
//            routeOrder(orderId, clientId, clientOrderId, sizeRemaining);
//        }
    }


    private void crossComplete(int orderID, Order order) throws IOException {
        int sliceId = order.getSlices().size() - 1;
        Order slice = order.getClientOrder(sliceId);

        int sizeFilled = slice.sizeFilled();

        newFill(orderID, order.getClientId(), order.getClientOrderID(), sizeFilled, order.getInitialMarketPrice());
    }

    private void newFill(int orderId, int clientId, int clientOrderId, int size, double price) throws IOException {
        // Returns the slice of an Order
        Order slice = ordersHashMap.get(clientId).get(clientOrderId);

        slice.createFill(size, price);

        // set order status of Parent Order.
        if (slice.isOrderSatisfied()) {
            slice.setOrderStatus('2');
        } else if (slice.isOrderPartiallySatisfied()) {
            slice.setOrderStatus('1');
        }

        String message = "11=" + slice.getClientOrderID() + ";35=0;39=" + slice.getOrderStatus();

        // if slice still has some remaining size, slice it again
        if (slice.getQuantityRemaining() != 0) {
            sliceOrder(orderId, slice.getQuantityRemaining(), clientId, clientOrderId);
        } else {
            Database.write(slice);

            sendMessageToClient(slice.getClientId(), message);      // send complete order acknowledgement to client
            logger.info(
                    "Trade Complete: ClientID:" + slice.getClientId() + ", ClientOrderId: " + slice.getClientOrderID()
                            + ", Instrument:" + slice.getInstrument()
                            + ", Quantity: " + slice.getQuantity() + ", Quantity remaining: " + slice.getQuantityRemaining());
        }
    }

    private void routeOrder(int orderId, int clientId, int clientOrderId) throws IOException {
        Order order = ordersHashMap.get(clientId).get(clientOrderId);

        for (Socket r : orderRouters) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(r.getOutputStream());
            objectOutputStream.writeObject(Router.api.priceAtSize);
            objectOutputStream.writeInt(orderId);
            objectOutputStream.writeInt(order.getClientId());
            objectOutputStream.writeInt(order.getClientOrderID());
            objectOutputStream.writeInt(order.getQuantityRemaining());
            objectOutputStream.writeObject(order.getInstrument());
            objectOutputStream.flush();
        }

        order.bestPrices = new double[orderRouters.length];
        order.bestPriceCount = 0;
    }

    private void reallyRouteOrder(int clientOrderId, Order order) throws IOException {
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
        objectOutputStream.writeInt(clientOrderId);
        objectOutputStream.writeInt(order.getClientId());
        objectOutputStream.writeInt(order.getClientOrderID());
        objectOutputStream.writeInt(order.getQuantityRemaining());
        objectOutputStream.writeObject(order.getInstrument());
        objectOutputStream.flush();
    }

    private void price(int id, Order o) throws IOException {
        Order order = ordersHashMap.get(o.getClientId()).get(o.getClientOrderID());
        liveMarketData.setPrice(order);
        sendOrderToTrader(id, order, TradeScreen.api.price);
    }

    private void cancelOrder() {

    }

    private void sendCancel(Order order, Router orderRouter) {
        //orderRouter.sendCancel(order);
        //order.orderRouter.writeObject(order);
    }


//    private void internalCross(int clientId, Order order, int clientOrderId) throws IOException {
//        Order slicedOrder = ordersHashMap.get(clientId).get(clientOrderId);
//
//        for (Map.Entry<Integer, Map<Integer, Order>> entry : ordersHashMap.entrySet()) {
//            // if not the same order
//            if (!(entry.getKey() == clientId)) {
//                Order matchingOrder = entry.getValue().get(clientOrderId);
//                // if instrument matches and price is equal or less than client's order
//                if ((matchingOrder.getInstrument().toString().equals(slicedOrder.getInstrument().toString()))
//                        && (matchingOrder.getInitialMarketPrice() <= slicedOrder.getInitialMarketPrice())) {
//                    int sizeBefore = slicedOrder.getQuantityRemaining();
//                    slicedOrder.setInitialMarketPrice(matchingOrder.getInitialMarketPrice());
//                    slicedOrder.cross(matchingOrder);
//                    if (sizeBefore != slicedOrder.getQuantityRemaining()) {
//                        sendOrderToTrader(clientId, slicedOrder, TradeScreen.api.cross);
//                    }
//                }
//            }
//        }
//    }
}