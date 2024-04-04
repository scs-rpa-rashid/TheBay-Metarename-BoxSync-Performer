package utility;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseUtil {
    public static Connection connectDB(String jdbcUrl, String userName, String passWord) {
        Connection connection = null;
        try {
            // Load the JDBC driver class
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            // Establish database connection
            connection = DriverManager.getConnection(jdbcUrl, userName, passWord);
            if (connection != null) {
                System.out.println("Connected to the database!");
            } else {
                System.out.println("Failed to connect to the database.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load JDBC driver: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
        return connection;
    }
    public static void insertDataIntoDb(String sql, String workItemId, String queueName, String state, String status, Object detail, int retry) {
        try (Connection connection = DatabaseUtil.connectDB(Constant.SQL_JDBC_URL, Constant.SQL_USER_NAME, Constant.SQL_PASS_WORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // Set values for the prepared statement
            statement.setString(1, workItemId);
            statement.setString(2, queueName);
            statement.setString(3, state);
            statement.setString(4, status);
            statement.setString(5, String.valueOf(detail));
            statement.setInt(6, retry);

            // Execute the insert statement
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Data inserted successfully.");
            } else {
                System.out.println("Failed to insert data.");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }

    public static void updateDatabase(String column,String value,int id) {
        String updateQuery = "UPDATE RPADev.TheBay_DigOps_Metarename_Box.workitem Set "+column+" = '"+value+"' WHERE id='" + id + "'";

        try (Connection connection = DatabaseUtil.connectDB(Constant.SQL_JDBC_URL, Constant.SQL_USER_NAME, Constant.SQL_PASS_WORD);
             PreparedStatement statement = connection.prepareStatement(updateQuery)) {

            // Execute the insert statement
            int rowsUpdated  = statement.executeUpdate();
            if (rowsUpdated  > 0) {
                System.out.println("Data updated successfully");
            } else {
                System.out.println("Failed to update the data data.");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }

    public static ResultSet fetchDataFromDb(String jdbcUrl, String userName, String password, String sql) {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl, userName, password);
            PreparedStatement statement = connection.prepareStatement(sql);

            // Execute the query and return the ResultSet
            return statement.executeQuery();
        } catch (SQLException e) {
            Log.error("SQL Error: " + e.getMessage());
            System.err.println("SQL Error: " + e.getMessage());
            return null;
        }
    }

    public static void updateDatabaseCustom(String updateQuery) {
        try (Connection connection = DatabaseUtil.connectDB(Constant.SQL_JDBC_URL, Constant.SQL_USER_NAME, Constant.SQL_PASS_WORD);
             PreparedStatement statement = connection.prepareStatement(updateQuery)) {

            // Execute the insert statement
            int rowsUpdated  = statement.executeUpdate();
            if (rowsUpdated  > 0) {
                System.out.println("Data updated successfully");
            } else {
                System.out.println("Failed to update the data data.");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }
}

