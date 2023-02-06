package ru.yandex.market.mbo.yttests;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.operations.utils.LambdaMapper;
import ru.yandex.inside.yt.kosher.impl.operations.utils.functions.YtFunction3V;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.operations.specs.MapSpec;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.yt.TestYt;

public class MapTests extends BaseTests {
    private final Yt yt = new TestYt();

    @Test
    public void simple() {
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, wrap(
                Map.of("key", "a"),
                Map.of("key", "b"),
                Map.of("key", "c")
        ));

        var operation = yt.operations().mapAndGetOp(MapSpec.builder()
                .setInputTables(inputTable)
                .setOutputTables(outputTable)
                .setMapper(new LambdaMapper<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                        (YtFunction3V<YTreeMapNode, Yield<YTreeMapNode>, Statistics>)
                                (entry, yield, statistics) -> yield.yield(entry)
                ))
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values = yt.tables().read(outputTable, YTableEntryTypes.YSON);
        Assertions.assertThat(values).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("key", "a"),
                Map.of("key", "b"),
                Map.of("key", "c")
        ));
    }

    @Test
    public void severalInputs() {
        YPath inputTable1 = YPath.simple("//tmp/input_table1");
        YPath inputTable2 = YPath.simple("//tmp/input_table2");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable1, CypressNodeType.TABLE, true, true);
        yt.cypress().create(inputTable2, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable1, YTableEntryTypes.YSON, wrap(
                Map.of("key", "a"),
                Map.of("key", "b"),
                Map.of("key", "c")
        ));
        yt.tables().write(inputTable2, YTableEntryTypes.YSON, wrap(
                Map.of("value", "1"),
                Map.of("value", "2"),
                Map.of("value", "3")
        ));

        var operation = yt.operations().mapAndGetOp(MapSpec.builder()
                .setInputTables(inputTable1, inputTable2)
                .setOutputTables(outputTable)
                .setMapper(new LambdaMapper<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                        (YtFunction3V<YTreeMapNode, Yield<YTreeMapNode>, Statistics>)
                                (entry, yield, statistics) -> yield.yield(entry)
                ))
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values = yt.tables().read(outputTable, YTableEntryTypes.YSON);
        Assertions.assertThat(values).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("key", "a"),
                Map.of("key", "b"),
                Map.of("key", "c"),
                Map.of("value", "1"),
                Map.of("value", "2"),
                Map.of("value", "3")
        ));
    }

    @Test
    public void severalOutputs() {
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable1 = YPath.simple("//tmp/output_table1");
        YPath outputTable2 = YPath.simple("//tmp/output_table2");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, wrap(
                Map.of("key", 1),
                Map.of("key", 2),
                Map.of("key", 3)
        ));

        var operation = yt.operations().mapAndGetOp(MapSpec.builder()
                .setInputTables(inputTable)
                .setOutputTables(outputTable1, outputTable2)
                .setMapper(new LambdaMapper<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                        (YtFunction3V<YTreeMapNode, Yield<YTreeMapNode>, Statistics>)
                                (entry, yield, statistics) -> {
                                    var value = entry.getInt("key");
                                    if (value % 2 == 0) {
                                        yield.yield(0, entry);
                                    } else {
                                        yield.yield(1, entry);
                                    }
                                }
                ))
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values1 = yt.tables().read(outputTable1, YTableEntryTypes.YSON);
        Assertions.assertThat(values1).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("key", 2)
        ));
        var values2 = yt.tables().read(outputTable2, YTableEntryTypes.YSON);
        Assertions.assertThat(values2).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("key", 1),
                Map.of("key", 3)
        ));
    }
}
