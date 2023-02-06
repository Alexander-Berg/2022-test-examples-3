package ru.yandex.search.mop.server.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.ConnectionInfo;
import io.zonky.test.db.postgres.embedded.DatabasePreparer;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;

import ru.yandex.search.mop.server.config.database.DatabaseConfig;

public class MockConnectionPool extends ConnectionPool {
    private final DataSource dataSource;

    public MockConnectionPool(final DatabaseConfig config) throws Exception {
        super(config);
        DatabasePreparer preparer = LiquibasePreparer.forClasspathLocation(
            "db/changelog/changelog.xml");
        PreparedDbProvider provider =
            PreparedDbProvider.forPreparer(preparer, new CopyOnWriteArrayList<>());
        ConnectionInfo connectionInfo = provider.createNewDatabase();
        dataSource = provider.createDataSourceFromConnectionInfo(connectionInfo);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
