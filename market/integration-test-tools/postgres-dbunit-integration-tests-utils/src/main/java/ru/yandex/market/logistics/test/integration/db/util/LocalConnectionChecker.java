package ru.yandex.market.logistics.test.integration.db.util;

import java.net.InetAddress;
import java.net.URI;
import java.sql.Connection;

import javax.sql.DataSource;

public final class LocalConnectionChecker {

    private LocalConnectionChecker() {
        throw new UnsupportedOperationException();
    }

    public static void checkConnectionLocal(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            InetAddress byName = InetAddress.getByName(new URI(url).getHost());
            if (!byName.isLoopbackAddress()) {
                throw new IllegalStateException("DataSource must be  connection to local machine");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
