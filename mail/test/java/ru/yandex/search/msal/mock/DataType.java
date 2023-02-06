package ru.yandex.search.msal.mock;

import java.sql.Types;
import java.util.Date;

public enum DataType {
    VARCHAR(Types.VARCHAR, String.class),
    INTEGER(Types.INTEGER, Integer.class),
    DOUBLE(Types.DOUBLE, Double.class),
    DATE(Types.DATE, Date.class),
    TIME(Types.TIME, Date.class),
    TIMESTAMP(Types.TIMESTAMP, Date.class);

    private final int sqlType;
    private final Class<?> clazz;

    DataType(final int sqlType, final Class<?> clazz) {
        this.sqlType = sqlType;
        this.clazz = clazz;
    }

    public static DataType fromString(final String spec) {
        return valueOf(spec.toUpperCase());
    }

    public int sqlType() {
        return sqlType;
    }

    public Class<?> clazz() {
        return clazz;
    }
}
