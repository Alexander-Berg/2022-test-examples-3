package ru.yandex.search.msal.mock;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class MockDriver implements Driver {
    private static final List<TestSuiteResolver> RESOLVERS = new ArrayList<>();

    private final DbMock mock;

    public MockDriver() {
        this.mock = resolve();
    }

    public static synchronized void addResolver(
        final TestSuiteResolver resolver)
    {
        RESOLVERS.add(resolver);
    }

    public static synchronized void removeResolver(
        final TestSuiteResolver resolver)
    {
        RESOLVERS.remove(resolver);
    }

    public static synchronized DbMock resolve() {
        DbMock mock = null;
        for (TestSuiteResolver resolver: RESOLVERS) {
            mock = resolver.apply(Thread.currentThread());
            if (mock != null) {
                return mock;
            }
        }

        throw new RuntimeException("No TestSuite found");
    }

    @Override
    public Connection connect(
        final String url,
        final Properties info)
        throws SQLException
    {
        if (acceptsURL(url)) {
            return new MockConnection(mock.db(url));
        }

        return null;
    }

    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(
        final String url,
        final Properties info)
        throws SQLException
    {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getAnonymousLogger();
    }
}
