package ru.yandex.market.mbo.yt.operations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.operations.OperationContext;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.operations.reduce.Reducer;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.yt.utils.StatisticsStub;
import ru.yandex.market.mbo.yt.utils.YieldStub;

import static ru.yandex.market.mbo.yt.operations.TestYtUtils.flatEntries;
import static ru.yandex.market.mbo.yt.operations.TestYtUtils.getColumnValue;

/**
 * Эмуляция работы {@link ru.yandex.inside.yt.kosher.impl.operations.mains.ReduceMain}.
 *
 * @author s-ermakov
 */
public class TestYtReduceUtils {
    private TestYtReduceUtils() {
    }

    @SafeVarargs
    public static <TInput, TOutput> YieldStub<TOutput> run(Reducer<TInput, TOutput> reducer, List<String> reduceBy,
                                                           TInput... entries) {
        return run(reducer, reduceBy, Arrays.asList(entries));
    }

    public static <TInput, TOutput> YieldStub<TOutput> run(Reducer<TInput, TOutput> reducer, List<String> reduceBy,
                                                           Collection<TInput> entries) {
        YieldStub<TOutput> yield = new YieldStub<>();
        run(reducer, reduceBy, entries, yield, new StatisticsStub());
        return yield;
    }

    public static <TInput, TOutput> void run(Reducer<TInput, TOutput> reducer, List<String> reduceBy,
                                             Collection<TInput> entries, Yield<TOutput> yield) {
        run(reducer, reduceBy, entries, yield, new StatisticsStub());
    }

    public static <TInput, TOutput> void run(Reducer<TInput, TOutput> reducer, List<String> reduceBy,
                                             Collection<TInput> entries, Yield<TOutput> yield,
                                             Statistics statistics) {
        run(reducer, reduceBy, entries.iterator(), yield, statistics);
    }

    public static <TInput, TOutput> void run(Reducer<TInput, TOutput> reducer, List<String> reduceBy,
                                             Iterator<TInput> entries, Yield<TOutput> yield, Statistics statistics) {
        run(reducer, reduceBy, Collections.singletonMap(0, entries), yield, statistics);
    }

    public static <TInput, TOutput> YieldStub<TOutput> run(Reducer<TInput, TOutput> reducer, List<String> reduceBy,
                                                           Map<Integer, Iterator<TInput>> entriesByInputTable) {
        YieldStub<TOutput> yield = new YieldStub<>();
        run(reducer, reduceBy, entriesByInputTable, yield, new StatisticsStub());
        return yield;
    }

    public static <TInput, TOutput> void run(Reducer<TInput, TOutput> reducer, List<String> reduceBy,
                                             Map<Integer, Iterator<TInput>> entriesByInputTable,
                                             Yield<TOutput> yield, Statistics statistics) {
        // group data by reduceBy key fields
        Map<YTreeMapNode, List<TInput>> groupedValues = groupData(reduceBy, entriesByInputTable,
                entriesByInputTable.size() > 1);

        // do reduce
        try (Yield<TOutput> yield1 = yield; Statistics statistics1 = statistics) {
            for (Map.Entry<YTreeMapNode, List<TInput>> mrKey2Values : groupedValues.entrySet()) {
                List<TInput> values = mrKey2Values.getValue();
                doReduce(reducer, values.iterator(), yield1, statistics1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Отдельный метод для алгоритма reduce c внешней таблицей.
     * ВНИМАНИЕ! внешняя таблица(ы) должны идти первыми в списке конфигурации операции
     */
    @SuppressWarnings("checkstyle:parameterNumber")
    public static <TInput, TOutput> void run(Reducer<TInput, TOutput> reducer,
                                             List<String> reduceBy, List<String> joinBy, boolean enableKeyGuarantee,
                                             Map<Integer, Iterator<TInput>> foreignEntries,
                                             Map<Integer, Iterator<TInput>> primaryEntries,
                                             Yield<TOutput> yield, Statistics statistics) {
        // проверяем, что foreign таблицы идут первыми в списке
        // Это нужно только для test-yt моков, так как другой функционал просто не реализован в моке
        Set<Integer> foreignKeys = foreignEntries.keySet();
        Set<Integer> primaryKeys = primaryEntries.keySet();

        Integer maxForeignIndex = foreignKeys.stream().max(Comparator.naturalOrder()).orElse(-1);
        Integer minPrimaryIndex = primaryKeys.stream().min(Comparator.naturalOrder()).orElse(-1);
        if (maxForeignIndex >= minPrimaryIndex) {
            throw new IllegalStateException("Currently supported foreign table on first places of list");
        }

        // group foreign data by joinBy key fields
        Map<YTreeMapNode, List<TInput>> foreignGroupedValues = groupData(joinBy, foreignEntries, true);

        // group primary data by reduceBy key fields
        Map<YTreeMapNode, List<TInput>> primaryGroupedValues = groupData(reduceBy, primaryEntries, true);

        // do reduce
        try (Yield<TOutput> yield1 = yield; Statistics statistics1 = statistics) {
            for (Map.Entry<YTreeMapNode, List<TInput>> mrKey2Values : primaryGroupedValues.entrySet()) {
                YTreeMapNode reduceKey = mrKey2Values.getKey();
                List<TInput> primaryValues = mrKey2Values.getValue();

                // sub group by joinBy
                Map<YTreeMapNode, List<TInput>> subGroupValues = groupData(joinBy,
                        Collections.singletonMap(-1, primaryValues.iterator()), false);
                for (Map.Entry<YTreeMapNode, List<TInput>> submrKey2Values : subGroupValues.entrySet()) {
                    YTreeMapNode joinByKey = submrKey2Values.getKey();
                    // запись в foreign table может отсутствовать (это вполне валидный кейс)
                    List<TInput> foreignV = foreignGroupedValues.getOrDefault(joinByKey, Collections.emptyList());
                    List<TInput> primaryV = submrKey2Values.getValue();

                    // read about enable_key_guarantee in https://clubs.at.yandex-team.ru/yt/3587
                    if (enableKeyGuarantee) {
                        // вообще порядок foreign таблиц определяется по table index,
                        // но для тестов в настоящий момент foreign таблицы идут перед списком
                        Iterator<TInput> iterator = Stream.concat(foreignV.stream(), primaryV.stream()).iterator();
                        doReduce(reducer, iterator, yield1, statistics1);
                    } else {
                        primaryV.forEach(entry -> {
                            Iterator<TInput> iterator = Stream.concat(foreignV.stream(), Stream.of(entry)).iterator();
                            doReduce(reducer, iterator, yield1, statistics1);
                        });
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <TInput> Map<YTreeMapNode, List<TInput>> groupData(List<String> groupBy,
                                                                      Map<Integer, Iterator<TInput>> entries,
                                                                      boolean setTableIndex) {
        Stream<TInput> flatEntries = flatEntries(entries, setTableIndex);

        return flatEntries.collect(
                Collectors.groupingBy(
                        dataNode -> {
                            YTreeBuilder keyBuilder = YTree.mapBuilder();
                            for (String reduceField : groupBy) {
                                keyBuilder.key(reduceField).value(getColumnValue(dataNode, reduceField));
                            }
                            return keyBuilder.buildMap();
                        },
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private static <TInput, TOutput> void doReduce(Reducer<TInput, TOutput> reducer, Iterator<TInput> entries,
                                                   Yield<TOutput> yield, Statistics statistics) {
        reducer.start(yield, statistics);
        reducer.reduce(entries, yield, statistics, new OperationContext());
        entries.forEachRemaining(tmp -> {
        });
        reducer.finish(yield, statistics);
    }
}
