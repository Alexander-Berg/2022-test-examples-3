package ru.yandex.market.yt.util.reducers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.operations.specs.ReduceSpec;
import ru.yandex.inside.yt.kosher.operations.specs.ReducerSpec;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.yt.util.mapreduce.Reducer2WithJoinByKeyAndStats;
import ru.yandex.market.yt.util.mapreduce.ReducerWithJoinByKeyAndStats;

/**
 * Tests of {@link Reducer2WithJoinByKeyAndStats}, {@link ReducerWithJoinByKeyAndStats}.
 */
public class ReduceWithForeignTable {

    private final TestYt yt = new TestYt();

    @Test
    public void testThrowExceptionIfForeignTableIsSetWithoutJoinBy() {
        // create input tables
        YPath tablePrimary = YPath.simple("//tmp/table_primary");
        YPath tableForeign = YPath.simple("//tmp/table_foreign");
        YPath outTable = YPath.simple("//tmp/table_output");

        yt.cypress().create(tablePrimary, CypressNodeType.TABLE, true, true);
        yt.tables().write(tablePrimary, YTableEntryTypes.YSON, getPrimaryRows());
        yt.cypress().create(tableForeign, CypressNodeType.TABLE, true, true);
        yt.tables().write(tableForeign, YTableEntryTypes.YSON, getForeignRows());

        // sort tables
        Operation sortPrimary = yt.operations().sortAndGetOp(tablePrimary, tablePrimary,
            Cf.arrayList("key", "key2", "value"));
        Operation sortForeign = yt.operations().sortAndGetOp(tableForeign, tableForeign,
            Cf.arrayList("key"));
        sortPrimary.awaitAndThrowIfNotSuccess();
        sortForeign.awaitAndThrowIfNotSuccess();

        Assertions.assertThatThrownBy(() -> {
            // run reduce with foreign table
            ReduceSpec spec = new ReduceSpec(
                Cf.arrayList(tableForeign.foreign(true), tablePrimary),
                Cf.arrayList(outTable),
                Cf.arrayList("key", "key2"),
                new ReducerWithForeignTable()
            );

            Operation op = yt.operations().reduceAndGetOp(spec);
            op.awaitAndThrowIfNotSuccess();
        }).hasMessageContaining("It is required to specify join_by when using foreign tables");
    }

    @Test
    public void testThrowExceptionIfJoinByIsSetWithoutForeignTable() {
        // create input tables
        YPath tablePrimary = YPath.simple("//tmp/table_primary");
        YPath outTable = YPath.simple("//tmp/table_output");

        yt.cypress().create(tablePrimary, CypressNodeType.TABLE, true, true);
        yt.tables().write(tablePrimary, YTableEntryTypes.YSON, getPrimaryRows());

        // sort tables
        Operation sortPrimary = yt.operations().sortAndGetOp(tablePrimary, tablePrimary,
            Cf.arrayList("key", "key2", "value"));
        sortPrimary.awaitAndThrowIfNotSuccess();

        Assertions.assertThatThrownBy(() -> {
            // run reduce with foreign table
            ReduceSpec spec = ReduceSpec.builder()
                .setInputTables(Cf.arrayList(tablePrimary))
                .setOutputTables(Cf.arrayList(outTable))
                .setReduceBy(Cf.arrayList("key", "key2"))
                .setReducerSpec(ReducerSpec.builder().setReducer(new ReducerWithForeignTable()).build())
                .setJoinBy(Cf.list("key"))
                .build();

            Operation op = yt.operations().reduceAndGetOp(spec);
            op.awaitAndThrowIfNotSuccess();
        }).hasMessageContaining("At least one foreign input table is required when join_by is specified");
    }

    @Test
    public void testThrowExceptionIsReduceByDoesntStartWithJoinBy() {
        // create input tables
        YPath tablePrimary = YPath.simple("//tmp/table_primary");
        YPath tableForeign = YPath.simple("//tmp/table_foreign");
        YPath outTable = YPath.simple("//tmp/table_output");

        yt.cypress().create(tablePrimary, CypressNodeType.TABLE, true, true);
        yt.tables().write(tablePrimary, YTableEntryTypes.YSON, getPrimaryRows());
        yt.cypress().create(tableForeign, CypressNodeType.TABLE, true, true);
        yt.tables().write(tableForeign, YTableEntryTypes.YSON, getForeignRows());

        // sort tables
        Operation sortPrimary = yt.operations().sortAndGetOp(tablePrimary, tablePrimary,
            Cf.arrayList("key", "key2", "value"));
        Operation sortForeign = yt.operations().sortAndGetOp(tableForeign, tableForeign,
            Cf.arrayList("key"));
        sortPrimary.awaitAndThrowIfNotSuccess();
        sortForeign.awaitAndThrowIfNotSuccess();

        Assertions.assertThatThrownBy(() -> {
            // run reduce with foreign table
            ReduceSpec spec = ReduceSpec.builder()
                .setInputTables(Cf.arrayList(tableForeign.foreign(true), tablePrimary))
                .setOutputTables(Cf.arrayList(outTable))
                .setReduceBy(Cf.arrayList("key", "key2"))
                .setReducerSpec(ReducerSpec.builder().setReducer(new ReducerWithForeignTable()).build())
                .setJoinBy(Cf.list("key2"))
                .build();

            Operation op = yt.operations().reduceAndGetOp(spec);
            op.awaitAndThrowIfNotSuccess();
        }).hasMessageContaining("Join key columns ([key2]) are not compatible with reduce key columns ([key, key2])");
    }

    /**
     * Проверяем, что reduce с внешней таблицей работает корректно.
     * Допустим у нас есть 2 таблицы: основная и внешняя (справочная)
     * Основная       Внешняя
     * (a, 0, 1)     (a, x)
     * (b, 1, 2)     (b, y)
     * (b, 0, 3)     (c, z)
     * (b, 1, 4)     (d, W)
     * (b, 0, 5)
     * (b, 1, 6)
     * (c, 0, 7)
     * (c, 1, 8)
     * <p>
     * reduce_by - первые 2 столбца у основной таблицы (key1, key2)
     * join_by - первый столбец у внешней таблицы и у внешней (key1)
     * <p>
     * Тогда операция должена упорядочить записи так (records - в этом порядке будут приходить записи в reducer):
     * join_by reduce_by        records
     * (a)     (a, 0)           (a, x)
     *                          (a, 0, 1)
     * (b)     (b, 0)           (b, y)
     *                          (b, 0, 3)
     *                          (b, 0, 5)
     *         (b, 1)           (b, 1, 2)
     *                          (b, 1, 4)
     *                          (b, 1, 6)
     * (c)     (c, 0)           (c, z)
     *                          (c, 0, 7)
     *         (c, q)           (c, 1, 8)
     *
     * можно заметить, что запись (d, W) отсутствует в records.
     * Это из-за того, что в основной таблице нет записей, имеющие ключ (d).
     */
    @Test
    public void testReduceWithForeignTable() {
        // create input tables
        YPath tablePrimary = YPath.simple("//tmp/table_primary");
        YPath tableForeign = YPath.simple("//tmp/table_foreign");
        YPath outTable = YPath.simple("//tmp/table_output");

        yt.cypress().create(tablePrimary, CypressNodeType.TABLE, true, true);
        yt.tables().write(tablePrimary, YTableEntryTypes.YSON, getPrimaryRows());
        yt.cypress().create(tableForeign, CypressNodeType.TABLE, true, true);
        yt.tables().write(tableForeign, YTableEntryTypes.YSON, getForeignRows());

        // sort tables
        Operation sortPrimary = yt.operations().sortAndGetOp(tablePrimary, tablePrimary,
            Cf.arrayList("key", "key2", "value"));
        Operation sortForeign = yt.operations().sortAndGetOp(tableForeign, tableForeign,
            Cf.arrayList("key"));
        sortPrimary.awaitAndThrowIfNotSuccess();
        sortForeign.awaitAndThrowIfNotSuccess();

        // run reduce with foreign table
        ReduceSpec spec = ReduceSpec.builder()
            .setInputTables(Cf.arrayList(tableForeign.foreign(true), tablePrimary))
            .setOutputTables(Cf.arrayList(outTable))
            .setReduceBy(Cf.arrayList("key", "key2"))
            .setReducerSpec(ReducerSpec.builder().setReducer(new ReducerWithForeignTable()).build())
            .setJoinBy(Cf.list("key"))
            .build();

        Operation op = yt.operations().reduceAndGetOp(spec);
        op.awaitAndThrowIfNotSuccess();

        // assert result
        List<YTreeMapNode> result = new ArrayList<>();
        yt.tables().read(outTable, YTableEntryTypes.YSON, (Consumer<YTreeMapNode>) result::add);

        // _____________________________________________________________________________
        // Если у кого-то начал падать этот тест, то я вам очень сочувствую
        // Отлаживать reduce with foreign tables в boilerplate code невыносимо тяжело,
        // но в любом случае отлаживать юнит тесты проще, чем дебажить в деве :)
        // -----------------------------------------------------------------------------
        // На момент написания теста текущий expected value написан очень верно
        // result не может и не должен содержать больше записей, даже не смотря на то,
        // что в foreign таблице есть еще записи.
        //
        // Значение result тоже написаны верно. Т.е. 2y4y6y - верно, 4y2y6y - не верно. Цифры
        // должны быть отсортированы в порядке возрастания.
        // За это отвечает сортировка по столбцу "value" в теле этого теста
        // -----------------------------------------------------------------------------
        Assertions.assertThat(result)
            .containsExactlyInAnyOrder(
                YTree.mapBuilder().key("key").value("a").key("key2").value("0").key("result").value("1x").buildMap(),
                YTree.mapBuilder().key("key").value("b").key("key2").value("1").key("result").value("2y4y6y")
                    .buildMap(),
                YTree.mapBuilder().key("key").value("b").key("key2").value("0").key("result").value("3y5y").buildMap(),
                YTree.mapBuilder().key("key").value("c").key("key2").value("0").key("result").value("7z").buildMap(),
                YTree.mapBuilder().key("key").value("c").key("key2").value("1").key("result").value("8z").buildMap()
            );
    }

    /**
     * Тест, аналогичный предыдущему, только некоторые записи из внешней таблицы отсутствуют.
     */
    @Test
    public void testReduceWithForeignTableWithMissingRows() {
        // create input tables
        YPath tablePrimary = YPath.simple("//tmp/table_primary");
        YPath tableForeign = YPath.simple("//tmp/table_foreign");
        YPath outTable = YPath.simple("//tmp/table_output");

        yt.cypress().create(tablePrimary, CypressNodeType.TABLE, true, true);
        yt.tables().write(tablePrimary, YTableEntryTypes.YSON, getPrimaryRows());
        yt.cypress().create(tableForeign, CypressNodeType.TABLE, true, true);
        yt.tables().write(tableForeign, YTableEntryTypes.YSON, Cf.list(
            keyName("b", "y"),
            keyName("d", "W")
        ));

        // sort tables
        Operation sortPrimary = yt.operations().sortAndGetOp(tablePrimary, tablePrimary,
            Cf.arrayList("key", "key2", "value"));
        Operation sortForeign = yt.operations().sortAndGetOp(tableForeign, tableForeign,
            Cf.arrayList("key"));
        sortPrimary.awaitAndThrowIfNotSuccess();
        sortForeign.awaitAndThrowIfNotSuccess();

        // run reduce with foreign table
        ReduceSpec spec = ReduceSpec.builder()
            .setInputTables(Cf.arrayList(tableForeign.foreign(true), tablePrimary))
            .setOutputTables(Cf.arrayList(outTable))
            .setReduceBy(Cf.arrayList("key", "key2"))
            .setReducerSpec(ReducerSpec.builder().setReducer(new ReducerWithForeignTable()).build())
            .setJoinBy(Cf.list("key"))
            .build();

        Operation op = yt.operations().reduceAndGetOp(spec);
        op.awaitAndThrowIfNotSuccess();

        // assert result
        List<YTreeMapNode> result = new ArrayList<>();
        yt.tables().read(outTable, YTableEntryTypes.YSON, (Consumer<YTreeMapNode>) result::add);
        Assertions.assertThat(result)
            .containsExactlyInAnyOrder(
                YTree.mapBuilder().key("key").value("a").key("key2").value("0").key("result").value("1").buildMap(),
                YTree.mapBuilder().key("key").value("b").key("key2").value("1").key("result").value("2y4y6y")
                    .buildMap(),
                YTree.mapBuilder().key("key").value("b").key("key2").value("0").key("result").value("3y5y").buildMap(),
                YTree.mapBuilder().key("key").value("c").key("key2").value("0").key("result").value("7").buildMap(),
                YTree.mapBuilder().key("key").value("c").key("key2").value("1").key("result").value("8").buildMap()
            );
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void testReduceWith2ForeignTables() {
        // create input tables
        YPath tablePrimary = YPath.simple("//tmp/table_primary");
        YPath tableForeign1 = YPath.simple("//tmp/table_foreign1");
        YPath tableForeign2 = YPath.simple("//tmp/table_foreign2");
        YPath outTable = YPath.simple("//tmp/table_output");

        yt.tables().write(tablePrimary, YTableEntryTypes.YSON, Cf.list(
            keysValue("a", "", 1),
            keysValue("b", "", 2),
            keysValue("b", "", 3),
            keysValue("c", "", 4),
            keysValue("e", "", 5)
        ));
        yt.tables().write(tableForeign1, YTableEntryTypes.YSON, Cf.list(
            keyName("a", "Сергей"),
            keyName("b", "Дмитрий"),
            keyName("d", "Константин")
        ));
        yt.tables().write(tableForeign2, YTableEntryTypes.YSON, Cf.list(
            keySurname("a", "Афанасьев"),
            keySurname("c", "Баранов"),
            keySurname("d", "Ветошкин")
        ));

        // sort tables
        Operation sortPrimary = yt.operations().sortAndGetOp(tablePrimary, tablePrimary,
            Cf.arrayList("key", "value"));
        Operation sortForeign1 = yt.operations().sortAndGetOp(tableForeign1, tableForeign1,
            Cf.arrayList("key"));
        Operation sortForeign2 = yt.operations().sortAndGetOp(tableForeign2, tableForeign2,
            Cf.arrayList("key"));
        sortPrimary.awaitAndThrowIfNotSuccess();
        sortForeign1.awaitAndThrowIfNotSuccess();
        sortForeign2.awaitAndThrowIfNotSuccess();

        // run reduce with foreign table
        ReduceSpec spec = ReduceSpec.builder()
            .setInputTables(Cf.arrayList(tableForeign1.foreign(true), tableForeign2.foreign(true), tablePrimary))
            .setOutputTables(Cf.arrayList(outTable))
            .setReduceBy(Cf.arrayList("key"))
            .setReducerSpec(ReducerSpec.builder().setReducer(new ReducerWith2ForeignTable()).build())
            .setJoinBy(Cf.list("key"))
            .build();

        Operation op = yt.operations().reduceAndGetOp(spec);
        op.awaitAndThrowIfNotSuccess();

        // assert result
        List<YTreeMapNode> result = new ArrayList<>();
        yt.tables().read(outTable, YTableEntryTypes.YSON, (Consumer<YTreeMapNode>) result::add);

        Assertions.assertThat(result)
            .containsExactlyInAnyOrder(
                YTree.mapBuilder().key("key").value("a").key("result").value("Сергей Афанасьев 1").buildMap(),
                YTree.mapBuilder().key("key").value("b").key("result").value("Дмитрий 2, 3").buildMap(),
                YTree.mapBuilder().key("key").value("c").key("result").value("Баранов 4").buildMap(),
                YTree.mapBuilder().key("key").value("e").key("result").value("5").buildMap()
            );
    }

    public static class ReducerWithForeignTable extends ReducerWithJoinByKeyAndStats<String, YTreeMapNode, String> {

        @Override
        public String joinByKey(YTreeMapNode foreignTableEntry) {
            return foreignTableEntry.getString("key");
        }

        @Override
        public YTreeMapNode reduceByKey(String joinByKey, YTreeMapNode primaryTableEntry) {
            return YTree.mapBuilder()
                .key("key").value(primaryTableEntry.getString("key"))
                .key("key2").value(primaryTableEntry.getString("key2"))
                .buildMap();
        }

        @Override
        public String mapForeignValue(String joinByKey, @Nullable YTreeMapNode foreignTableEntry) {
            return foreignTableEntry == null ? "" : foreignTableEntry.getString("name");
        }

        @Override
        public void reduceGroup(String joinByKey, @Nullable String name,
                                YTreeMapNode reduceByKey, Iterator<YTreeMapNode> entries,
                                Yield<YTreeMapNode> yield, Statistics statistics) {
            String key1 = reduceByKey.getString("key");
            String key2 = reduceByKey.getString("key2");
            StringBuilder result = new StringBuilder();

            entries.forEachRemaining(entry -> {
                long value = entry.getLong("value");
                result.append(value).append(name);
            });

            YTreeMapNode mapNode = YTree.mapBuilder()
                .key("key").value(key1)
                .key("key2").value(key2)
                .key("result").value(result.toString())
                .buildMap();
            yield.yield(mapNode);
        }
    }

    public static class ReducerWith2ForeignTable
        extends Reducer2WithJoinByKeyAndStats<String, String, String, String> {

        @Override
        public String joinByKey(YTreeMapNode foreignTableEntry) {
            return foreignTableEntry.getString("key");
        }

        @Override
        public String reduceByKey(String joinByKey, YTreeMapNode primaryTableEntry) {
            return primaryTableEntry.getString("key");
        }

        @Override
        public String mapForeignValue1(String joinByKey, @Nullable YTreeMapNode foreignTableEntry1) {
            return foreignTableEntry1 == null ? null : foreignTableEntry1.getString("name");
        }

        @Override
        public String mapForeignValue2(String joinByKey, @Nullable YTreeMapNode foreignTableEntry2) {
            return foreignTableEntry2 == null ? null : foreignTableEntry2.getString("surname");
        }

        @Override
        public void reduceGroup(String joinByKey, String name, String surname, String reduceByKey,
                                Iterator<YTreeMapNode> primaryTableEntries,
                                Yield<YTreeMapNode> yield, Statistics statistics) {
            StringBuilder sb = new StringBuilder();
            if (name != null) {
                sb.append(name).append(" ");
            }
            if (surname != null) {
                sb.append(surname).append(" ");
            }

            List<Long> values = new ArrayList<>();
            while (primaryTableEntries.hasNext()) {
                YTreeMapNode n = primaryTableEntries.next();
                values.add(n.getLong("value"));
            }

            String result = values.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
            sb.append(result);

            yield.yield(YTree.mapBuilder().key("key").value(joinByKey).key("result").value(sb.toString()).buildMap());
        }
    }

    @SuppressWarnings("checkstyle:magicNumber")
    private static ListF<YTreeMapNode> getPrimaryRows() {
        return Cf.list(
            keysValue("a", "0", 1),
            keysValue("b", "1", 2),
            keysValue("b", "0", 3),
            keysValue("b", "1", 4),
            keysValue("b", "0", 5),
            keysValue("b", "1", 6),
            keysValue("c", "0", 7),
            keysValue("c", "1", 8)
        );
    }

    @SuppressWarnings("checkstyle:magicNumber")
    private static ListF<YTreeMapNode> getForeignRows() {
        return Cf.list(
            keyName("a", "x"),
            keyName("b", "y"),
            keyName("c", "z"),
            keyName("d", "W")
        );
    }

    private static YTreeMapNode keysValue(String key1, String key2, int value) {
        return YTree.mapBuilder()
            .key("key").value(key1)
            .key("key2").value(key2)
            .key("value").value(value)
            .buildMap();
    }

    private static YTreeMapNode keyName(String key, String name) {
        return YTree.mapBuilder()
            .key("key").value(key)
            .key("name").value(name)
            .buildMap();
    }

    private static YTreeMapNode keySurname(String key, String name) {
        return YTree.mapBuilder()
            .key("key").value(key)
            .key("surname").value(name)
            .buildMap();
    }
}
