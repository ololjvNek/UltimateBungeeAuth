package pl.jms.auth.database.mysql;

import pl.jms.auth.database.mysql.modes.StoreMode;

import java.sql.Connection;
import java.sql.ResultSet;

public interface Store
{
    Connection getConnection();

    boolean connect();

    void disconnect();

    void reconnect();

    boolean isConnected();

    ResultSet query(String p0);

    void query(String p0, Callback<ResultSet> p1);

    void update(boolean p0, String p1);

    ResultSet update(String p0);

    StoreMode getStoreMode();
}
