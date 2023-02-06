package ru.yandex.market.fulfillment.stockstorage.util.filter;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.filter.IColumnFilter;

public class FfUpdatedFieldFilter implements IColumnFilter {

    @Override
    public boolean accept(String tableName, Column column) {
        return !column.getColumnName().equalsIgnoreCase("ff_updated");
    }
}
