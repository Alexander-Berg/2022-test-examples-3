package ru.yandex.market.logistics.test.integration.db;

import javax.sql.DataSource;

public interface TestDatabaseInitializer {
    void initializeDatabase(DataSource dataSource);
}
