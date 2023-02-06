package ru.yandex.market.antifraud.yql.validate.executor;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.validate.filter.YqlFilter;
import ru.yandex.market.antifraud.yql.validate.filter.YqlFilterSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.antifraud.yql.validate.executor.FilterRunnerTestUtil.generateFilters;

public class ParallelRunnerTest {
    @Test
    public void mustRunParallel() throws InterruptedException {
        ConcurrentLinkedQueue<Long> filtersOrder = new ConcurrentLinkedQueue<>();
        ParallelRunner runner = new ParallelRunner();
        YqlFilterSet filterSet = new YqlFilterSet("test", generateFilters(filtersOrder, 10));
        runner.run(filterSet, f -> f.apply(Mockito.mock(YqlSession.class)));
        assertThat(filtersOrder.size(), is(10));
        assertThat(new HashSet<>(filtersOrder), is(getFilterIds(filterSet)));
    }

    @Test(expected = ParallelRunnerTestException.class)
    public void mustFailOnFirstError() throws InterruptedException {
        ConcurrentLinkedQueue<Long> filtersOrder = new ConcurrentLinkedQueue<>();
        ParallelRunner runner = new ParallelRunner();
        List<YqlFilter> filters = new ArrayList<>(generateFilters(filtersOrder, 4));
        filters.add(new FailingYqlFilter());
        filters.addAll(generateFilters(filtersOrder, 6,10));

        YqlFilterSet filterSet = new YqlFilterSet("test", filters);
        runner.run(filterSet, f -> f.apply(Mockito.mock(YqlSession.class)));
    }

    @Test(expected = ParallelRunnerTestException.class)
    public void mustFailIfAllErrors() throws InterruptedException {
        List<YqlFilter> filters = new ArrayList<>();
        filters.add(new FailingYqlFilter());
        filters.add(new FailingYqlFilter());
        filters.add(new FailingYqlFilter());
        filters.add(new FailingYqlFilter());
        ParallelRunner runner = new ParallelRunner();
        YqlFilterSet filterSet = new YqlFilterSet("test", filters);
        runner.run(filterSet, f -> f.apply(Mockito.mock(YqlSession.class)));
    }

    private static int SEQ = 0;
    private class FailingYqlFilter implements YqlFilter {
        private final int id = SEQ++;

        @Override
        public void apply(YqlSession session) {
            throw new ParallelRunnerTestException();
        }

        @Override
        public long getId() {
            return id;
        }
    }

    private class ParallelRunnerTestException extends RuntimeException {}

    private Set<Long> getFilterIds(YqlFilterSet filterSet) {
        return filterSet.getFilters().stream()
            .map(YqlFilter::getId)
            .collect(Collectors.toSet());
    }
}
