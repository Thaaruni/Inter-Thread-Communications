package lk.ijse.dep13.interthreadexercise.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

public class NurseryCP {
    private int poolSize;

    private final HashMap<Integer, Connection> MAIN_POOL = new HashMap<>();
    private final HashMap<Integer, Connection> CONSUMER_POOL = new HashMap<>();

    public NurseryCP() {
        try {
            initializePoolFromProperties();
        } catch (IOException | SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializePoolFromProperties() throws IOException, SQLException, ClassNotFoundException {

        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream("/application.properties"));

        String host = properties.getProperty("app.db.host");
        String port = properties.getProperty("app.db.port");
        String database = properties.getProperty("app.db.database");
        String user = properties.getProperty("app.db.user");
        String password = properties.getProperty("app.db.password");


        try {
            poolSize = Integer.parseInt(properties.getProperty("app.db.pool.size", "4"));
        } catch (NumberFormatException e) {
            poolSize = 4;
        }


        Class.forName("com.mysql.cj.jdbc.Driver");


        for (int i = 0; i < poolSize; i++) {
            Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://%s:%s/%s".formatted(host, port, database), user, password);
            MAIN_POOL.put((i + 1) * 10, connection);
        }
    }

    public synchronized int getPoolSize() {
        return poolSize;
    }

    public synchronized ConnectionWrapper getConnection() {
        while (MAIN_POOL.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Integer key = MAIN_POOL.keySet().stream().findFirst().get();
        Connection connection = MAIN_POOL.get(key);
        MAIN_POOL.remove(key);
        CONSUMER_POOL.put(key, connection);
        return new ConnectionWrapper(key, connection);
    }

    public synchronized void releaseConnection(Integer id) {
        if (!CONSUMER_POOL.containsKey(id)) throw new RuntimeException("Invalid Connection ID");
        Connection connection = CONSUMER_POOL.get(id);
        CONSUMER_POOL.remove(id);
        MAIN_POOL.put(id, connection);
        notify();
    }

    public synchronized void releaseAllConnections() {
        CONSUMER_POOL.forEach(MAIN_POOL::put);
        CONSUMER_POOL.clear();
        notifyAll();
    }

    public synchronized void resizePool(int newSize) throws SQLException, IOException, ClassNotFoundException {
        if (newSize <= 0) throw new IllegalArgumentException("Pool size must be greater than 0");

        // Expand the pool
        if (newSize > poolSize) {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/application.properties"));
            String host = properties.getProperty("app.db.host");
            String port = properties.getProperty("app.db.port");
            String database = properties.getProperty("app.db.database");
            String user = properties.getProperty("app.db.user");
            String password = properties.getProperty("app.db.password");

            for (int i = poolSize; i < newSize; i++) {
                Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://%s:%s/%s".formatted(host, port, database), user, password);
                MAIN_POOL.put((i + 1) * 10, connection);
            }

            // Shrink the pool
        } else if (newSize < poolSize) {
            int difference = poolSize - newSize;
            for (int i = 0; i < difference; i++) {
                Integer key = MAIN_POOL.keySet().stream().findFirst().orElse(null);
                if (key != null) {
                    Connection connection = MAIN_POOL.remove(key);
                    connection.close();
                }
            }
        }

        this.poolSize = newSize;
    }

    public record ConnectionWrapper(Integer id, Connection connection) {
    }
}


