package ru.yandex.market.yql_test.checker;

import org.dbunit.dataset.ITable;

import static com.google.common.base.Preconditions.checkNotNull;
import static ru.yandex.market.yql_test.checker.UncheckedTableMetaDataFacade.uncheckedTableMetaDataFacade;
import static ru.yandex.market.yql_test.utils.YqlDbUnitUtils.wrapToUnchecked;

public class UncheckedTableFacade implements ITable {

    private final ITable table;

    public UncheckedTableFacade(ITable table) {
        this.table = table;
    }

    public static UncheckedTableFacade uncheckedTableFacade(ITable table) {
        checkNotNull(table, "table");
        return new UncheckedTableFacade(table);
    }

    @Override
    public UncheckedTableMetaDataFacade getTableMetaData() {
        return uncheckedTableMetaDataFacade(table.getTableMetaData());
    }

    @Override
    public int getRowCount() {
        return table.getRowCount();
    }

    @Override
    public Object getValue(int row, String column) {
        return wrapToUnchecked(() -> table.getValue(row, column));
    }
}
