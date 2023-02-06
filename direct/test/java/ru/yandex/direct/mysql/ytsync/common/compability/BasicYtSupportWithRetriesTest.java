package ru.yandex.direct.mysql.ytsync.common.compability;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.mysql.ytsync.common.compatibility.BasicYtSupport;
import ru.yandex.direct.mysql.ytsync.common.compatibility.BasicYtSupportWithRetries;
import ru.yandex.direct.mysql.ytsync.common.compatibility.YtLock;
import ru.yandex.direct.mysql.ytsync.common.compatibility.YtSupport;
import ru.yandex.direct.mysql.ytsync.common.compatibility.YtSupportViaBasic;
import ru.yandex.direct.mysql.ytsync.common.keys.PivotKeys;
import ru.yandex.direct.mysql.ytsync.common.row.FlatRow;
import ru.yandex.direct.mysql.ytsync.common.row.FlatRowView;
import ru.yandex.inside.yt.kosher.impl.common.YtErrorMapping;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

public class BasicYtSupportWithRetriesTest {
    private static ScheduledExecutorService executor;

    @BeforeClass
    public static void beforeClass() {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterClass
    public static void afterClass() {
        executor.shutdown();
    }

    private BasicYtSupport createBasicSupport() {
        return new BasicYtSupportWithRetries(new MyBasicYtSupport(), Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    private YtSupport createSupport() {
        return new YtSupportViaBasic(createBasicSupport());
    }

    @Test
    public void usingNullTransaction() {
        // Композиция через nullTransaction должна повторить операцию и завершиться успешно
        createBasicSupport().nullTransaction().thenCompose(tx -> tx.insertRows(null, null, null)).join();
    }

    @Test
    public void usingStartTransaction() {
        // Композиция через startTransaction должна повторить операцию и завершиться успешно
        createBasicSupport().startTransaction().thenCompose(tx -> tx.insertRows(null, null, null)).join();
    }

    @Test
    public void usingRunTransaction() {
        // Операция внутри runTransaction должна повториться и завершиться успешно
        createSupport().runTransaction(tx -> tx.insertRows(null, null, null)).join();
    }

    private static class MyBasicYtSupport implements BasicYtSupport {
        @Override
        public ScheduledExecutorService executor() {
            return executor;
        }

        @Override
        public CompletableFuture<Boolean> exists(String path) {
            return null;
        }

        @Override
        public CompletableFuture<YTreeNode> getNode(String path) {
            return null;
        }

        @Override
        public CompletableFuture<YTreeNode> getNode(String path, Set<String> attributes) {
            return null;
        }

        @Override
        public CompletableFuture<Void> setNode(String path, YTreeNode value) {
            return null;
        }

        @Override
        public CompletableFuture<YtLock> lockNodeExclusive(String path, String title, boolean waitable) {
            return null;
        }

        @Override
        public CompletableFuture<Void> remove(String path) {
            return null;
        }

        @Override
        public CompletableFuture<Void> createTable(String path, Map<String, YTreeNode> attributes) {
            return null;
        }

        @Override
        public CompletableFuture<Void> reshardTable(String path, PivotKeys pivotKeys) {
            return null;
        }

        @Override
        public CompletableFuture<Void> mountTable(String path) {
            return null;
        }

        @Override
        public CompletableFuture<Void> unfreezeTable(String path) {
            return null;
        }

        @Override
        public CompletableFuture<Void> unmountTable(String path) {
            return null;
        }

        @Override
        public CompletableFuture<List<FlatRow>> selectRows(String query, TableSchema resultSchema) {
            return null;
        }

        @Override
        public CompletableFuture<UnversionedRowset> selectRows(String query) {
            return null;
        }

        @Override
        public CompletableFuture<? extends BasicTransaction> nullTransaction() {
            return CompletableFuture.completedFuture(new MyBasicTransaction(false));
        }

        @Override
        public CompletableFuture<? extends BasicTransaction> startTransaction() {
            return CompletableFuture.supplyAsync(() -> new MyBasicTransaction(true), executor);
        }

        private class MyBasicTransaction implements BasicTransaction {
            private final AtomicInteger count = new AtomicInteger();
            private final boolean atomic;

            private MyBasicTransaction(boolean atomic) {
                this.atomic = atomic;
            }

            @Override
            public BasicYtSupport support() {
                return MyBasicYtSupport.this;
            }

            @Override
            public boolean isAtomic() {
                return false;
            }

            @Override
            public CompletableFuture<Void> ping() {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> abort() {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> commit() {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> insertRows(String path, TableSchema schema,
                                                      List<? extends FlatRowView> rows) {
                return CompletableFuture.runAsync(() -> {
                    switch (count.incrementAndGet()) {
                        case 1:
                            // В первый раз кидаем исключение
                            throw YtErrorMapping.exceptionFor(1, "whatever");
                        default:
                            // Во все последующие разы завершаемся успешно
                    }
                }, executor);
            }

            @Override
            public CompletableFuture<Void> updateRows(String path, TableSchema schema,
                                                      List<? extends FlatRowView> rows) {
                return null;
            }

            @Override
            public CompletableFuture<Void> deleteRows(String path, TableSchema schema,
                                                      List<? extends FlatRowView> keys) {
                return null;
            }

            @Override
            public CompletableFuture<Void> modifyRows(String path, TableSchema schema,
                                                      List<? extends FlatRowView> insertedRows,
                                                      List<? extends FlatRowView> updatedRows, List<? extends FlatRowView> deletedKeys) {
                return null;
            }

            @Override
            public CompletableFuture<List<FlatRow>> lookupRows(String path, TableSchema keySchema,
                                                               List<? extends FlatRowView> keys, TableSchema resultSchema) {
                return null;
            }
        }
    }
}
