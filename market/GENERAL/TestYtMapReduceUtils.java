package ru.yandex.market.mbo.yt.operations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import ru.yandex.inside.yt.kosher.operations.map.Mapper;
import ru.yandex.inside.yt.kosher.operations.reduce.Reducer;
import ru.yandex.market.mbo.yt.utils.StatisticsStub;
import ru.yandex.market.mbo.yt.utils.YieldStub;

import static ru.yandex.market.mbo.yt.operations.TestYtUtils.flatEntries;

/**
 * @author s-ermakov
 */
public class TestYtMapReduceUtils {
    private TestYtMapReduceUtils() {
    }

    @SafeVarargs
    public static <TInput, TSort, TOutput> YieldStub<TOutput> run(
            Mapper<TInput, TSort> mapper,
            Reducer<TSort, TOutput> reducer,
            List<String> sortBy,
            List<String> reduceBy,
            TInput... entries
    ) {
        return run(mapper, reducer, sortBy, reduceBy, Arrays.asList(entries));
    }

    public static <TInput, TSort, TOutput> YieldStub<TOutput> run(
            Mapper<TInput, TSort> mapper,
            Reducer<TSort, TOutput> reducer,
            List<String> sortBy,
            List<String> reduceBy,
            Collection<TInput> entries
    ) {
        return run(mapper, reducer, sortBy, reduceBy, Collections.singletonMap(0, entries.iterator()));
    }

    public static <TInput, TSort, TOutput> YieldStub<TOutput> run(
            @Nullable Mapper<TInput, TSort> mapper,
            Reducer<TSort, TOutput> reducer,
            List<String> sortBy, List<String> reduceBy,
            Map<Integer, Iterator<TInput>> inputRows) {
        if (sortBy.isEmpty()) {
            sortBy = reduceBy;
        }

        Preconditions.checkArgument(isStartsWith(sortBy, reduceBy), "sortBy should start with reduceBy");

        StatisticsStub mapStatisticsStub = new StatisticsStub();
        StatisticsStub reduceStatisticsStub = new StatisticsStub();

        YieldStub<TSort> mapYieldStub = new YieldStub<>();
        YieldStub<TSort> sortYieldStub = new YieldStub<>();
        YieldStub<TOutput> reduceYieldStub = new YieldStub<>();

        // Do map operation
        if (mapper != null) {
            TestYtMapUtils.run(mapper, inputRows, mapYieldStub, mapStatisticsStub);

            // Do sort operation
            TestYtSortUtils.run(sortBy, mapYieldStub.getOut(0), sortYieldStub);

            // Do reduce operation
            TestYtReduceUtils.run(reducer, reduceBy, sortYieldStub.getOut(0), reduceYieldStub,
                    reduceStatisticsStub);
        } else {
            Iterator<TSort> entries = flatEntries(inputRows, inputRows.size() > 1)
                    .map(v -> (TSort) v)
                    .iterator();

            // Do sort operation
            Stream<TSort> sortEntriesStream = TestYtSortUtils.run(sortBy, entries);

            // Do reduce operation
            TestYtReduceUtils.run(reducer, reduceBy, sortEntriesStream.collect(Collectors.toList()),
                    reduceYieldStub, reduceStatisticsStub);
        }

        // merge statistics to check successful merge
        StatisticsStub.merge(mapStatisticsStub, reduceStatisticsStub);

        return reduceYieldStub;
    }

    private static boolean isStartsWith(List<String> sortBy, List<String> reduceBy) {
        if (reduceBy.size() > sortBy.size()) {
            return false;
        }
        for (int i = 0; i < reduceBy.size(); i++) {
            String reduceColumn = reduceBy.get(i);
            String sortColumn = sortBy.get(i);
            if (!reduceColumn.equals(sortColumn)) {
                return false;
            }
        }
        return true;
    }
}
