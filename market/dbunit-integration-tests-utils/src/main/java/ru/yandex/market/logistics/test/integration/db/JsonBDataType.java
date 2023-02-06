package ru.yandex.market.logistics.test.integration.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;

import javax.annotation.Nonnull;

import org.dbunit.dataset.datatype.AbstractDataType;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static ru.yandex.market.logistics.test.integration.db.PostgresqlDataTypeFactoryExt.JSONB;

public class JsonBDataType extends AbstractDataType {

    public JsonBDataType() {
        super(JSONB, Types.OTHER, String.class, false);
    }

    @Override
    public String typeCast(Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    public int compare(Object o1, Object o2) {
        return Comparator.nullsFirst(this::compareJsons).compare(o1, o2);

    }

    private int compareJsons(@Nonnull Object t, @Nonnull Object t1) {
        try {
            return JSONCompare.compareJSON(t.toString(), t1.toString(), JSONCompareMode.STRICT).passed() ? 0 : -1;
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
