package jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;

public class JdbcTestWorker implements Callable<Integer> {

    private Connection connection;
    private boolean leakConnection;
    private volatile boolean terminate;

    public JdbcTestWorker(Connection connection, boolean leakConnection) {
        this.connection = connection;
        this.leakConnection = leakConnection;
    }

    public Integer call() throws Exception {
        int count = 0;
        try {
            while (!Thread.interrupted() && !terminate) {
                testStatementsPath1(connection);
                testStatementsPath2(connection);
                Thread.sleep(50);
                count++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
        }
        if (!leakConnection) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return count;
    }
    
    private void testStatementsPath1(Connection connection) throws SQLException {
        testStatement(connection);
        testPreparedStatement(connection);
    }

    private void testStatementsPath2(Connection connection) throws SQLException {
        testStatement(connection);
        testPreparedStatement(connection);
    }

    private void testPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement statement3 = connection.prepareStatement("UPDATE customer SET city=? WHERE city=?");
        PreparedStatement statement2 = connection.prepareStatement("UPDATE customer SET city=? WHERE city=?");
        for (int i = 0; i < 10; i++) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM customer WHERE city=?");
            statement.setString(1, "Olten");
            statement.executeQuery();
            statement.setString(1, "New York");
            statement.executeQuery();

            statement2.setString(1, "New York 2");
            statement2.setString(2, "New York");
            statement2.execute();
            statement2.executeUpdate();

            statement3.setString(1, "New York 2");
            statement3.setString(2, "New York");
            statement3.addBatch();
        }
        statement3.executeBatch();
    }

    private void testStatement(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        for (int i=0; i<10; i++) {
            statement.executeQuery("SELECT * FROM customer WHERE city='New York'");
        }
        for (int i=0; i<10; i++) {
            statement.executeQuery("SELECT * FROM customer WHERE city='New York2'" );
        }
        statement.close();
    }

    public void terminate() {
        terminate = true;
    }

}
