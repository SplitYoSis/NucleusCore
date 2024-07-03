package dev.splityosis.nucleuscore.storage;


import com.zaxxer.hikari.HikariDataSource;
import dev.splityosis.nucleuscore.Nucleus;
import dev.splityosis.nucleuscore.exceptions.UnsupportedDatabaseType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class HikariDatabaseConnectionManager implements DatabaseConnectionManager{

    private Nucleus nucleus;
    private String databaseType;
    private boolean local;
    private String databaseAddress;
    private String databaseName;
    private String username;
    private String password;
    private HikariDataSource hikariDataSource;

    public HikariDatabaseConnectionManager(Nucleus nucleus, String databaseType, String databaseAddress, String databaseName, String username, String password) throws UnsupportedDatabaseType {
        this.nucleus = nucleus;
        if (databaseType == null)
            throw new UnsupportedDatabaseType("null");
        this.databaseType = databaseType.toLowerCase();
        if (Arrays.asList("sqlite", "h2").contains(databaseType.toLowerCase())) {
            local = true;
            return;
        }
        if (!Arrays.asList("mysql", "mariadb").contains(databaseType.toLowerCase()))
            throw new UnsupportedDatabaseType(databaseType);
        local = false;
        this.databaseAddress = databaseAddress;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }

    @Override
    public void setup(){

        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        StringBuilder url = new StringBuilder("jdbc:").append(databaseType).append(":");  // jdbc:MySql
        hikariDataSource = new HikariDataSource();

        if (local) {
            url.append(nucleus.getLocalDatabaseFile().getPath());
            hikariDataSource.setJdbcUrl(url.toString());
        }
        else {
            url.append("//").append(databaseAddress).append("/").append(databaseName);
            hikariDataSource.setJdbcUrl(url.toString());
            hikariDataSource.setUsername(username);
            hikariDataSource.setPassword(password);
        }
        try {
            getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        nucleus.log("&7Database URL: " +url.toString());
    }

    @Override
    public boolean isLocal() {
        return local;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }
}
