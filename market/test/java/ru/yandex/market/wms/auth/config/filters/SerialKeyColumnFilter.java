package ru.yandex.market.wms.auth.config.filters;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.filter.IColumnFilter;

public class SerialKeyColumnFilter implements IColumnFilter {

    @Override
    public boolean accept(String tableName, Column column) {
        return !(column.getColumnName().equalsIgnoreCase("SERIALKEY"));
    }
}
