package ru.geekbrains.cloudAgent.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBAuthenticationProvider implements AuthenticationProvider {
    private static final Logger log = LogManager.getLogger(DBAuthenticationProvider.class);

    private DBConnection dbConnection;

    @Override
    public void init() {
        dbConnection = new DBConnection();
    }


    @Override
    public boolean isUsernameBusy(String username) {
        String query = String.format("select id from users where nickname = '%s';", username);
        try (ResultSet rs = dbConnection.getStmt().executeQuery(query)) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            log.throwing(Level.ERROR, e);
        }
        return false;
    }

    @Override
    public void shutdown() {
        dbConnection.close();
    }
}
