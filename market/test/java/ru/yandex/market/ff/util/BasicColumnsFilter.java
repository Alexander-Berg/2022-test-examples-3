package ru.yandex.market.ff.util;

import com.google.common.collect.ImmutableSet;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.filter.IColumnFilter;

public class BasicColumnsFilter implements IColumnFilter {

    private static final ImmutableSet<String> BASIC_DATE_COLUMNS = ImmutableSet.of("updated", "created", "id");

    @Override
    public boolean accept(String tableName, Column column) {
        return !BASIC_DATE_COLUMNS.contains(column.getColumnName().toLowerCase());
    }
}
