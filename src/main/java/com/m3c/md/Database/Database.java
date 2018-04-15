package com.m3c.md.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

//TODO figure out how to make this abstract or an interface, but want the method to be static
public class Database {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Database.class);
    private static Connection conn = null;

    private static final String USER_NAME = "todowin";
    private static final String PASSWORD = "todopass";
    private static final String HOST_NAME = "mthree-oms-todowin.cbpt8oeqlkno.us-west-2.rds.amazonaws.com";
    private static final String PORT_NUMBER = "1521";
    private static final String SID = "TODOWIN";

    private static final String DB_HOSTNAME = "jdbc:oracle:thin:@//" +
            HOST_NAME
            + ":"
            + PORT_NUMBER
            + "/" + SID;

    /**
     * @return Get the current connection, if not create a connection
     */
    private static Connection getConnection() {
        if (conn != null) return conn;
        return connect();
    }

    /**
     * Initialise connection to database
     *
     * @return Connection
     */
    private static Connection connect() {
        try {
            conn = DriverManager.getConnection(DB_HOSTNAME, USER_NAME, PASSWORD);
            System.out.println("Connection to database on " + HOST_NAME + " successful:\n");
        } catch (Exception e) {
            System.out.println(DB_HOSTNAME);
            System.out.println("Connection to database failed: " + e.getMessage());
        }
        return conn;
    }


    public static void insertTradeToDb(String client_id, String client_order_id, String instrument,
                                       int quantity, double price) {
        conn = getConnection();

        String sqlQuery = "INSERT INTO completed_trades" +
                "(client_id, client_order_id, instrument, quantity, price)" +
                "VALUES(?,?,?,?,?)";

        try {
            PreparedStatement preparedStatement = conn.prepareStatement(sqlQuery);

            preparedStatement.setString(1, client_id);
            preparedStatement.setString(2, client_order_id);
            preparedStatement.setString(3, instrument);
            preparedStatement.setInt(4, quantity);
            preparedStatement.setDouble(5, price);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            logger.debug("Error writing the trade to the database " + e.getMessage());
            System.out.println("insertTradeToDb() failed");
        }
    }

    public static void write(Object o) {

    }
}