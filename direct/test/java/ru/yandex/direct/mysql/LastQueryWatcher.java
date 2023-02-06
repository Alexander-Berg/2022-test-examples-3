package ru.yandex.direct.mysql;

import java.time.Duration;

public class LastQueryWatcher extends TransactionsCountWatcher {
    private String lastQuery;

    public LastQueryWatcher(MySQLBinlogConsumer consumer) {
        super(consumer);
        this.lastQuery = null;
    }

    @Override
    public void onInsertRows(MySQLSimpleData data) {
        synchronized (this) {
            lastQuery = data.getRowsQuery();
        }
        super.onInsertRows(data);
    }

    @Override
    public void onUpdateRows(MySQLUpdateData data) {
        synchronized (this) {
            lastQuery = data.getRowsQuery();
        }
        super.onUpdateRows(data);
    }

    @Override
    public void onDeleteRows(MySQLSimpleData data) {
        synchronized (this) {
            lastQuery = data.getRowsQuery();
        }
        super.onDeleteRows(data);
    }

    public void waitForQuery(int minTransactionsCount, String expectedLastQuery, Duration timeout) {
        synchronized (this) {
            waitForTransactionsCount(minTransactionsCount, timeout);
            if (!expectedLastQuery.equals(lastQuery)) {
                throw new IllegalStateException("Expected last query: " + expectedLastQuery + ", actual: " + lastQuery);
            }
        }
    }
}
