package ru.yandex.market.mbo.yt;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.operations.OperationImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.OperationContext;
import ru.yandex.inside.yt.kosher.operations.OperationProgress;
import ru.yandex.inside.yt.kosher.operations.OperationStatus;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.operations.map.Mapper;
import ru.yandex.inside.yt.kosher.operations.reduce.Reducer;
import ru.yandex.inside.yt.kosher.operations.specs.CommandSpec;
import ru.yandex.inside.yt.kosher.operations.specs.EraseSpec;
import ru.yandex.inside.yt.kosher.operations.specs.JoinReduceSpec;
import ru.yandex.inside.yt.kosher.operations.specs.MapReduceSpec;
import ru.yandex.inside.yt.kosher.operations.specs.MapSpec;
import ru.yandex.inside.yt.kosher.operations.specs.MapperSpec;
import ru.yandex.inside.yt.kosher.operations.specs.MergeSpec;
import ru.yandex.inside.yt.kosher.operations.specs.ReduceSpec;
import ru.yandex.inside.yt.kosher.operations.specs.ReducerSpec;
import ru.yandex.inside.yt.kosher.operations.specs.RemoteCopySpec;
import ru.yandex.inside.yt.kosher.operations.specs.SortSpec;
import ru.yandex.inside.yt.kosher.operations.specs.UserJobSpec;
import ru.yandex.inside.yt.kosher.operations.specs.VanillaSpec;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.yt.operations.TestYtMapReduceUtils;
import ru.yandex.market.mbo.yt.operations.TestYtMapUtils;
import ru.yandex.market.mbo.yt.operations.TestYtReduceUtils;
import ru.yandex.market.mbo.yt.operations.TestYtSortUtils;
import ru.yandex.market.mbo.yt.utils.StatisticsStub;
import ru.yandex.market.mbo.yt.utils.YieldStub;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.GetNode;
import ru.yandex.yt.ytclient.proxy.request.ResumeOperation;
import ru.yandex.yt.ytclient.proxy.request.SetNode;
import ru.yandex.yt.ytclient.proxy.request.SuspendOperation;
import ru.yandex.yt.ytclient.proxy.request.TransactionalOptions;
import ru.yandex.yt.ytclient.proxy.request.UpdateOperationParameters;

/**
 * @author s-ermakov
 */
public class TestYtOperations implements YtOperations {

    private static final Mapper<YTreeMapNode, YTreeMapNode> IDENTITY_MAPPER = new Mapper<YTreeMapNode, YTreeMapNode>() {
        @Override
        public void map(YTreeMapNode input, Yield<YTreeMapNode> yield, Statistics sts, OperationContext context) {
            yield.yield(input);
        }
    };

    private final Map<String, UserJobSpec> commandSpecs = new HashMap<>();

    private final TestYt testYt;
    private final TestYtTables tables;
    private final TestCypress cypress;

    TestYtOperations(TestYt testYt) {
        this.testYt = testYt;
        this.tables = testYt.tables();
        this.cypress = testYt.cypress();
    }

    /**
     * Mocks raw command to other specs.
     */
    public void mockCommandSpec(String command, ReducerSpec reducerSpec) {
        commandSpecs.put(command, reducerSpec);
    }

    @Override
    public OperationStatus status(GUID operationId) {
        return OperationStatus.COMPLETED;
    }

    @Override
    public OperationProgress progress(GUID operationId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public YTreeNode result(GUID operationId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GUID merge(Optional<GUID> transactionId, boolean pingAncestorTransactions, MergeSpec spec) {
        List<YPath> inputTables = spec.getInputTables();
        YPath outputTable = spec.getOutputTable();

        Preconditions.checkArgument(!inputTables.isEmpty(), "No input tables are passed");
        Preconditions.checkArgument(outputTable != null, "No output table is passed");
        Preconditions.checkArgument(inputTables.size() == 1, "Currently supported only single input table");
        TransactionalOptions to = new TransactionalOptions().setTransactionId(transactionId.orElse(null));
        YPath inputTable = inputTables.get(0);

        // just copy-paste data
        List<YTreeMapNode> nodes = new ArrayList<>();
        tables.read(transactionId, false, inputTable, YTableEntryTypes.YSON,
                (Consumer<YTreeMapNode>) nodes::add);
        cypress.create(new CreateNode(outputTable, CypressNodeType.TABLE)
                .setTransactionalOptions(to)
                .setIgnoreExisting(true));

        // save sort attribute
        if (cypress.get(new GetNode(inputTable.attribute("sorted")).setTransactionalOptions(to)).boolValue()) {
            YTreeNode sortedBy = cypress
                    .get(new GetNode(inputTable.attribute("sorted_by")).setTransactionalOptions(to));

            cypress.set(new SetNode(outputTable.attribute("sorted"), YTree.booleanNode(true))
                    .setTransactionalOptions(to)
                    .setForce(true));

            cypress.set(new SetNode(outputTable.attribute("sorted_by"), sortedBy).setTransactionalOptions(to));
        }
        tables.write(transactionId, false, outputTable, YTableEntryTypes.YSON, nodes.iterator());

        return transactionId.orElse(GUID.create());
    }

    @Override
    public GUID erase(Optional<GUID> transactionId, boolean pingAncestorTransactions, EraseSpec spec) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GUID map(Optional<GUID> transactionId, boolean pingAncestorTransactions, MapSpec spec) {
        List<YPath> inputTables = spec.getInputTables();
        List<YPath> outputTables = spec.getOutputTables();

        MapperSpec mapperSpec = (MapperSpec) spec.getMapperSpec();
        Mapper<YTreeMapNode, YTreeMapNode> mapper = getField(mapperSpec, "mapperOrReducer", Mapper.class);

        Preconditions.checkArgument(!inputTables.isEmpty(), "No input tables are passed");
        Preconditions.checkArgument(!outputTables.isEmpty(), "No output tables are passed");

        // run map operation
        List<YTreeMapNode> inputRows = new ArrayList<>();
        for (int i = 0; i < inputTables.size(); i++) {
            tables.read(transactionId, false, inputTables.get(i), YTableEntryTypes.YSON,
                    (Consumer<YTreeMapNode>) inputRows::add);
        }

        YieldStub<YTreeMapNode> resultYield = TestYtMapUtils.run(mapper, inputRows);

        for (int i = 0; i < outputTables.size(); i++) {
            tables.write(transactionId, false, outputTables.get(i), YTableEntryTypes.YSON,
                    resultYield.getOut(i).iterator());
        }

        return transactionId.orElse(GUID.create());
    }

    @Override
    public GUID reduce(Optional<GUID> transactionId, boolean pingAncestorTransactions, ReduceSpec spec) {
        // validate input
        List<YPath> allInputTables = spec.getInputTables();
        List<YPath> outputTables = spec.getOutputTables();
        List<String> reduceBy = spec.getReduceBy();
        List<String> joinBy = spec.getJoinBy();
        boolean enableKeyGuarantee = Optional.ofNullable(spec.getAdditionalSpecParameters().get("enable_key_guarantee"))
                .map(YTreeNode::boolValue).orElse(true);
        Map<Boolean, List<YPath>> inputForeign = allInputTables.stream()
                .collect(Collectors.groupingBy(yPath -> yPath.getForeign().orElse(false)));
        List<YPath> inputTables = inputForeign.getOrDefault(false, Collections.emptyList());
        List<YPath> foreignTables = inputForeign.get(true);

        UserJobSpec reducerSpec = spec.getReducerSpec();
        if (reducerSpec instanceof CommandSpec) {
            String command = ((CommandSpec) reducerSpec).getCommand();
            reducerSpec = commandSpecs.get(command);
            if (reducerSpec == null) {
                throw new RuntimeException("No spec for command: " + command + ". " +
                        "Current commands: " + commandSpecs.keySet());
            }
        }
        if (!(reducerSpec instanceof ReducerSpec)) {
            throw new IllegalStateException("Expected to be ReduceSpec type. Actual is: " + reducerSpec.getClass());
        }

        Reducer<YTreeMapNode, YTreeMapNode> reducer = getField(reducerSpec, "mapperOrReducer", Reducer.class);

        Preconditions.checkArgument(!allInputTables.isEmpty(), "No input tables are passed");
        Preconditions.checkArgument(!outputTables.isEmpty(), "No output tables are passed");

        if (foreignTables != null && joinBy.isEmpty()) {
            throw new IllegalStateException("It is required to specify join_by when using foreign tables");
        }
        if (!joinBy.isEmpty() && foreignTables == null) {
            throw new IllegalStateException("At least one foreign input table is required when join_by is specified");
        }

        // check if all input tables are sorted
        checkTablesAreSortedByColumnsPrefix(transactionId, reduceBy, inputTables);

        if (!joinBy.isEmpty()) {
            if (enableKeyGuarantee && !startsWith(reduceBy, joinBy)) {
                throw new IllegalStateException(String.format("Join key columns (%s) are not compatible " +
                        "with reduce key columns (%s)", joinBy, reduceBy));
            }

            // check if all foreign tables are sorted
            checkTablesAreSortedByColumnsPrefix(transactionId, joinBy, foreignTables);

            // run reduce operation with foreign table
            Map<Integer, Iterator<YTreeMapNode>> foreignRows = new HashMap<>();
            for (YPath foreignTable : foreignTables) {
                Iterator<YTreeMapNode> values = tables.read(transactionId, false,
                        foreignTable, YTableEntryTypes.YSON);
                foreignRows.put(allInputTables.indexOf(foreignTable), values);
            }
            Map<Integer, Iterator<YTreeMapNode>> inputRows = new HashMap<>();
            for (YPath inputTable : inputTables) {
                Iterator<YTreeMapNode> values = tables.read(transactionId, false,
                        inputTable, YTableEntryTypes.YSON);
                inputRows.put(allInputTables.indexOf(inputTable), values);
            }

            YieldStub<YTreeMapNode> yield = new YieldStub<>();
            StatisticsStub statisticsStub = new StatisticsStub();
            TestYtReduceUtils.run(reducer, reduceBy, joinBy, enableKeyGuarantee,
                    foreignRows, inputRows,
                    yield, statisticsStub);

            for (int i = 0; i < outputTables.size(); i++) {
                tables.write(transactionId, false, outputTables.get(i), YTableEntryTypes.YSON,
                        yield.getOut(i).iterator());
            }
        } else {
            // run reduce operation
            Map<Integer, Iterator<YTreeMapNode>> inputRows = new HashMap<>();
            for (YPath inputTable : inputTables) {
                Iterator<YTreeMapNode> values = tables.read(transactionId, false,
                        inputTable, YTableEntryTypes.YSON);
                inputRows.put(allInputTables.indexOf(inputTable), values);
            }

            YieldStub<YTreeMapNode> resultYield = TestYtReduceUtils.run(reducer, reduceBy, inputRows);
            for (int i = 0; i < outputTables.size(); i++) {
                tables.write(transactionId, false, outputTables.get(i), YTableEntryTypes.YSON,
                        resultYield.getOut(i).iterator());
            }
        }

        return transactionId.orElse(GUID.create());
    }

    private void checkTablesAreSortedByColumnsPrefix(Optional<GUID> transactionId, List<String> columns,
                                                     List<YPath> tables) {
        TransactionalOptions to = new TransactionalOptions(transactionId.orElse(null));
        for (YPath table : tables) {
            YTreeNode sorted = cypress.get(new GetNode(table.attribute("sorted")).setTransactionalOptions(to));
            if (sorted == null || !sorted.boolValue()) {
                throw new IllegalStateException("Input table " + table + " is not sorted");
            }

            List<String> sortedBy = cypress.get(new GetNode(table.attribute("sorted_by")).setTransactionalOptions(to))
                    .asList().stream()
                    .map(YTreeNode::stringValue)
                    .collect(Collectors.toList());

            Map<String, String> renameColumns = table.getRenameColumns();

            sortedBy = sortedBy.stream()
                    .map(s -> renameColumns.getOrDefault(s, s))
                    .collect(Collectors.toList());

            boolean compatible = startsWith(sortedBy, columns);
            if (!compatible) {
                throw new IllegalStateException("Input table " + table + " is sorted by columns " + sortedBy + " " +
                        "that are not compatible with the requested columns " + columns);
            }
        }
    }

    @Override
    public GUID joinReduce(Optional<GUID> transactionId, boolean pingAncestorTransactions, JoinReduceSpec spec) {
        throw new IllegalStateException("join reduce is deprecated: https://clubs.at.yandex-team.ru/yt/3587");
    }

    @Override
    public GUID sort(Optional<GUID> transactionId, boolean pingAncestorTransactions, SortSpec spec) {
        TransactionalOptions to = new TransactionalOptions(transactionId.orElse(null));
        List<YPath> inputTables = spec.getInputTables();
        YPath outputTable = spec.getOutputTable();
        List<String> sortBy = spec.getSortBy();

        Preconditions.checkArgument(!sortBy.isEmpty(), "No sort by columns");
        Preconditions.checkArgument(!inputTables.isEmpty(), "No input tables are passed");
        Preconditions.checkArgument(outputTable != null, "No output table is passed");
        Preconditions.checkArgument(inputTables.size() == 1, "Currently supported only single input table");

        // run sort operation
        List<YTreeMapNode> inputRows = new ArrayList<>();
        tables.read(transactionId, false, inputTables.get(0), YTableEntryTypes.YSON,
                (Consumer<YTreeMapNode>) inputRows::add);
        YieldStub<YTreeMapNode> resultYield = TestYtSortUtils.run(sortBy, inputRows);
        tables.write(transactionId, false, outputTable, YTableEntryTypes.YSON,
                resultYield.getOut().iterator());

        // set sorted attributes
        cypress.set(new SetNode(outputTable.attribute("sorted"), YTree.booleanNode(true)).setTransactionalOptions(to));
        YTreeNode sortByYTree = YTree.builder().value(sortBy).build();
        cypress.set(new SetNode(outputTable.attribute("sorted_by"), sortByYTree).setTransactionalOptions(to));

        return transactionId.orElse(GUID.create());
    }

    @Override
    public GUID mapReduce(Optional<GUID> transactionId, boolean pingAncestorTransactions, MapReduceSpec spec) {
        List<YPath> inputTables = spec.getInputTables();
        List<YPath> outputTables = spec.getOutputTables();
        List<String> sortBy = spec.getSortBy();
        List<String> reduceBy = spec.getReduceBy();

        MapperSpec mapperSpec = (MapperSpec) spec.getMapperSpec().orElse((MapperSpec) null);
        ReducerSpec reducerSpec = (ReducerSpec) spec.getReducerSpec();
        Mapper<YTreeMapNode, YTreeMapNode> mapper = getField(mapperSpec, "mapperOrReducer", Mapper.class);
        Reducer<YTreeMapNode, YTreeMapNode> reducer = getField(reducerSpec, "mapperOrReducer", Reducer.class);

        Preconditions.checkArgument(!inputTables.isEmpty(), "No input tables are passed");
        Preconditions.checkArgument(!outputTables.isEmpty(), "No output tables are passed");

        // run map-reduce operation
        Map<Integer, Iterator<YTreeMapNode>> inputRows = new HashMap<>();
        for (YPath inputTable : inputTables) {
            Iterator<YTreeMapNode> values = tables.read(transactionId, false,
                    inputTable, YTableEntryTypes.YSON);
            inputRows.put(inputTables.indexOf(inputTable), values);
        }

        YieldStub<YTreeMapNode> resultYield = TestYtMapReduceUtils.run(mapper, reducer, sortBy, reduceBy, inputRows);

        for (int i = 0; i < outputTables.size(); i++) {
            tables.write(transactionId, false, outputTables.get(i), YTableEntryTypes.YSON,
                    resultYield.getOut(i).iterator());
        }

        return transactionId.orElse(GUID.create());
    }

    @Override
    public GUID remoteCopy(RemoteCopySpec spec) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void abort(GUID operationId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Operation getOperation(GUID operationId) {
        return new OperationImpl(operationId, "unit-tests.host.com", Duration.ofMillis(1), testYt);
    }

    @Override
    public Map<String, YTreeNode> getOperationInfo(GUID operationId, Duration timeout, Collection<String> attributes) {
        return new HashMap<String, YTreeNode>() {{
            put("state", YTree.stringNode(status(operationId).name()));
            put("operation_type", YTree.node("unit-test"));
        }};
    }

    @Override
    public GUID vanilla(Optional<GUID> transactionId, boolean pingAncestorTransactions, VanillaSpec spec) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public GUID vanilla(VanillaSpec spec) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Map<String, YTreeNode> listJobs(GUID operationId, Map<String, YTreeNode> attributes) {
        return Collections.singletonMap("jobs", new YTreeListNodeImpl(null));
    }

    @Override
    public String getJobStderr(GUID operationId, GUID jobId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void suspendOperation(SuspendOperation req) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void resumeOperation(ResumeOperation req) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void updateOperationParameters(UpdateOperationParameters operationParameters) {
        throw new RuntimeException("not implemented");
    }


    @SuppressWarnings("unchecked")
    private <T> T getField(@Nullable UserJobSpec spec, String fieldName, Class<T> tClass) {
        if (spec == null) {
            return null;
        }
        try {
            Class<?> clazz = spec.getClass();
            Field field = null;
            while (clazz != Object.class && field == null) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException skip) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (field == null) {
                throw new RuntimeException("No " + fieldName + " in " + spec);
            }

            field.setAccessible(true);
            return (T) field.get(spec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean startsWith(List<String> sortedBy, List<String> reduceBy) {
        if (reduceBy.size() > sortedBy.size()) {
            return false;
        }
        for (int i = 0; i < reduceBy.size(); i++) {
            String reduceColumn = reduceBy.get(i);
            String sortedColumn = sortedBy.get(i);
            if (!reduceColumn.equals(sortedColumn)) {
                return false;
            }
        }
        return true;
    }
}
