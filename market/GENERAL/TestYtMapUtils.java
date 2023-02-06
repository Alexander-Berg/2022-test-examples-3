package ru.yandex.market.mbo.yt.operations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import ru.yandex.inside.yt.kosher.operations.OperationContext;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.operations.map.Mapper;
import ru.yandex.market.mbo.yt.utils.StatisticsStub;
import ru.yandex.market.mbo.yt.utils.YieldStub;

import static ru.yandex.market.mbo.yt.operations.TestYtUtils.flatEntries;

/**
 * Эмуляция работы {@link ru.yandex.inside.yt.kosher.impl.operations.mains.MapMain}.
 *
 * @author s-ermakov
 */
public class TestYtMapUtils {
    private TestYtMapUtils() {
    }

    @SafeVarargs
    public static <TInput, TOutput> YieldStub<TOutput> run(Mapper<TInput, TOutput> mapper, TInput... entries) {
        return run(mapper, Arrays.asList(entries));
    }

    public static <TInput, TOutput> YieldStub<TOutput> run(Mapper<TInput, TOutput> mapper, Collection<TInput> entries) {
        YieldStub<TOutput> yield = new YieldStub<>();
        run(mapper, entries, yield, new StatisticsStub());
        return yield;
    }

    public static <TInput, TOutput> void run(Mapper<TInput, TOutput> mapper, Collection<TInput> entries,
                                             Yield<TOutput> yield) {
        run(mapper, entries, yield, new StatisticsStub());
    }

    public static <TInput, TOutput> void run(Mapper<TInput, TOutput> mapper, Collection<TInput> entries,
                                             Yield<TOutput> yield, Statistics statistics) {
        run(mapper, entries.iterator(), yield, statistics);
    }

    public static <TInput, TOutput> void run(Mapper<TInput, TOutput> mapper, Iterator<TInput> entries,
                                             Yield<TOutput> yield, Statistics statistics) {
        OperationContext context = new OperationContext();
        try (Yield<TOutput> yield1 = yield; Statistics statistics1 = statistics) {
            mapper.start(yield1, statistics1);
            while (entries.hasNext()) {
                mapper.map(entries.next(), yield1, statistics1, context);
            }
            mapper.finish(yield1, statistics1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <TOutput, TInput> void run(Mapper<TInput, TOutput> mapper, Map<Integer, Iterator<TInput>> inputRows,
                                             YieldStub<TOutput> yield, StatisticsStub statistics) {
        OperationContext context = new OperationContext();
        try (Yield<TOutput> yield1 = yield; Statistics statistics1 = statistics) {
            mapper.start(yield1, statistics1);

            Stream<TInput> entries = flatEntries(inputRows, inputRows.size() > 1);
            entries.forEach(e -> {
                mapper.map(e, yield1, statistics1, context);
            });
            mapper.finish(yield1, statistics1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
