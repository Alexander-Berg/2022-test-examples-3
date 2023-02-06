package ru.yandex.market.common.test.db;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author antipov93.
 */
public enum DatabaseType {

    H2("H2"),
    POSTGRES("PostgreSQL"),
    MYSQL("MySQL"),
    CLICKHOUSE("ClickHouse"),
    ORACLE("Oracle"),
    ;

    private final String databaseProductName;

    DatabaseType(String databaseProductName) {
        this.databaseProductName = databaseProductName;
    }

    public static Optional<DatabaseType> of(String databaseProductName) {
        return Arrays.stream(values())
                .filter(type -> type.databaseProductName.equals(databaseProductName))
                .findFirst();
    }
}
