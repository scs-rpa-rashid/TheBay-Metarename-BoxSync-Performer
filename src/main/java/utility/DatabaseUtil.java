package utility;

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

