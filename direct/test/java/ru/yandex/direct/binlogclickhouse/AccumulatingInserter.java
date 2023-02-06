package ru.yandex.direct.binlogclickhouse;

import java.util.Optional;

import ru.yandex.direct.mysql.MySQLBinlogState;

public class AccumulatingInserter implements Inserter, BinlogStateGetter {
    private BinlogTransactionsBatch batch;

    public AccumulatingInserter() {
        batch = new BinlogTransactionsBatch();
    }

    public BinlogTransactionsBatch getBatch() {
        return batch;
    }

    @Override
    public void insert(BinlogTransactionsBatch transactions) {
        batch.add(transactions);
    }

    @Override
    public synchronized Optional<MySQLBinlogState> getLastState(String source) {
        return batch.getStateSet().getBySourceName(source);
    }
}
