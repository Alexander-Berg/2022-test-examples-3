package ru.yandex.market.antifraud.yql.validate.executor;

import com.google.common.base.Preconditions;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.validate.filter.YqlFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class FilterRunnerTestUtil {

    public static List<YqlFilter> generateFilters(Queue<Long> filterOrder, int N) {
        return generateFilters(filterOrder, 0, N);
    }

    // startId inclusive
    // endIf not inclusive
    public static List<YqlFilter> generateFilters(Queue<Long> filterOrder, int startId, int endId) {
        Preconditions.checkArgument(startId <= endId);
        List<YqlFilter> filters = new ArrayList<>(endId - startId);
        for(int i = startId; i < endId; i++) {
            int z = i;
            filters.add(new YqlFilter() {
                @Override
                public void apply(YqlSession session) {
                    filterOrder.add(getId());
                }

                @Override
                public long getId() {
                    return z;
                }
            });
        }
        return filters;
    }
}
