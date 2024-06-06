package dev.splityosis.nucleuscore.storage;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnectionManager {

    void setup();

    boolean isLocal();

    Connection getConnection() throws SQLException;

}
