package ru.yandex.chemodan.app.orchestrator.manager;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * @author yashunsky
 */
public class ActualizationManagerTest {
    private MockManager manager;

    private final MockActualizationItem fastItem = MockActualizationItem.successful(Duration.millis(100));
    private final MockActualizationItem failing = MockActualizationItem.failing(new RuntimeException());

    @Test
    public void testSuccessfulParallelActualization() {
        manager = new MockManager(Cf.repeat(fastItem, 10));
        Instant start = Instant.now();
        ActualisationStatistic<MockActualizationItem> stat = manager.actualize();
        Assert.assertFalse(stat.hasFailures());
        Instant end = Instant.now();
        Assert.le(new Duration(start, end).getMillis(), 500L);

        Assert.equals(10, manager.getActualized());
    }

    @Test
    public void testPartlySuccessfulActualization() {
        manager = new MockManager(Cf.list(fastItem, failing));

        ActualisationStatistic<MockActualizationItem> stat = manager.actualize();
        Assert.assertTrue(stat.hasFailures());
        Assert.equals(stat.getGettingSublistFailed(), 0);
        Assert.equals(stat.getActualizeSubItemFailed(), 1);
        Assert.equals(stat.getFailures().size(), 1);

        Assert.equals(1, manager.getActualized());
    }

    private static class MockManager extends ActualizationManager<MockActualizationItem> {
        private final ListF<MockActualizationItem> items;
        private final AtomicInteger actualizedCounter;

        public MockManager(ListF<MockActualizationItem> items) {
            super(null, null, null, null, 10);
            this.items = items;
            this.actualizedCounter = new AtomicInteger(0);
        }

        @Override
        ListF<MockActualizationItem> listItems() {
            return items;
        }

        @Override
        void actualizeItem(MockActualizationItem item) {
            if (item.exception.isPresent()) {
                throw item.exception.get();
            }
            ThreadUtils.sleep(item.duration);
            actualizedCounter.incrementAndGet();
        }

        public int getActualized() {
            return actualizedCounter.get();
        }
    }

    @Data
    private static class MockActualizationItem {
        private final Duration duration;
        private final Option<RuntimeException> exception;

        public static MockActualizationItem failing(RuntimeException exception) {
            return new MockActualizationItem(Duration.ZERO, Option.of(exception));
        }

        public static MockActualizationItem successful(Duration duration) {
            return new MockActualizationItem(duration, Option.empty());
        }
    }
}
