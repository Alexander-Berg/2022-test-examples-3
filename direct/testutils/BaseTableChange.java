package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.Map;

import org.jooq.Named;


public abstract class BaseTableChange {
    private final Map<String, ColumnChange> changedColumns = new HashMap<>();

    public void addChangedColumn(Named columnName, Object beforeValue, Object afterValue) {
        changedColumns.put(columnName.getName(), new ColumnChange(beforeValue, afterValue));
    }

    public void addInsertedColumn(Named columnName, Object afterValue) {
        changedColumns.put(columnName.getName(), new ColumnChange(null, afterValue));
    }

    public void addDeletedColumn(Named columnName, Object beforeValue) {
        changedColumns.put(columnName.getName(), new ColumnChange(beforeValue, null));
    }

    public ColumnChange getColumnValues(Named columnName) {
        return changedColumns.get(columnName.getName());
    }

    Map<String, ColumnChange> getChangedColumns() {
        return changedColumns;
    }
}
