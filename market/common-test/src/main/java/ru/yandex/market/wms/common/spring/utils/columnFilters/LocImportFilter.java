package ru.yandex.market.wms.common.spring.utils.columnFilters;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.filter.IColumnFilter;

public class LocImportFilter implements IColumnFilter {

    @Override
    public boolean accept(String tableName, Column column) {
        return !(column.getColumnName().equalsIgnoreCase("EDITDATE") ||
                column.getColumnName().equalsIgnoreCase("ADDDATE") ||
                column.getColumnName().equalsIgnoreCase("ADDWHO") ||
                column.getColumnName().equalsIgnoreCase("REQUEST_ID") ||
                column.getColumnName().equalsIgnoreCase("EDITWHO"));
    }
}
