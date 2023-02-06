package ru.yandex.market.mbo.yttests;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.operations.utils.ReducerWithKey;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.operations.specs.ReduceSpec;
import ru.yandex.inside.yt.kosher.operations.specs.SortSpec;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.yt.TestYt;

public class ReduceTest extends BaseTests {

    private final Yt yt = new TestYt();

    @Test
    public void simple() {
        // create tables
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, getRows());

        // sort table
        var sortPrimary = yt.operations().sortAndGetOp(SortSpec.builder()
                .setInputTables(inputTable)
                .setOutputTable(inputTable)
                .setSortBy("key", "value")
                .build());
        sortPrimary.awaitAndThrowIfNotSuccess();

        // run reduce
        var reduce = yt.operations().reduceAndGetOp(ReduceSpec.builder()
                .setInputTables(inputTable)
                .setOutputTables(outputTable)
                .setReduceBy("key")
                .setReducer(new ReduceTest.ReduceTestReducer())
                .build());
        reduce.awaitAndThrowIfNotSuccess();

        // assert result
        var value = yt.tables().read(outputTable, YTableEntryTypes.YSON);

        Assertions.assertThat(value)
                .toIterable()
                .containsExactlyInAnyOrder(
                        YTree.mapBuilder().key("key").value("a").key("value").value("1").buildMap(),
                        YTree.mapBuilder().key("key").value("b").key("value").value("23456").buildMap(),
                        YTree.mapBuilder().key("key").value("c").key("value").value("78").buildMap()
                );
    }

    @Test
    public void severalInput() {
        // create tables
        YPath inputTable1 = YPath.simple("//tmp/input_table1");
        YPath inputTable2 = YPath.simple("//tmp/input_table2");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable1, CypressNodeType.TABLE, true, true);
        yt.cypress().create(inputTable2, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable1, YTableEntryTypes.YSON, getRows());
        yt.tables().write(inputTable2, YTableEntryTypes.YSON, getRows());

        // sort table
        var sortPrimary1 = yt.operations().sortAndGetOp(SortSpec.builder()
                .setInputTables(inputTable1)
                .setOutputTable(inputTable1)
                .setSortBy("key", "value")
                .build());
        var sortPrimary2 = yt.operations().sortAndGetOp(SortSpec.builder()
                .setInputTables(inputTable2)
                .setOutputTable(inputTable2)
                .setSortBy("key", "value")
                .build());
        sortPrimary1.awaitAndThrowIfNotSuccess();
        sortPrimary2.awaitAndThrowIfNotSuccess();

        // run reduce
        var reduce = yt.operations().reduceAndGetOp(ReduceSpec.builder()
                .setInputTables(inputTable1, inputTable2)
                .setOutputTables(outputTable)
                .setReduceBy("key", "value")
                .setReducer(new ReduceTest.ReduceTestReducer())
                .build());
        reduce.awaitAndThrowIfNotSuccess();

        // assert result
        var value = yt.tables().read(outputTable, YTableEntryTypes.YSON);
        Assertions.assertThat(value)
                .toIterable()
                .containsExactlyInAnyOrder(
                        YTree.mapBuilder().key("key").value("a").key("value").value("11").buildMap(),
                        YTree.mapBuilder().key("key").value("b").key("value").value("22").buildMap(),
                        YTree.mapBuilder().key("key").value("b").key("value").value("33").buildMap(),
                        YTree.mapBuilder().key("key").value("b").key("value").value("44").buildMap(),
                        YTree.mapBuilder().key("key").value("b").key("value").value("55").buildMap(),
                        YTree.mapBuilder().key("key").value("b").key("value").value("66").buildMap(),
                        YTree.mapBuilder().key("key").value("c").key("value").value("77").buildMap(),
                        YTree.mapBuilder().key("key").value("c").key("value").value("88").buildMap()
                );
    }

    @Test
    public void severalOutputs() {
        // create tables
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable1 = YPath.simple("//tmp/output_table1");
        YPath outputTable2 = YPath.simple("//tmp/output_table2");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, getRows());

        // sort table
        var sortPrimary = yt.operations().sortAndGetOp(SortSpec.builder()
                .setInputTables(inputTable)
                .setOutputTable(inputTable)
                .setSortBy("key", "value")
                .build());
        sortPrimary.awaitAndThrowIfNotSuccess();

        // run reduce
        var reduce = yt.operations().reduceAndGetOp(ReduceSpec.builder()
                .setInputTables(inputTable)
                .setOutputTables(outputTable1, outputTable2)
                .setReduceBy("key")
                .setReducer(new ReduceTest.ReduceTestReducer2())
                .build());
        reduce.awaitAndThrowIfNotSuccess();

        // assert result
        var values1 = yt.tables().read(outputTable1, YTableEntryTypes.YSON);
        var values2 = yt.tables().read(outputTable2, YTableEntryTypes.YSON);

        Assertions.assertThat(values1)
                .toIterable()
                .containsExactlyInAnyOrder(
                        YTree.mapBuilder().key("key").value("b").key("value").value("23456").buildMap()
                );
        Assertions.assertThat(values2)
                .toIterable()
                .containsExactlyInAnyOrder(
                        YTree.mapBuilder().key("key").value("a").key("value").value("1").buildMap(),
                        YTree.mapBuilder().key("key").value("c").key("value").value("78").buildMap()
                );
    }

    @Test
    public void testThrowNotSortedException() {
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, getRows());

        Assertions.assertThatThrownBy(() -> {
            Operation operation = yt.operations().reduceAndGetOp(ReduceSpec.builder()
                    .setInputTables(inputTable)
                    .setOutputTables(outputTable)
                    .setReduceBy("key")
                    .setReducer(new ReduceTest.ReduceTestReducer())
                    .build());
            operation.awaitAndThrowIfNotSuccess();
        }).hasMessageContaining("Input table //tmp/input_table is not sorted");
    }

    @Test
    public void testThrowNotSortedCorrectlyException() {
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, getRows());

        var sortPrimary = yt.operations().sortAndGetOp(SortSpec.builder()
                .setInputTables(inputTable)
                .setOutputTable(inputTable)
                .setSortBy("value")
                .build());
        sortPrimary.awaitAndThrowIfNotSuccess();

        Assertions.assertThatThrownBy(() -> {
            Operation operation = yt.operations().reduceAndGetOp(
                    ReduceSpec.builder()
                            .setInputTables(inputTable)
                            .setOutputTables(outputTable)
                            .setReduceBy("key", "value")
                            .setReducer(new ReduceTest.ReduceTestReducer())
                            .build()
            );
            operation.awaitAndThrowIfNotSuccess();
        }).hasMessageContaining("Input table //tmp/input_table is sorted by columns [value] " +
                "that are not compatible with the requested columns [key, value]");
    }

    public static class ReduceTestReducer implements ReducerWithKey<YTreeMapNode, YTreeMapNode, String> {
        @Override
        public String key(YTreeMapNode entry) {
            return entry.getString("key");
        }

        @Override
        public void reduce(String key, Iterator<YTreeMapNode> entries, Yield<YTreeMapNode> yield, Statistics stats) {
            String resultValue = StreamSupport.stream(((Iterable<YTreeMapNode>) () -> entries).spliterator(), false)
                    .map(node -> node.getLong("value"))
                    .map(String::valueOf)
                    .collect(Collectors.joining());
            YTreeMapNode result = YTree.mapBuilder()
                    .key("key").value(key)
                    .key("value").value(resultValue)
                    .buildMap();
            yield.yield(result);
        }
    }

    public static class ReduceTestReducer2 implements ReducerWithKey<YTreeMapNode, YTreeMapNode, String> {
        @Override
        public String key(YTreeMapNode entry) {
            return entry.getString("key");
        }

        @Override
        public void reduce(String key, Iterator<YTreeMapNode> entries, Yield<YTreeMapNode> yield, Statistics stats) {
            String resultValue = StreamSupport.stream(((Iterable<YTreeMapNode>) () -> entries).spliterator(), false)
                    .map(node -> node.getLong("value"))
                    .map(String::valueOf)
                    .collect(Collectors.joining());
            YTreeMapNode result = YTree.mapBuilder()
                    .key("key").value(key)
                    .key("value").value(resultValue)
                    .buildMap();
            if (key.charAt(0) % 2 == 0) {
                yield.yield(0, result);
            } else {
                yield.yield(1, result);
            }
        }
    }

    private static List<YTreeMapNode> getRows() {
        return List.of(
                YTree.mapBuilder()
                        .key("key").value("a")
                        .key("value").value(1)
                        .buildMap(),
                YTree.mapBuilder()
                        .key("key").value("b")
                        .key("value").value(2)
                        .buildMap(),
                YTree.mapBuilder()
                        .key("key").value("b")
                        .key("value").value(3)
                        .buildMap(),
                YTree.mapBuilder()
                        .key("key").value("b")
                        .key("value").value(4)
                        .buildMap(),
                YTree.mapBuilder()
                        .key("key").value("b")
                        .key("value").value(5)
                        .buildMap(),
                YTree.mapBuilder()
                        .key("key").value("b")
                        .key("value").value(6)
                        .buildMap(),
                YTree.mapBuilder()
                        .key("key").value("c")
                        .key("value").value(7)
                        .buildMap(),
                YTree.mapBuilder()
                        .key("key").value("c")
                        .key("value").value(8)
                        .buildMap()
        );
    }
}
