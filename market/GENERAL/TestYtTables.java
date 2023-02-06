package ru.yandex.market.mbo.yt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.function.Function;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.Range;
import ru.yandex.inside.yt.kosher.cypress.RangeCriteria;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.http.Compressor;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.tables.ReplicaMode;
import ru.yandex.inside.yt.kosher.tables.TableReaderOptions;
import ru.yandex.inside.yt.kosher.tables.TableWriterOptions;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.yt.util.table.constants.TabletState;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:ParameterNumber")
public class TestYtTables implements YtTables {

    private final TestYt testYt;

    private final NavigableMap<YPath, Table> tables = Collections.synchronizedNavigableMap(
            new TreeMap<>(Comparator.comparing(YPath::toString))
    );

    TestYtTables(TestYt testYt) {
        this.testYt = testYt;
    }

    @Override
    public <T> void write(Optional<GUID> transactionId, boolean pingAncestorTransactions,
                          YPath path, YTableEntryType<T> entryType, Iterator<T> entries, TableWriterOptions options) {
        boolean isAppend = path.getAppend().orElse(false);
        path = path.justPath();
        if (!this.testYt.cypress().exists(transactionId, false, path)) {
            this.testYt.cypress().create(transactionId, false, path,
                    CypressNodeType.TABLE, true, false, Cf.map());
        }
        List<String> sortedBy = this.testYt.cypress()
                .get(transactionId, pingAncestorTransactions, path, Collections.singleton("sorted_by"))
                .getAttribute("sorted_by").map(sb -> sb.asList().stream()
                        .map(YTreeNode::stringValue).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        if (!sortedBy.isEmpty()) {
            List<T> checked = new ArrayList<>();
            CheckSortedFunction<T> function = new CheckSortedFunction<>(sortedBy);

            if (isAppend) {
                this.tables.get(path).getEntries()
                        .forEach((Consumer<T>) e -> checked.add(function.apply(e)));
            }
            while (entries.hasNext()) {
                checked.add(function.apply(entries.next()));
            }
            entries = checked.iterator();
        }

        boolean isStrict = this.testYt.cypress()
                .get(transactionId, pingAncestorTransactions, path, Collections.singleton("strict"))
                .getAttribute("strict").map(YTreeNode::boolValue).orElse(false);
        if (isStrict) {
            Optional<YTreeListNode> schemaO = this.testYt.cypress()
                    .get(transactionId, pingAncestorTransactions, path, Collections.singleton("schema"))
                    .getAttribute("schema").map(YTreeNode::listNode);
            if (!schemaO.isPresent()) {
                throw new IllegalStateException(String.format("Table '%s' is strict, but schema doesn't exists", path));
            }

            Set<String> names = schemaO.get().asList().stream()
                    .map(YTreeNode::asMap)
                    .map(x -> x.get("name").stringValue())
                    .collect(Collectors.toSet());

            CheckSchemaFunction<T> function = new CheckSchemaFunction<>(names);
            List<T> checked = new ArrayList<>();
            while (entries.hasNext()) {
                checked.add(function.apply(entries.next()));
            }
            entries = checked.iterator();
        }

        if (isAppend && this.tables.containsKey(path)) {
            this.tables.get(path).addEntries(entries);
        } else {
            Table<T> table = new Table<>(path, entryType, entries);
            this.tables.put(path, table);
        }
    }

    public <T> List<T> readToList(YPath path, YTableEntryType<T> entryType) {
        List<T> result = new ArrayList<>();
        read(path, entryType, (Consumer<T>) result::add);
        return result;
    }

    public void read(Optional<GUID> transactionId, boolean pingAncestorTransactions, YPath path,
                     YTableEntryType<YTreeMapNode> yson, Consumer<YTreeMapNode> consumer) {
        read(transactionId, pingAncestorTransactions, path, yson, it -> {
            it.forEachRemaining(consumer);
            return null;
        }, null);
    }


    @Override
    public <T, U> U read(Optional<GUID> transactionId, boolean pingAncestorTransactions, YPath path,
                         YTableEntryType<T> entryType, java.util.function.Function<Iterator<T>, U> callback,
                         TableReaderOptions options) {
        Iterator<T> iterator = read(transactionId, pingAncestorTransactions, path, entryType);
        return callback.apply(iterator);
    }

    @Override
    public <T> CloseableIterator<T> read(Optional<GUID> transactionId, boolean pingAncestorTransactions, YPath path,
                                         YTableEntryType<T> entryType, TableReaderOptions options) {
        YPath simplifyPath = path.justPath();

        Table<T> table = this.tables.get(simplifyPath);
        if (table == null) {
            throw new IllegalStateException("No table in " + simplifyPath);
        }

        if (!table.getEntryType().equals(entryType)) {
            throw new IllegalStateException("Table on path " + simplifyPath + " is different type. " +
                    "Expected " + entryType + ", actual " + table.getEntryType());
        }

        Stream<T> stream = table.getEntriesStream();

        Map<String, String> renameColumns = path.getRenameColumns();
        if (!renameColumns.isEmpty()) {
            stream = stream.map(node -> renameColumns(renameColumns, node));
        }

        List<String> columns = path.getColumns();
        if (!columns.isEmpty()) {
            stream = stream.map(node -> selectColumns(node, columns));
        }

        List<RangeCriteria> ranges = path.getRanges();
        if (!ranges.isEmpty()) {
            if (ranges.size() > 1) {
                throw new UnsupportedOperationException("Currently supported only single range. Actual: " + ranges);
            }
            RangeCriteria rangeCriteria = ranges.get(0);
            if (!(rangeCriteria instanceof Range)) {
                throw new UnsupportedOperationException("Currently supported only Range. Actual: " + rangeCriteria);
            }
            Range range = (Range) rangeCriteria;
            stream = stream.filter(node -> filterByRange(range, node));
        }

        return CloseableIterator.wrap(stream.iterator());
    }

    private <T> T renameColumns(Map<String, String> renameColumns, T node) {
        YTreeMapNode mapNode = (YTreeMapNode) node;

        Map<String, YTreeNode> map = mapNode.asMap();
        for (String oldColumn : renameColumns.keySet()) {
            String newColumn = renameColumns.get(oldColumn);

            YTreeNode value = map.remove(oldColumn);
            if (value != null) {
                map.put(newColumn, value);
            }
        }
        return (T) YTree.builder().value(map).build();
    }

    private <T> boolean filterByRange(Range range, T node) {
        YTreeMapNode mapNode = (YTreeMapNode) node;

        // for now only category_id is used for range
        YTreeNode categoryIdNode = mapNode.getOrThrow("category_id");
        int categoryId = categoryIdNode.intValue();

        int fromCategory = range.lower.key.get(0).intValue();
        int toCategory = range.upper.key.get(0).intValue();

        return fromCategory <= categoryId && categoryId < toCategory;
    }

    private <T> T selectColumns(T node, Collection<String> columns) {
        YTreeMapNode mapNode = (YTreeMapNode) node;

        YTreeBuilder yTreeBuilder = YTree.mapBuilder();
        for (String column : columns) {
            mapNode.get(column).ifPresent(value -> {
                yTreeBuilder.key(column).value(value);
            });
        }
        return (T) yTreeBuilder.buildMap();
    }

    @Override
    public <T, U> U selectRows(Optional<GUID> transactionId, String query, Optional<Instant> timestamp,
                               Optional<Integer> inputRowLimit, Optional<Integer> outputRowLimit,
                               boolean enableCodeCache, YTableEntryType<T> entryType,
                               java.util.function.Function<Iterator<T>, U> callback) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public <TInput, TOutput, T> T lookupRows(Optional<GUID> transactionId, YPath path,
                                             Optional<Instant> timestamp, YTableEntryType<TInput> inputType,
                                             Iterable<TInput> keys, YTableEntryType<TOutput> outputType,
                                             java.util.function.Function<Iterator<TOutput>, T> callback,
                                             Compressor compressor) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public <T> void insertRows(Optional<GUID> transactionId, YPath path, boolean update,
                               boolean aggregate, boolean requireSyncReplica,
                               YTableEntryType<T> entryType, Iterator<T> iterator, Compressor compressor) {

        throw new RuntimeException("Not implemented");
    }

    @Override
    public <T> void deleteRows(Optional<GUID> transactionId, YPath path,
                               boolean requireSyncReplica, YTableEntryType<T> entryType,
                               Iterable<T> keys, Compressor compressor) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void trimRows(YPath path, long tabletIndex, long trimmedRowCount) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void mount(YPath path, Optional<Integer> firstTabletIndex,
                      Optional<Integer> lastTabletIndex, Optional<GUID> cellId) {
        testYt.cypress().set(path.attribute("tablet_state"), TabletState.MOUNTED.name().toLowerCase());
    }

    @Override
    public void remount(YPath path, Optional<Integer> firstTabletIndex, Optional<Integer> lastTabletIndex) {
        testYt.cypress().set(path.attribute("tablet_state"), TabletState.MOUNTED.name().toLowerCase());
    }

    @Override
    public void unmount(YPath path, Optional<Integer> firstTabletIndex,
                        Optional<Integer> lastTabletIndex, boolean force) {
        testYt.cypress().set(path.attribute("tablet_state"), TabletState.UNMOUNTED.name().toLowerCase());
    }

    @Override
    public void freeze(YPath path, Optional<Integer> firstTabletIndex,
                       Optional<Integer> lastTabletIndex) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void unfreeze(YPath path, Optional<Integer> firstTabletIndex,
                         Optional<Integer> lastTabletIndex) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void reshard(Optional<GUID> transactionId,
                        boolean pingAncestorTransactions, YPath path,
                        List<List<YTreeNode>> pivotKeys, Optional<Integer> firstTabletIndex,
                        Optional<Integer> lastTabletIndex) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void alterTable(YPath path, Optional<Boolean> dynamic,
                           Optional<YTreeNode> schema, Optional<GUID> upstreamReplicaId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void alterTableReplica(GUID replicaId, Optional<ReplicaMode> replicaMode,
                                  Optional<Boolean> enabled) {
        throw new RuntimeException("Not implemented");
    }

    public void copy(YPath source, YPath destination) {
        Table table = tables.get(source);
        if (table != null) {
            Table<?> copy = new Table<>(destination, table.entryType, table.entries.iterator());
            tables.put(destination, copy);
        }
    }

    public void remove(YPath path) {
        tables.remove(path);
    }

    public void createIfNotExists(YPath path) {
        if (!this.tables.containsKey(path)) {
            this.tables.put(path, new Table<>(path, YTableEntryTypes.YSON, Collections.emptyIterator()));
        }
    }

    private class Table<T> {
        private final YPath yPath;
        private final YTableEntryType<T> entryType;
        private final List<T> entries = new ArrayList<>();

        private Table(YPath yPath, YTableEntryType<T> entryType, Iterator<T> entries) {
            try {
                this.yPath = yPath;
                this.entryType = entryType;
                entries.forEachRemaining(this.entries::add);
            } catch (Exception e) {
                throw new RuntimeException("Exception happened on writing to table: " + yPath, e);
            }
        }

        public YPath getyPath() {
            return yPath;
        }

        public YTableEntryType<T> getEntryType() {
            return entryType;
        }

        public List<T> getEntries() {
            return Collections.unmodifiableList(entries);
        }

        public Stream<T> getEntriesStream() {
            return entries.stream();
        }

        public Iterator<T> getEntriesIterator() {
            return entries.iterator();
        }

        public void addEntries(Iterator<T> entries) {
            entries.forEachRemaining(this.entries::add);
        }
    }

    private static class CheckSortedFunction<T> implements Function<T, T> {
        private List<String> sortedBy;
        private T lastEntry;

        private CheckSortedFunction(List<String> sortedBy) {
            this.sortedBy = sortedBy;
        }

        @Override
        public T apply(T t) {
            if (lastEntry != null) {
                List<Object> lastValues = getValues(lastEntry, sortedBy);
                List<Object> curValues = getValues(t, sortedBy);
                int compare = compare(lastValues, curValues);
                if (compare > 0) {
                    throw new IllegalStateException(String.format("Sorted consistency is broken: (%s) > (%s)",
                            lastValues, curValues));
                }
            }
            lastEntry = t;
            return lastEntry;
        }

        private int compare(List<Object> prevValues, List<Object> curValues) {
            return IntStream.range(0, prevValues.size())
                    .map(i -> {
                        Object prev = prevValues.get(i);
                        Object cur = curValues.get(i);
                        return Comparator.<Comparable<Object>>naturalOrder()
                                .compare((Comparable<Object>) prev, (Comparable<Object>) cur);
                    })
                    .reduce((left, right) -> {
                        if (left == 0) {
                            return right;
                        } else {
                            return left;
                        }
                    })
                    .orElse(0);
        }

        private List<Object> getValues(T entry, List<String> sortedBy) {
            YTreeMapNode mapNode = (YTreeMapNode) entry;
            return sortedBy.stream()
                    .map(key -> {
                        YTreeNode node = mapNode.getOrThrow(key);
                        if (node.isBooleanNode()) {
                            return node.boolValue();
                        }
                        if (node.isDoubleNode()) {
                            return node.doubleNode();
                        }
                        if (node.isIntegerNode()) {
                            return node.longValue();
                        }
                        if (node.isListNode()) {
                            return node.listNode();
                        }
                        if (node.isMapNode()) {
                            return node.asMap();
                        }
                        if (node.isStringNode()) {
                            return node.stringValue();
                        }
                        if (node.isEntityNode()) {
                            return null;
                        }
                        throw new IllegalStateException();
                    })
                    .collect(Collectors.toList());
        }
    }

    private static class CheckSchemaFunction<T> implements Function<T, T> {
        private final Set<String> names;

        CheckSchemaFunction(Set<String> names) {
            this.names = names;
        }

        @Override
        public T apply(T t) {
            if (t instanceof YTreeMapNode) {
                YTreeMapNode node = (YTreeMapNode) t;
                for (String key : node.keys()) {
                    if (!names.contains(key)) {
                        throw new IllegalStateException(String.format("Key '%s' not exists in strict schema: %s. " +
                                "Entry: %s", key, names, node));
                    }
                }
            } else {
                throw new IllegalStateException("Can't check schema of type: " + t);
            }
            return t;
        }
    }
}
