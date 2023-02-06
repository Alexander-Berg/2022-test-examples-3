package ru.yandex.market.tsum.pipelines.load;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Streams;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.tsum.clients.checkout.ShootingOrderRef;
import ru.yandex.market.tsum.clients.lom.ShootingOrdersReadyToCancel;
import ru.yandex.market.tsum.pipelines.load.configs.ShootingOrderCancellerConfig;
import ru.yandex.market.tsum.pipelines.load.configs.ShootingOrderLimiterWindowConfig;

public class ShootingOrderCancellerTest {
    private static class MockFetcher implements ShootingOrderFetcher {
        private final List<List<ShootingOrderRef>> orders;
        private int idx = -1;

        MockFetcher(List<List<ShootingOrderRef>> orders) {
            this.orders = orders;
        }

        public static MockFetcher of(List<ShootingOrderRef>... orders) {
            return new MockFetcher(Arrays.asList(orders));
        }

        @Override
        public List<ShootingOrderRef> getOrders(int skip) {
            if (idx + 1 >= orders.size()) {
                return List.of();
            }
            ++idx;
            return orders.get(idx);
        }
    }

    private static final class MockCleanerImpl implements ShootingOrderCleaner {
        private static final MockCleanerImpl INSTANCE = new MockCleanerImpl();

        @Override
        public Set<ShootingOrderRef> tryToDeleteTracks(Collection<ShootingOrderRef> orderIds) {
            return Collections.emptySet();
        }

        @Override
        public Set<ShootingOrderRef> tryToCancelOrders(Collection<ShootingOrderRef> orderIds) {
            return Collections.emptySet();
        }
    }

    private static final class MockClock extends Clock {
        private final List<Instant> instants;
        private int idx = -1;

        MockClock(List<Instant> instants) {
            this.instants = instants;
        }

        public static MockClock of(Instant... instants) {
            return new MockClock(Arrays.asList(instants));
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new IllegalStateException("Unimplemented method");
        }

        @Override
        public Instant instant() {
            ++idx;
            return instants.get(idx);
        }
    }

    private static final class MockListener implements ShootingOrderListener {
        private List<Collection<ShootingOrdersReadyToCancel.ShootingOrder>> lomOrders = new ArrayList<>();
        private List<Collection<ShootingOrderRef>> checkouterOrders = new ArrayList<>();

        private List<Pair<Collection<ShootingOrderRef>, Collection<ShootingOrderRef>>> fetched = new ArrayList<>();
        private List<Pair<Collection<ShootingOrderRef>, Collection<ShootingOrderRef>>> scheduled = new ArrayList<>();
        private List<Pair<Collection<ShootingOrderRef>, Collection<ShootingOrderRef>>> waiting = new ArrayList<>();
        private int emptyScheduled = 0;
        private int emptyWarning = 0;

        @Override
        public void onFetchFromLOM(Collection<ShootingOrdersReadyToCancel.ShootingOrder> ids) {
            lomOrders.add(ids);
        }

        @Override
        public void onFetchFromCheckouter(Collection<ShootingOrderRef> ids) {
            checkouterOrders.add(ids);
        }

        @Override
        public void onFetch(Collection<ShootingOrderRef> queue, Collection<ShootingOrderRef> orders) {
            if (!orders.isEmpty()) {
                fetched.add(Pair.of(new ArrayList<>(queue), new ArrayList<>(orders)));
            }
        }

        @Override
        public void onSchedule(Collection<ShootingOrderRef> queue, Collection<ShootingOrderRef> orders) {
            scheduled.add(Pair.of(new ArrayList<>(queue), new ArrayList<>(orders)));
        }

        @Override
        public void onWaiting(Collection<ShootingOrderRef> queue, Collection<ShootingOrderRef> orders) {
            waiting.add(Pair.of(new ArrayList<>(queue), new ArrayList<>(orders)));
        }

        @Override
        public void onSuccessfulClean(Collection<ShootingOrderRef> cleaned) {

        }

        @Override
        public void onEmptyScheduled() {
            ++emptyScheduled;
        }

        @Override
        public void onEmptyWaiting() {
            ++emptyWarning;
        }

        @Override
        public void onFailedOrders(Collection<ShootingOrderRef> queue, Collection<ShootingOrderRef> failedOrders) {

        }

        @Override
        public void onFailedTracks(Collection<ShootingOrderRef> queue, Collection<ShootingOrderRef> failedTracks) {

        }

        public List<Pair<Collection<ShootingOrderRef>, Collection<ShootingOrderRef>>> getFetched() {
            return fetched;
        }

        public List<Pair<Collection<ShootingOrderRef>, Collection<ShootingOrderRef>>> getScheduled() {
            return scheduled;
        }

        public List<Pair<Collection<ShootingOrderRef>, Collection<ShootingOrderRef>>> getWaiting() {
            return waiting;
        }

        public int getEmptyScheduled() {
            return emptyScheduled;
        }

        public int getEmptyWaiting() {
            return emptyWarning;
        }
    }

    private static final ShootingOrderRef ORDER_1 = new ShootingOrderRef(1L, 1L);
    private static final ShootingOrderRef ORDER_2 = new ShootingOrderRef(2L, 2L);
    private static final ShootingOrderRef ORDER_3 = new ShootingOrderRef(3L, 3L);
    private static final ShootingOrderRef ORDER_4 = new ShootingOrderRef(4L, 4L);
    private static final ShootingOrderRef ORDER_5 = new ShootingOrderRef(5L, 5L);
    private static final ShootingOrderRef ORDER_6 = new ShootingOrderRef(6L, 6L);
    private static final ShootingOrderRef ORDER_7 = new ShootingOrderRef(7L, 7L);
    private static final ShootingOrderRef ORDER_8 = new ShootingOrderRef(8L, 8L);
    private static final ShootingOrderRef ORDER_9 = new ShootingOrderRef(9L, 9L);
    private static final ShootingOrderRef ORDER_10 = new ShootingOrderRef(10L, 10L);


    private ShootingOrderCancellerConfig config = new ShootingOrderCancellerConfig();

    {
        config.setWaitTimeoutInMillis(10);
        config.setTimeWithoutFetchToFinish(500);
    }

    private ShootingOrderLimiterWindowConfig limiterConfig = new ShootingOrderLimiterWindowConfig();

    {
        limiterConfig.setPeriodLimit(10);
        limiterConfig.setDurationInSecond(1);
    }

    private ShootingOrderCancelLimiter limiter = new ShootingOrderCancelWindowLimiter(limiterConfig);

    @Test
    public void allWithoutWaiting() {
        Instant now = getNow();
        Instant next = plusMillis(now, 10);
        Instant next2 = plusMillis(next, 10);

        MockListener listener = new MockListener();

        ShootingOrderCanceller canceller = createCanceller(
            MockFetcher.of(
                Arrays.asList(ORDER_1, ORDER_2),
                Collections.singletonList(ORDER_3),
                Arrays.asList(ORDER_4, ORDER_5, ORDER_6)
            ),
            listener,
            MockClock.of(now, next, next2)
        );

        cancel(canceller);

        assertRecord(
            listener.getFetched(),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2),
                Arrays.asList(ORDER_1, ORDER_2)
            ),
            Pair.of(
                Collections.singletonList(ORDER_3),
                Collections.singletonList(ORDER_3)
            ),
            Pair.of(
                Arrays.asList(ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_4, ORDER_5, ORDER_6)
            )
        );

        assertRecord(
            listener.getScheduled(),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2),
                Arrays.asList(ORDER_1, ORDER_2)
            ),
            Pair.of(
                Collections.singletonList(ORDER_3),
                Collections.singletonList(ORDER_3)
            ),
            Pair.of(
                Arrays.asList(ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_4, ORDER_5, ORDER_6)
            )
        );

        assertRecord(
            listener.getWaiting(),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2),
                Collections.emptyList()
            ),
            Pair.of(
                Collections.singletonList(ORDER_3),
                Collections.emptyList()
            ),
            Pair.of(
                Arrays.asList(ORDER_4, ORDER_5, ORDER_6),
                Collections.emptyList()
            )
        );

        Assert.assertEquals(0, listener.getEmptyScheduled());
        Assert.assertEquals(3, listener.getEmptyWaiting());
    }

    @Test
    public void waitingOnlyIncrease() {
        Instant now = getNow();
        Instant next = plusMillis(now, 10);
        Instant next2 = plusMillis(next, 10);
        Instant next3 = plusMillis(next2, 10);
        Instant next4 = plusMillis(next3, 1000);

        MockListener listener = new MockListener();

        ShootingOrderCanceller canceller = createCanceller(
            MockFetcher.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1),
                Arrays.asList(ORDER_2, ORDER_3, ORDER_4),
                Arrays.asList(ORDER_5, ORDER_6)
            ),
            listener,
            MockClock.of(now, next, next2, next3, next4)
        );

        cancel(canceller);

        assertRecord(
            listener.getFetched(),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6)
            ),
            Pair.of(
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1),
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1)
            ),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4),
                Arrays.asList(ORDER_2, ORDER_3, ORDER_4)
            ),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_5, ORDER_6)
            )
        );

        assertRecord(
            listener.getScheduled(),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6)
            ),
            Pair.of(
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1),
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10)
            ),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6)
            )
        );

        assertRecord(
            listener.getWaiting(),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Collections.emptyList()
            ),
            Pair.of(
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1),
                Collections.singletonList(ORDER_1)
            ),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Collections.emptyList()
            )
        );

        Assert.assertEquals(2, listener.getEmptyScheduled());
        Assert.assertEquals(2, listener.getEmptyWaiting());
    }

    @Test
    public void waitingIncreaseAndDecrease() {
        Instant now = getNow();
        Instant next = plusMillis(now, 10);
        Instant next2 = plusMillis(next, 10);
        Instant next3 = plusMillis(next2, 1000);
        Instant next4 = plusMillis(next3, 10);

        MockListener listener = new MockListener();

        ShootingOrderCanceller canceller = createCanceller(
            MockFetcher.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1),
                Arrays.asList(ORDER_2, ORDER_3, ORDER_4),
                Arrays.asList(ORDER_5, ORDER_6),
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9)
            ),
            listener,
            MockClock.of(now, next, next2, next3, next4)
        );

        cancel(canceller);

        assertRecord(
            listener.getFetched(),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6)
            ),
            Pair.of(
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1),
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1)
            ),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4),
                Arrays.asList(ORDER_2, ORDER_3, ORDER_4)
            ),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_5, ORDER_6)
            ),
            Pair.of(
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9),
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9)
            )
        );

        assertRecord(
            listener.getScheduled(),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6)
            ),
            Pair.of(
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1),
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10)
            ),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6)
            ),
            Pair.of(
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9),
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9)
            )
        );

        assertRecord(
            listener.getWaiting(),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Collections.emptyList()
            ),
            Pair.of(
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9, ORDER_10, ORDER_1),
                Collections.singletonList(ORDER_1)
            ),
            Pair.of(
                Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_6),
                Collections.emptyList()
            ),
            Pair.of(
                Arrays.asList(ORDER_7, ORDER_8, ORDER_9),
                Collections.emptyList()
            )
        );

        Assert.assertEquals(1, listener.getEmptyScheduled());
        Assert.assertEquals(3, listener.getEmptyWaiting());
    }

    private void assertRecord(List<Pair<Collection<ShootingOrderRef>, Collection<ShootingOrderRef>>> orders,
                              Pair<Collection<ShootingOrderRef>, Collection<ShootingOrderRef>>... refs) {
        Assert.assertEquals(refs.length, orders.size());
        for (int i = 0; i < refs.length; ++i) {
            Collection<ShootingOrderRef> firstExpected = refs[i].getFirst();
            Collection<ShootingOrderRef> firstActual = orders.get(i).getFirst();

            assertOrders(firstExpected, firstActual);

            Collection<ShootingOrderRef> secondExpected = refs[i].getSecond();
            Collection<ShootingOrderRef> secondActual = orders.get(i).getSecond();

            assertOrders(secondExpected, secondActual);

        }
    }

    private void assertOrders(Collection<ShootingOrderRef> refs,
                              Collection<ShootingOrderRef> orders) {
        Assert.assertEquals(refs.size(), orders.size());
        Streams.zip(refs.stream(), orders.stream(), Pair::of).forEach(
            pair -> {
                ShootingOrderRef expected = pair.getFirst();
                ShootingOrderRef actual = pair.getSecond();

                Assert.assertEquals(expected.getOrderId(), actual.getOrderId());
                Assert.assertEquals(expected.getUid(), actual.getUid());
            }
        );
    }

    private Instant getNow() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private Instant plusMillis(Instant instant, int millis) {
        return instant.plus(millis, ChronoUnit.MILLIS);
    }

    private static void cancel(ShootingOrderCanceller canceller) {
        canceller.cancel();
    }

    private ShootingOrderCanceller createCanceller(MockFetcher fetcher, MockListener listener, MockClock clock) {
        return new ShootingOrderCanceller(
            config,
            fetcher,
            limiter,
            MockCleanerImpl.INSTANCE,
            listener,
            clock
        );
    }
}
