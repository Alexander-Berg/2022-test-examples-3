package ru.yandex.market.yql_test.checker;

import org.dbunit.dataset.DefaultTable;

import static com.google.common.base.Preconditions.checkNotNull;
import static ru.yandex.market.yql_test.utils.YqlDbUnitUtils.wrapToUnchecked;

public class UncheckedDefaultTableFacade {

    private final DefaultTable table;

    public UncheckedDefaultTableFacade(DefaultTable table) {
        this.table = table;
    }

    public static UncheckedDefaultTableFacade uncheckedDefaultTableFacade(DefaultTable table) {
        checkNotNull(table, "table");
        return new UncheckedDefaultTableFacade(table);
    }

    public void addRow() {
        wrapToUnchecked(() -> table.addRow());
    }

    public Object setValue(int row, String column, Object value) {
        return wrapToUnchecked(() -> table.setValue(row, column, value));
    }

    public DefaultTable getOriginalTable() {
        return table;
    }
}
