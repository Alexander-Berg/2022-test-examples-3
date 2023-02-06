package ru.yandex.market.logistics.test.integration.db;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;

import javax.annotation.Nonnull;

import org.dbunit.dataset.datatype.AbstractDataType;

import static ru.yandex.market.logistics.test.integration.db.PostgresqlDataTypeFactoryExt.INT_ARRAY;

public class ArrayDataType extends AbstractDataType {

    public ArrayDataType() {
        super(INT_ARRAY, Types.OTHER, Array.class, false);
    }

    @Override
    public String typeCast(Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    public int compare(Object o1, Object o2) {
        return Comparator.nullsFirst(this::compareArray).compare(o1, o2);
    }

    private int compareArray(@Nonnull Object t, @Nonnull Object t1) {
        try {
            return Comparator.comparing(String::valueOf).compare(t, t1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getSqlValue(int column, ResultSet resultSet) throws SQLException {
        String value = resultSet.getString(column);
        if (value == null || resultSet.wasNull()) {
            return null;
        }
        return value;
    }

    @Override
    public void setSqlValue(Object value, int column, PreparedStatement statement) throws SQLException {
        if (value == null) {
            statement.setNull(column, Types.OTHER);
            return;
        }
        statement.setObject(column, value.toString(), Types.OTHER);
    }
}
