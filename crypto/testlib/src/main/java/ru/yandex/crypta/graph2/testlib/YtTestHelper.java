package ru.yandex.crypta.graph2.testlib;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.protobuf.Message;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.dao.yt.bendable.YsonMultiEntityReducerWithKey;
import ru.yandex.crypta.graph2.dao.yt.bendable.YsonMultiEntitySupport;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.dao.yt.local.StatisticsSlf4jLoggingImpl;
import ru.yandex.crypta.graph2.dao.yt.proto.NativeProtobufOneOfMessageEntryType;
import ru.yandex.crypta.graph2.dao.yt.utils.YTreeUtils;
import ru.yandex.inside.yt.kosher.impl.operations.utils.ReducerWithKey;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.map.Mapper;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static ru.yandex.crypta.graph2.dao.yt.bendable.YsonMultiEntitySupport.TABLE_INDEX_COLUMN;

public class YtTestHelper {

    public static <TInput, TOutput, TKey extends Comparable<TKey>> LocalYield<TOutput> testReducer(ReducerWithKey<TInput, TOutput, TKey> reducer, List<TInput> input) {
        List<TInput> sortedInput = input
                .stream()
                .sorted(Comparator.comparing(reducer::key))
                .collect(Collectors.toList());
        LocalYield<TOutput> yield = new LocalYield<>();
        reducer.reduce(sortedInput.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);
        return yield;
    }

    public static <TInput extends Message, TOutput extends Message, TKey> LocalYield<TOutput> testOneOfProtoReducer(
            ReducerWithKey<TInput, TOutput, TKey> reducer, ListF<TInput> input, Comparator<TInput> inputRecsComparator) {
        if (inputRecsComparator == null) {
            // by default recs are sorted by reducer key toString representation
            inputRecsComparator = Comparator.comparing(vk -> reducer.key(vk).toString());
        }

        NativeProtobufOneOfMessageEntryType outputType = (NativeProtobufOneOfMessageEntryType) reducer.outputType();
        List<TInput> sortedInput = input.sorted(inputRecsComparator);
        LocalYield<TOutput> yield = new LocalYield<>() {
            @Override
            public void yield(TOutput value) {
                // one-of yield determines tableIndex by message type
                int tableIndex = value.getOneofFieldDescriptor(outputType.getOneofDescriptor()).getIndex();
                this.yield(tableIndex, value);
            }
        };
        reducer.reduce(sortedInput.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);
        return yield;
    }

    public static <TInput extends Message, TKey> LocalYield<YTreeMapNode> testOneOfProtoReducerWithYsonOutput(
            ReducerWithKey<TInput, YTreeMapNode, TKey> reducer, ListF<TInput> input, Comparator<TInput> inputRecsComparator) {
        if (inputRecsComparator == null) {
            // by default recs are sorted by reducer key toString representation
            inputRecsComparator = Comparator.comparing(vk -> reducer.key(vk).toString());
        }

        List<TInput> sortedInput = input.sorted(inputRecsComparator);
        LocalYield<YTreeMapNode> yield = new LocalYield<>();
        reducer.reduce(sortedInput.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);
        return yield;
    }

    public static <TKey> LocalYield<YTreeMapNode> testYsonMultiEntityReducer(YsonMultiEntityReducerWithKey<TKey> reducer, ListF<YTreeMapNode> input) {
        ListF<YTreeMapNode> inputCopy = input.map(YTreeUtils::deepCopyRec);
        List<YTreeMapNode> sortedInput = inputCopy.sortedBy(reducer::key);
        LocalYield<YTreeMapNode> yield = new LocalYield<>();
        reducer.reduce(sortedInput.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);
        return yield;
    }

    public static <TInput, TOutput> LocalYield<TOutput> testMapper(Mapper<TInput, TOutput> mapper, ListF<TInput> inputRecs) {
        LocalYield<TOutput> yield = new LocalYield<>();
        Statistics statistics = new StatisticsSlf4jLoggingImpl();

        mapper.start(yield, statistics);
        for (TInput rec : inputRecs) {
            mapper.map(rec, yield, statistics);
        }
        mapper.finish(yield, statistics);
        return yield;
    }

    public static <T> ListF<YTreeMapNode> toYsonRecs(YsonMultiEntitySupport reducer, List<T> entities, int tableIndex) {
        List<YTreeMapNode> result = entities
                .stream()
                .map(reducer::serialize)
                .peek(rec -> rec.putAttribute(TABLE_INDEX_COLUMN, YTree.integerNode(tableIndex)))
                .collect(Collectors.toList());
        return Cf.wrap(result);
    }

    public static <T> ListF<T> fromYsonRecs(YsonMultiEntitySupport reducer, Class<T> clazz,
                                            LocalYield<YTreeMapNode> recs, int tableIndex) {
        List<T> result = recs.getRecsByIndex(tableIndex)
                .stream()
                .map(r -> reducer.parse(r, clazz))
                .collect(Collectors.toList());
        return Cf.wrap(result);
    }
}
