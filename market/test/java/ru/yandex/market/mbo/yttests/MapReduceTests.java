package ru.yandex.market.mbo.yttests;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.operations.utils.LambdaMapper;
import ru.yandex.inside.yt.kosher.impl.operations.utils.LambdaReducerWithKey;
import ru.yandex.inside.yt.kosher.impl.operations.utils.functions.YtFunction;
import ru.yandex.inside.yt.kosher.impl.operations.utils.functions.YtFunction3V;
import ru.yandex.inside.yt.kosher.impl.operations.utils.functions.YtFunction4V;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.operations.specs.MapReduceSpec;
import ru.yandex.inside.yt.kosher.operations.specs.MapperSpec;
import ru.yandex.inside.yt.kosher.operations.specs.ReducerSpec;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.yt.TestYt;

public class MapReduceTests extends BaseTests {
    private final Yt yt = new TestYt();

    @Test
    public void simple() {
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, wrap(
                Map.of("name", "Sergey"),
                Map.of("name", "Sasha"),
                Map.of("name", "Maxim")
        ));

        var operation = yt.operations().mapReduceAndGetOp(MapReduceSpec.builder()
                .setInputTables(inputTable)
                .setOutputTables(outputTable)
                .setMapperSpec(MapperSpec.builder()
                        .setMapper(new LambdaMapper<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction3V<YTreeMapNode, Yield<YTreeMapNode>, Statistics>) (entry, yield,
                                                                                               statistics) -> {
                                    // первая буква имени
                                    String name = entry.getString("name");
                                    entry.put("letter", YTree.node(name.substring(0, 1)));
                                    yield.yield(entry);
                                }))
                        .build()
                )
                .setReduceBy("letter")
                .setReducerSpec(ReducerSpec.builder()
                        .setReducer(new LambdaReducerWithKey<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction<YTreeMapNode, String>) entries -> {
                                    return entries.getString("letter");
                                },
                                (YtFunction4V<String, Iterator<YTreeMapNode>, Yield<YTreeMapNode>, Statistics>)
                                        (key, entry, yield, statistics) -> {
                                            // группируем имена
                                            String names = toStream(entry)
                                                    .map(v -> v.getString("name"))
                                                    .sorted()
                                                    .collect(Collectors.joining(", "));
                                            yield.yield(YTree.mapBuilder().key("names").value(names).buildMap());
                                        }))
                        .build())
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values = yt.tables().read(outputTable, YTableEntryTypes.YSON);
        Assertions.assertThat(values).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("names", "Sasha, Sergey"),
                Map.of("names", "Maxim")
        ));
    }

    @Test
    public void mapReduceWithoutMapper() {
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, wrap(
                Map.of("letter", "S", "name", "Sergey"),
                Map.of("letter", "S", "name", "Sasha"),
                Map.of("letter", "M", "name", "Maxim")
        ));

        var operation = yt.operations().mapReduceAndGetOp(MapReduceSpec.builder()
                .setInputTables(inputTable)
                .setOutputTables(outputTable)
                .setReduceBy("letter")
                .setReducerSpec(ReducerSpec.builder()
                        .setReducer(new LambdaReducerWithKey<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction<YTreeMapNode, String>) entries -> {
                                    return entries.getString("letter");
                                },
                                (YtFunction4V<String, Iterator<YTreeMapNode>, Yield<YTreeMapNode>, Statistics>)
                                        (key, entry, yield, statistics) -> {
                                            // группируем имена
                                            String names = toStream(entry)
                                                    .map(v -> v.getString("name"))
                                                    .sorted()
                                                    .collect(Collectors.joining(", "));
                                            yield.yield(YTree.mapBuilder().key("names").value(names).buildMap());
                                        }))
                        .build())
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values = yt.tables().read(outputTable, YTableEntryTypes.YSON);
        Assertions.assertThat(values).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("names", "Sasha, Sergey"),
                Map.of("names", "Maxim")
        ));
    }

    @Test
    public void mapReduceWithSeveralInput() {
        YPath inputTable1 = YPath.simple("//tmp/input_table1");
        YPath inputTable2 = YPath.simple("//tmp/input_table2");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable1, CypressNodeType.TABLE, true, true);
        yt.cypress().create(inputTable2, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable1, YTableEntryTypes.YSON, wrap(
                Map.of("name", "Sergey"),
                Map.of("name", "Sasha"),
                Map.of("name", "Maxim")
        ));
        yt.tables().write(inputTable2, YTableEntryTypes.YSON, wrap(
                Map.of("name", "Vera"),
                Map.of("name", "Nadeshda"),
                Map.of("name", "Lubov")
        ));

        var operation = yt.operations().mapReduceAndGetOp(MapReduceSpec.builder()
                .setInputTables(inputTable1, inputTable2)
                .setOutputTables(outputTable)
                .setMapperSpec(MapperSpec.builder()
                        .setMapper(new LambdaMapper<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction3V<YTreeMapNode, Yield<YTreeMapNode>, Statistics>) (entry, yield,
                                                                                               statistics) -> {
                                    // первая буква имени
                                    String name = entry.getString("name");
                                    entry.put("letter", YTree.node(name.substring(0, 1)));
                                    entry.clearAttributes();
                                    yield.yield(entry);
                                }))
                        .build()
                )
                .setReduceBy("letter")
                .setReducerSpec(ReducerSpec.builder()
                        .setReducer(new LambdaReducerWithKey<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction<YTreeMapNode, String>) entries -> {
                                    return entries.getString("letter");
                                },
                                (YtFunction4V<String, Iterator<YTreeMapNode>, Yield<YTreeMapNode>, Statistics>)
                                        (key, entry, yield, statistics) -> {
                                            // группируем имена
                                            String names = toStream(entry)
                                                    .map(v -> v.getString("name"))
                                                    .sorted()
                                                    .collect(Collectors.joining(", "));
                                            yield.yield(YTree.mapBuilder().key("names").value(names).buildMap());
                                        }))
                        .build())
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values = yt.tables().read(outputTable, YTableEntryTypes.YSON);
        Assertions.assertThat(values).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("names", "Sasha, Sergey"),
                Map.of("names", "Maxim"),
                Map.of("names", "Vera"),
                Map.of("names", "Nadeshda"),
                Map.of("names", "Lubov")
        ));
    }

    @Test
    public void mapReduceWithSeveralInputWithoutMapper() {
        YPath inputTable1 = YPath.simple("//tmp/input_table1");
        YPath inputTable2 = YPath.simple("//tmp/input_table2");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable1, CypressNodeType.TABLE, true, true);
        yt.cypress().create(inputTable2, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable1, YTableEntryTypes.YSON, wrap(
                Map.of("letter", "S", "name", "Sergey"),
                Map.of("letter", "S", "name", "Sasha"),
                Map.of("letter", "M", "name", "Maxim")
        ));
        yt.tables().write(inputTable2, YTableEntryTypes.YSON, wrap(
                Map.of("letter", "V", "name", "Vera"),
                Map.of("letter", "N", "name", "Nadeshda"),
                Map.of("letter", "L", "name", "Lubov")
        ));

        var operation = yt.operations().mapReduceAndGetOp(MapReduceSpec.builder()
                .setInputTables(inputTable1, inputTable2)
                .setOutputTables(outputTable)
                .setReduceBy("letter")
                .setReducerSpec(ReducerSpec.builder()
                        .setReducer(new LambdaReducerWithKey<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction<YTreeMapNode, String>) entries -> {
                                    return entries.getString("letter");
                                },
                                (YtFunction4V<String, Iterator<YTreeMapNode>, Yield<YTreeMapNode>, Statistics>)
                                        (key, entry, yield, statistics) -> {
                                            // группируем имена
                                            String names = toStream(entry)
                                                    .map(v -> v.getString("name"))
                                                    .sorted()
                                                    .collect(Collectors.joining(", "));
                                            yield.yield(YTree.mapBuilder().key("names").value(names).buildMap());
                                        }))
                        .build())
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values = yt.tables().read(outputTable, YTableEntryTypes.YSON);
        Assertions.assertThat(values).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("names", "Sasha, Sergey"),
                Map.of("names", "Maxim"),
                Map.of("names", "Vera"),
                Map.of("names", "Nadeshda"),
                Map.of("names", "Lubov")
        ));
    }

    @Test
    public void mapReduceWithSeveralOutputs() {
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable1 = YPath.simple("//tmp/output_table1");
        YPath outputTable2 = YPath.simple("//tmp/output_table2");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, wrap(
                Map.of("sex", "M", "name", "Sergey"),
                Map.of("sex", "M", "name", "Sasha"),
                Map.of("sex", "M", "name", "Maxim"),
                Map.of("sex", "F", "name", "Ira")
        ));

        var operation = yt.operations().mapReduceAndGetOp(MapReduceSpec.builder()
                .setInputTables(inputTable)
                .setOutputTables(outputTable1, outputTable2)
                .setMapperSpec(MapperSpec.builder()
                        .setMapper(new LambdaMapper<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction3V<YTreeMapNode, Yield<YTreeMapNode>, Statistics>) (entry, yield,
                                                                                               statistics) -> {
                                    // первая буква имени
                                    String name = entry.getString("name");
                                    entry.put("letter", YTree.node(name.substring(0, 1)));
                                    yield.yield(entry);
                                }))
                        .build()
                )
                .setReduceBy("sex")
                .setReducerSpec(ReducerSpec.builder()
                        .setReducer(new LambdaReducerWithKey<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction<YTreeMapNode, String>) entries -> {
                                    return entries.getString("sex");
                                },
                                (YtFunction4V<String, Iterator<YTreeMapNode>, Yield<YTreeMapNode>, Statistics>)
                                        (key, entry, yield, statistics) -> {
                                            // группируем имена
                                            String names = toStream(entry)
                                                    .map(v -> v.getString("name"))
                                                    .sorted()
                                                    .collect(Collectors.joining(", "));
                                            if (key.equals("M")) {
                                                yield.yield(0, YTree.mapBuilder().key("names").value(names).buildMap());
                                            } else {
                                                yield.yield(1, YTree.mapBuilder().key("names").value(names).buildMap());
                                            }
                                        }))
                        .build())
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values1 = yt.tables().read(outputTable1, YTableEntryTypes.YSON);
        Assertions.assertThat(values1).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("names", "Maxim, Sasha, Sergey")
        ));

        var values2 = yt.tables().read(outputTable2, YTableEntryTypes.YSON);
        Assertions.assertThat(values2).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("names", "Ira")
        ));
    }

    @Test
    public void mapReduceWithSeveralOutputsWithouMapper() {
        YPath inputTable = YPath.simple("//tmp/input_table");
        YPath outputTable1 = YPath.simple("//tmp/output_table1");
        YPath outputTable2 = YPath.simple("//tmp/output_table2");

        yt.cypress().create(inputTable, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable, YTableEntryTypes.YSON, wrap(
                Map.of("sex", "M", "name", "Sergey"),
                Map.of("sex", "M", "name", "Sasha"),
                Map.of("sex", "M", "name", "Maxim"),
                Map.of("sex", "F", "name", "Ira")
        ));

        var operation = yt.operations().mapReduceAndGetOp(MapReduceSpec.builder()
                .setInputTables(inputTable)
                .setOutputTables(outputTable1, outputTable2)
                .setReduceBy("sex")
                .setReducerSpec(ReducerSpec.builder()
                        .setReducer(new LambdaReducerWithKey<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction<YTreeMapNode, String>) entries -> {
                                    return entries.getString("sex");
                                },
                                (YtFunction4V<String, Iterator<YTreeMapNode>, Yield<YTreeMapNode>, Statistics>)
                                        (key, entry, yield, statistics) -> {
                                            // группируем имена
                                            String names = toStream(entry)
                                                    .map(v -> v.getString("name"))
                                                    .sorted()
                                                    .collect(Collectors.joining(", "));
                                            if (key.equals("M")) {
                                                yield.yield(0, YTree.mapBuilder().key("names").value(names).buildMap());
                                            } else {
                                                yield.yield(1, YTree.mapBuilder().key("names").value(names).buildMap());
                                            }
                                        }))
                        .build())
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values1 = yt.tables().read(outputTable1, YTableEntryTypes.YSON);
        Assertions.assertThat(values1).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("names", "Maxim, Sasha, Sergey")
        ));

        var values2 = yt.tables().read(outputTable2, YTableEntryTypes.YSON);
        Assertions.assertThat(values2).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("names", "Ira")
        ));
    }

    @Test
    public void useTableIndexInReducerWithoutMapper() {
        YPath inputTable1 = YPath.simple("//tmp/input_table1");
        YPath inputTable2 = YPath.simple("//tmp/input_table2");
        YPath outputTable = YPath.simple("//tmp/output_table");

        yt.cypress().create(inputTable1, CypressNodeType.TABLE, true, true);
        yt.cypress().create(inputTable2, CypressNodeType.TABLE, true, true);
        yt.tables().write(inputTable1, YTableEntryTypes.YSON, wrap(
                Map.of("letter", "S", "name", "Sergey"),
                Map.of("letter", "S", "name", "Sasha"),
                Map.of("letter", "M", "name", "Maxim")
        ));
        yt.tables().write(inputTable2, YTableEntryTypes.YSON, wrap(
                Map.of("letter", "V", "name", "Vera"),
                Map.of("letter", "N", "name", "Nadeshda"),
                Map.of("letter", "L", "name", "Lubov")
        ));

        class Key {
            private int index;
            private String letter;

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                Key key = (Key) o;
                return index == key.index && Objects.equals(letter, key.letter);
            }

            @Override
            public int hashCode() {
                return Objects.hash(index, letter);
            }
        }

        var operation = yt.operations().mapReduceAndGetOp(MapReduceSpec.builder()
                .setInputTables(inputTable1, inputTable2)
                .setOutputTables(outputTable)
                .setReduceBy("letter")
                .setReducerSpec(ReducerSpec.builder()
                        .setReducer(new LambdaReducerWithKey<>(YTableEntryTypes.YSON, YTableEntryTypes.YSON,
                                (YtFunction<YTreeMapNode, Key>) entries -> {
                                    var key = new Key();
                                    key.index = entries.getAttribute("table_index").map(v -> v.intValue()).orElse(-1);
                                    key.letter = entries.getString("letter");
                                    return key;
                                },
                                (YtFunction4V<Key, Iterator<YTreeMapNode>, Yield<YTreeMapNode>, Statistics>)
                                        (key, entry, yield, statistics) -> {
                                            // группируем имена
                                            String names = toStream(entry)
                                                    .map(v -> v.getString("name"))
                                                    .sorted()
                                                    .collect(Collectors.joining(", "));
                                            String sex = key.index == 0 ? "M" : "F";

                                            yield.yield(YTree.mapBuilder()
                                                    .key("names").value(names)
                                                    .key("sex").value(sex)
                                                    .buildMap()
                                            );
                                        }))
                        .build())
                .build());
        operation.awaitAndThrowIfNotSuccess();

        var values = yt.tables().read(outputTable, YTableEntryTypes.YSON);
        Assertions.assertThat(values).toIterable().containsExactlyInAnyOrderElementsOf(wrap(
                Map.of("sex", "M", "names", "Sasha, Sergey"),
                Map.of("sex", "M", "names", "Maxim"),
                Map.of("sex", "F", "names", "Vera"),
                Map.of("sex", "F", "names", "Nadeshda"),
                Map.of("sex", "F", "names", "Lubov")
        ));
    }
}
