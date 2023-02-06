package ru.yandex.market.fulfillment.stockstorage.util.filter;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.filter.IColumnFilter;

public class UpdatedFieldFilter implements IColumnFilter {

    @Override
    public boolean accept(String tableName, Column column) {
        return !column.getColumnName().equalsIgnoreCase("updated");
    }
}
