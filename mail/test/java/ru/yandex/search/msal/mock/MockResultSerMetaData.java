package ru.yandex.search.msal.mock;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MockResultSerMetaData implements ResultSetMetaData {
    private static final int DEFAULT_COLUMN_PRECISION = 60;
    private static final int DEFAULT_COLUMN_SCALE = 0;
    private final String tableName;
    private final List<String> columnNames;
    private final Map<String, DataType> columnTypes;

    public MockResultSerMetaData(
        final String tableName,
        final Map<String, DataType> columnTypes)
    {
        this.columnTypes = new LinkedHashMap<>(columnTypes);
        this.columnNames = new ArrayList<>(columnTypes.keySet());
        this.tableName = tableName;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return columnTypes.size();
    }

    @Override
    public boolean isAutoIncrement(final int column) throws SQLException {
        throw new UnsupportedOperationException("isAutoIncrement");
    }

    @Override
    public boolean isCaseSensitive(final int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isSearchable(final int column) throws SQLException {
        throw new UnsupportedOperationException("isSearchable");
    }

    @Override
    public boolean isCurrency(final int column) throws SQLException {
        throw new UnsupportedOperationException("isCurrency");
    }

    @Override
    public int isNullable(final int column) throws SQLException {
        return columnNoNulls;
    }

    @Override
    public boolean isSigned(final int column) throws SQLException {
        int type = getColumnType(column);
        if (type == DataType.DOUBLE.sqlType()
            || type == DataType.INTEGER.sqlType())
        {
            return true;
        }

        return false;
    }

    @Override
    public int getColumnDisplaySize(final int column) throws SQLException {
        throw new UnsupportedOperationException("getColumnDisplaySize");
    }

    @Override
    public String getColumnLabel(final int column) throws SQLException {
        return getColumnName(column);
    }

    @Override
    public String getColumnName(final int column) throws SQLException {
        return columnNames.get(column - 1);
    }

    @Override
    public String getSchemaName(final int column) throws SQLException {
        return "";
    }

    @Override
    public int getPrecision(final int column) throws SQLException {
        return DEFAULT_COLUMN_PRECISION;
    }

    @Override
    public int getScale(final int column) throws SQLException {
        return DEFAULT_COLUMN_SCALE;
    }

    @Override
    public String getTableName(final int column) throws SQLException {
        return tableName;
    }

    @Override
    public String getCatalogName(final int column) throws SQLException {
        throw new UnsupportedOperationException("getCatalogName");
    }

    @Override
    public int getColumnType(final int column) throws SQLException {
        return columnTypes.get(columnNames.get(column - 1)).sqlType();
    }

    @Override
    public String getColumnTypeName(final int column) throws SQLException {
        return columnTypes.get(columnNames.get(column - 1)).name();
    }

    @Override
    public boolean isReadOnly(final int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isWritable(final int column) throws SQLException {
        return true;
    }

    @Override
    public boolean isDefinitelyWritable(final int column) throws SQLException {
        throw new UnsupportedOperationException("isDefinitelyWritable");
    }

    @Override
    public String getColumnClassName(final int column) throws SQLException {
        return columnTypes.get(columnNames.get(column - 1)).clazz().getName();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("unwrap");
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("isWrapperFor");
    }
}
