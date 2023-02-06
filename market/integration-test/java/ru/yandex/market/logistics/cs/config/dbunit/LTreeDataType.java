package ru.yandex.market.logistics.cs.config.dbunit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.dbunit.dataset.datatype.AbstractDataType;

public class LTreeDataType extends AbstractDataType {
    public static final String LTREE_TYPE = "ltree";

    public LTreeDataType() {
        super(LTREE_TYPE, Types.OTHER, null, false);
    }

    @Override
    public Object typeCast(Object value) {
        return value;
    }

    @Override
    public Object getSqlValue(int column, ResultSet resultSet) throws SQLException {
        return resultSet.getString(column);
    }

    @Override
    public void setSqlValue(Object value, int column, PreparedStatement statement) throws SQLException {
        statement.setObject(column, value, Types.OTHER);
    }
}
