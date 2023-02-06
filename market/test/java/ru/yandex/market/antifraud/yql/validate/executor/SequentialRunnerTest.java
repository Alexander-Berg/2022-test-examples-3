package ru.yandex.market.antifraud.yql.validate.executor;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.validate.filter.YqlFilterSet;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.antifraud.yql.validate.executor.FilterRunnerTestUtil.generateFilters;

public class SequentialRunnerTest {

    @Test
    public void mustRunSequentialy() throws InterruptedException {
        ConcurrentLinkedQueue<Long> filtersOrder = new ConcurrentLinkedQueue<>();
        SequentialRunner runner = new SequentialRunner();
        runner.run(new YqlFilterSet("test", generateFilters(filtersOrder, 10)),
            f -> f.apply(Mockito.mock(YqlSession.class)));
        assertThat(filtersOrder.size(), is(10));
        for(int i = 0; i < 10; i++) {
            assertEquals(i, filtersOrder.poll().longValue());
        }
    }
}
