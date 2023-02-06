package ru.yandex.market.fintech.fintechutils.helpers.yt;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.http.Compressor;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.tables.ReplicaMode;
import ru.yandex.inside.yt.kosher.tables.TableReaderOptions;
import ru.yandex.inside.yt.kosher.tables.TableWriterOptions;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

/**
 * Не делает ничего, просто скипает Override интерфейса
 */
public class IntermediateTables implements YtTables {

    @Override
    public <T> void write(Optional<GUID> transactionId, boolean pingAncestorTransactions, YPath path, YTableEntryType<T> entryType, Iterator<T> entries, TableWriterOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T, U> U read(Optional<GUID> transactionId, boolean pingAncestorTransactions, YPath path, YTableEntryType<T> entryType, Function<Iterator<T>, U> callback, TableReaderOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> CloseableIterator<T> read(Optional<GUID> transactionId, boolean pingAncestorTransactions, YPath path,
                                         YTableEntryType<T> entryType, TableReaderOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T, U> U selectRows(Optional<GUID> transactionId, String query, Optional<Instant> timestamp,
                               Optional<Integer> inputRowLimit, Optional<Integer> outputRowLimit,
                               boolean enableCodeCache, YTableEntryType<T> entryType,
                               Function<Iterator<T>, U> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <TInput, TOutput, T> T lookupRows(Optional<GUID> transactionId, YPath path, Optional<Instant> timestamp, YTableEntryType<TInput> inputType, Iterable<TInput> keys, YTableEntryType<TOutput> outputType, Function<Iterator<TOutput>, T> callback, Compressor compressor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void insertRows(Optional<GUID> transactionId, YPath path, boolean update, boolean aggregate,
                               boolean requireSyncReplica, YTableEntryType<T> entryType, Iterator<T> iterator,
                               Compressor compressor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void deleteRows(Optional<GUID> transactionId, YPath path, boolean requireSyncReplica,
                               YTableEntryType<T> entryType, Iterable<T> keys, Compressor compressor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void trimRows(YPath path, long tabletIndex, long trimmedRowCount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void mount(YPath path, Optional<Integer> firstTabletIndex, Optional<Integer> lastTabletIndex,
                      Optional<GUID> cellId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remount(YPath path, Optional<Integer> firstTabletIndex, Optional<Integer> lastTabletIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unmount(YPath path, Optional<Integer> firstTabletIndex, Optional<Integer> lastTabletIndex,
                        boolean force) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void freeze(YPath path, Optional<Integer> firstTabletIndex, Optional<Integer> lastTabletIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unfreeze(YPath path, Optional<Integer> firstTabletIndex, Optional<Integer> lastTabletIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reshard(Optional<GUID> transactionId, boolean pingAncestorTransactions, YPath path, List<List<YTreeNode>> pivotKeys, Optional<Integer> firstTabletIndex, Optional<Integer> lastTabletIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void alterTable(YPath path, Optional<Boolean> dynamic, Optional<YTreeNode> schema, Optional<GUID> upstreamReplicaId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void alterTableReplica(GUID replicaId, Optional<ReplicaMode> replicaMode, Optional<Boolean> enabled) {
        throw new UnsupportedOperationException();
    }
}
