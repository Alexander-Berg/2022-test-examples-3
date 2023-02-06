package ru.yandex.direct.mysql.ytsync.synchronizator.tables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import ru.yandex.direct.mysql.ytsync.common.compatibility.YtSupport;
import ru.yandex.direct.mysql.ytsync.common.row.FlatRow;
import ru.yandex.direct.mysql.ytsync.common.row.FlatRowView;
import ru.yandex.yt.ytclient.tables.TableSchema;

public class MockTransaction implements YtSupport.Transaction {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    private final ConcurrentMap<String, TableData> tables = new ConcurrentHashMap<>();

    public TableData getTableData(String path) {
        return tables.computeIfAbsent(path, k -> new TableData());
    }

    @Override
    public YtSupport support() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledExecutorService executor() {
        return EXECUTOR;
    }

    @Override
    public boolean isAtomic() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> ping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> abort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> commit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> insertRows(String path, TableSchema schema, List<? extends FlatRowView> rows) {
        getTableData(path).insertedRows.addAll(rows);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> updateRows(String path, TableSchema schema, List<? extends FlatRowView> rows) {
        getTableData(path).updatedRows.addAll(rows);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> deleteRows(String path, TableSchema schema, List<? extends FlatRowView> keys) {
        getTableData(path).deletedKeys.addAll(keys);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> modifyRows(String path, TableSchema schema,
                                              List<? extends FlatRowView> insertedRows,
                                              List<? extends FlatRowView> updatedRows, List<? extends FlatRowView> deletedKeys) {
        TableData tableData = getTableData(path);
        tableData.insertedRows.addAll(insertedRows);
        tableData.updatedRows.addAll(updatedRows);
        tableData.deletedKeys.addAll(deletedKeys);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<FlatRow>> lookupRows(String path, TableSchema keySchema,
                                                       List<? extends FlatRowView> keys, TableSchema resultSchema) {
        TableData tableData = getTableData(path);
        List<FlatRow> results = new ArrayList<>();
        for (FlatRowView key : keys) {
            FlatRow row = tableData.lookupData.get(key);
            if (row != null) {
                results.add(row);
            }
        }
        return CompletableFuture.completedFuture(results);
    }

    public static class TableData {
        public final ConcurrentMap<FlatRowView, FlatRow> lookupData = new ConcurrentHashMap<>();
        public final List<FlatRowView> insertedRows = Collections.synchronizedList(new ArrayList<>());
        public final List<FlatRowView> updatedRows = Collections.synchronizedList(new ArrayList<>());
        public final List<FlatRowView> deletedKeys = Collections.synchronizedList(new ArrayList<>());
    }
}
