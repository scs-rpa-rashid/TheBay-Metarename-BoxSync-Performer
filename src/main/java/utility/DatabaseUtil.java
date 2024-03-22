package utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {
    private static final String JDBC_URL = "jdbc:sqlserver://thebay-rds-uipath-dev.cyeuvydpkw6m.us-east-1.rds.amazonaws.com:1433;databaseName=TheBayUipathOrchestratorDev;encrypt=true;trustServerCertificate=true";
    private static final String USERNAME = "bayrpasqladmin";
    private static final String PASSWORD = "chlp7#r!b=sWa9&7";

    public static void main(String[] args) {
        try {
            // Load the JDBC driver class
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            // Establish database connection
            try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
                if (connection != null) {
                    System.out.println("Connected to the database!");
                } else {
                    System.out.println("Failed to connect to the database.");
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load JDBC driver: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }
}
