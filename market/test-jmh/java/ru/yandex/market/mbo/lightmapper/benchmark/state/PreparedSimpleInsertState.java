package ru.yandex.market.mbo.lightmapper.benchmark.state;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class PreparedSimpleInsertState {
    private Connection connection;
    private PreparedStatement statement;

    @Setup
    public void setup(ConnectionState connectionState) throws SQLException {
        connection = connectionState.getDataSource().getConnection();
        statement = connection.prepareStatement(
                "insert into test (name, date, data, mapping_id) values (?, ?, ?, ?) returning id");
    }

    @TearDown
    public void tearDown() throws SQLException {
        statement.close();
        connection.close();
    }

    public Connection getConnection() {
        return connection;
    }

    public PreparedStatement getStatement() {
        return statement;
    }
}
