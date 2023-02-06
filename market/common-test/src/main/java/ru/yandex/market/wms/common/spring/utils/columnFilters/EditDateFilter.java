package ru.yandex.market.wms.common.spring.utils.columnFilters;


import org.dbunit.dataset.Column;
import org.dbunit.dataset.filter.IColumnFilter;

public class EditDateFilter implements IColumnFilter {

    @Override
    public boolean accept(String tableName, Column column) {
        return !(tableName.equalsIgnoreCase("LOT")
                && column.getColumnName().equalsIgnoreCase("EDITDATE"));
    }
}
