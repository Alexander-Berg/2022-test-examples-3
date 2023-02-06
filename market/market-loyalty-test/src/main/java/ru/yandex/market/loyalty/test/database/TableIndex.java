package ru.yandex.market.loyalty.test.database;

import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

class TableIndex {
    private final String tableName;
    private final boolean unique;
    private final boolean primary;
    private final List<String> columns;
    private final String indexExpression;

    public TableIndex(
            String tableName,
            boolean unique,
            boolean primary,
            String[] columns,
            String indexExpression) {
        this.tableName = tableName;
        this.unique = unique;
        this.primary = primary;
        this.columns = columns != null ? ImmutableList.copyOf(columns) : Collections.emptyList();
        this.indexExpression = indexExpression;
    }

    public String getTableName() {
        return tableName;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isPrimary() {
        return primary;
    }

    public List<String> getColumns() {
        return columns;
    }

    public String getIndexExpression() {
        return indexExpression;
    }

    public int getColumnPosition(String columnName) {
        return getColumns().indexOf(columnName.toLowerCase());
    }
}
