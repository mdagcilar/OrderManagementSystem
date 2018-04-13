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

public class OrderManager {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Main.class);

    private static LiveMarketData liveMarketData;
    private Map<Integer, List<Map.Entry<Integer, Order>>> clientOrders;

    List<Map.Entry<Integer, Order>> ordersList;

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

        clientOrders = new HashMap<>();
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
            ordersList = new ArrayList<>();
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
                    int sliceId = objectInputStream.readInt();

                    System.out.println(Thread.currentThread().getName() + " calling " + method + ", with OrderID: " + orderId);

                    switch (method) { //determine the type of message and process it
                        case "bestPrice":
                            //TODO: define bestPrice() method


                            // todo: change to get clientId - not orderId
                            Order slice = clientOrders.get(clientId).get(clientOrderId).getValue();

                            slice.bestPrices[routerIndex] = objectInputStream.readDouble();
                            slice.bestPriceCount += 1;
                            if (slice.bestPriceCount == slice.bestPrices.length)
                                reallyRouteOrder(sliceId, slice);
                            break;
                        case "newFill":
                            newFill(orderId, clientId, clientOrderId, sliceId, objectInputStream.readInt(), objectInputStream.readDouble());
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
                        acceptOrder(orderID, (Order) objectInputStream.readObject());
                        break;
                    case "sliceOrder":
                        sliceOrder(orderID, objectInputStream.readInt(), (Order) objectInputStream.readObject());
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
        // put the order in the hashmap
        Order order = new Order(clientId, clientOrderId, newOrderSingle.getInstrument(), newOrderSingle.getSize());

        ordersList.add(new java.util.AbstractMap.SimpleEntry<>(clientOrderId, order));

        clientOrders.put(clientId, ordersList);

        // send newOrderAck to client
        String message = "11=" + clientOrderId + ";35=A;39=A;";
        sendMessageToClient(clientId, message);

        sendOrderToTrader(orderID, clientOrders.get(clientId).get(clientOrderId).getValue(), TradeScreen.api.newOrder);
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

    public void sliceOrder(int orderId, int sliceSize, Order order) throws IOException {
        //slice the order. We have to check this is a valid quantity.
        //Order has a list of slices, and a list of fills, each slice is a childorder and each fill is associated with either a child order or the original order
        if (sliceSize > order.getQuantityRemaining() - order.totalSliceQuantity()) {
            logger.error("Error sliceSize is bigger than remaining quantity to be filled on the order");
            return;
        }
        order.newSlice(sliceSize);
        int sliceId = order.slices.size() - 1;
        Order slice = order.slices.get(sliceId);

        internalCross(orderId, slice);
        int sizeRemaining = order.slices.get(sliceId).getQuantityRemaining();

        // if slice remaining is not satisfied by internalCross, route order outside.
        if (sizeRemaining > 0) {
            routeOrder(orderId, sliceId, sizeRemaining, slice);
        }
    }

    private void internalCross(int orderID, Order slicedOrder) throws IOException {
        for (Map.Entry<Integer, List<Map.Entry<Integer, Order>>> entry : clientOrders.entrySet()) {
            // if not the same order
            if (!(entry.getKey() == orderID)) {
                Order matchingOrder = entry.getValue().get(0).getValue();
                // if instrument matches and price is equal or less than client's order
                if ((matchingOrder.getInstrument().toString().equals(slicedOrder.getInstrument().toString()))
                        && (matchingOrder.getInitialMarketPrice() <= slicedOrder.getInitialMarketPrice())) {
                    int sizeBefore = slicedOrder.getQuantityRemaining();
                    slicedOrder.setInitialMarketPrice(matchingOrder.getInitialMarketPrice());
                    slicedOrder.cross(matchingOrder);
                    if (sizeBefore != slicedOrder.getQuantityRemaining()) {
                        sendOrderToTrader(orderID, slicedOrder, TradeScreen.api.cross);
                    }
                }
            }
        }
    }

    private void crossComplete(int orderID, Order order) throws IOException {
        int sliceId = order.slices.size() - 1;
        Order slice = order.slices.get(sliceId);
        int sizeFilled = order.slices.get(sliceId).sizeFilled();

        newFill(orderID, order.getClientId(), order.getClientOrderID(), sliceId, sizeFilled, order.getInitialMarketPrice());
    }

    private void newFill(int orderId, int clientId, int clientOrderId, int sliceId, int size, double price) throws IOException {
        Order order = clientOrders.get(clientId).get(clientOrderId).getValue();

        System.out.println("OrderID: " + orderId + ", clientOrderId: " + clientOrderId + ", Slice ID: " + sliceId);
        order.slices.get(sliceId).createFill(size, price);      // sliced order status will change to '1' or '2'

        // set order status of Parent Order.
        if (order.isOrderSatisfied()) {
            order.setOrderStatus('2');
        } else if (order.isOrderPartiallySatisfied()) {
            order.setOrderStatus('1');
        }

        // Send message to client, order status will tell the client weather it is partial/full
        String message = "11=" + order.getClientOrderID() + ";35=0;39=" + order.getOrderStatus();
        sendMessageToClient(order.getClientId(), message);

        logger.info(
                "Trade successful: ClientID:" + order.getClientId() + ", ClientOrderId: " + order.getClientOrderID()
                        + ", Instrument:" + order.getInstrument()
                        + ", Quantity: " + order.getQuantity() + ", Quantity remaining: " + order.getQuantityRemaining());

        if (order.getQuantityRemaining() == 0) { // complete fill
            Database.write(order);
        }

        sendOrderToTrader(orderId, order, TradeScreen.api.fill);
    }

    private void routeOrder(int orderId, int sliceId, int size, Order order) throws IOException {
        for (Socket r : orderRouters) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(r.getOutputStream());
            objectOutputStream.writeObject(Router.api.priceAtSize);
            objectOutputStream.writeInt(orderId);
            objectOutputStream.writeInt(order.getClientId());
            objectOutputStream.writeInt(order.getClientOrderID());
            objectOutputStream.writeInt(sliceId);
            objectOutputStream.writeInt(order.getQuantityRemaining());
            objectOutputStream.writeObject(order.getInstrument());
            objectOutputStream.flush();
        }
        //need to wait for these prices to come back before routing

        for (Integer key : clientOrders.keySet()) {
            for (Map.Entry<Integer, Order> entry : clientOrders.get(key)) {
                entry.getValue().bestPrices = new double[orderRouters.length];
                entry.getValue().bestPriceCount = 0;
            }
        }
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
        //objectOutputStream.writeInt(order.getClientId() + order.getClientOrderID());
        objectOutputStream.writeInt(sliceId);
        objectOutputStream.writeInt(order.getClientId());
        objectOutputStream.writeInt(order.getClientOrderID());
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