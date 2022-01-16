package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final String dbName = System.getenv("dbName");
    private static final String dbUsername = System.getenv("dbUsername");
    private static final String dbPassword = System.getenv("dbPassword");
    private static final String dbConnectionName = System.getenv("dbConnectionName");

    static HikariDataSource connectionPool;

    /**
     * Creates the connection to the database
     * @return The database connectionpool
     */
    protected static HikariDataSource connect() {
        if (connectionPool != null)
            return connectionPool;

        String jdbcURL = String.format("jdbc:mysql:///%s", dbName);
        Properties connProps = new Properties();
        connProps.setProperty("user", dbUsername);
        connProps.setProperty("password", dbPassword);
        connProps.setProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
        connProps.setProperty("cloudSqlInstance", dbConnectionName);

        // Initialize connection pool
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcURL);
        config.setDataSourceProperties(connProps);
        config.setConnectionTimeout(100000); // 100s
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        connectionPool = new HikariDataSource(config);
        return connectionPool;
    }

    /**
     * Helper to close the stmt and set used by the other DAO objects after finished.
     * @param stmt The PreparedStatement to close.
     * @param set The ResultSet to close.
     */
    protected static void closeStmt(PreparedStatement stmt, ResultSet set) {
        try {
            if (set != null) {
                set.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
