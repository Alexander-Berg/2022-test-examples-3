package ru.yandex.market.tsum.pipelines.load;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.checkout.ShootingOrderRef;
import ru.yandex.market.tsum.pipelines.load.configs.ShootingOrderLimiterWindowConfig;

public class ShootingOrderCancelWindowLimiterTest {
    private ShootingOrderLimiterWindowConfig config = new ShootingOrderLimiterWindowConfig();

    {
        config.setPeriodLimit(5);
        config.setDurationInSecond(60);
    }

    private ShootingOrderCancelLimiter limiter = new ShootingOrderCancelWindowLimiter(config);

    private static final ShootingOrderRef ORDER_1 = new ShootingOrderRef(1L, 1L);
    private static final ShootingOrderRef ORDER_2 = new ShootingOrderRef(2L, 2L);
    private static final ShootingOrderRef ORDER_3 = new ShootingOrderRef(3L, 3L);
    private static final ShootingOrderRef ORDER_4 = new ShootingOrderRef(4L, 4L);
    private static final ShootingOrderRef ORDER_5 = new ShootingOrderRef(5L, 5L);


    @Test
    public void nullableLimit() {
        ShootingOrderCancelScheduleResult result = limiter.schedule(null, Instant.now());
        assertOrdersEmpty(result.getScheduled());
        assertOrdersEmpty(result.getWaiting());
    }

    @Test
    public void emptyLimit() {
        ShootingOrderCancelScheduleResult result = limiter.schedule(Collections.emptyList(), Instant.now());
        assertOrdersEmpty(result.getScheduled());
        assertOrdersEmpty(result.getWaiting());
    }

    @Test
    public void twoTriesLessThanLimit() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_3, ORDER_4);

        ShootingOrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2);
        assertOrdersEmpty(result.getWaiting());

        ShootingOrderCancelScheduleResult result2 = limiter.schedule(refs2, now);
        assertOrders(result2.getScheduled(), ORDER_3, ORDER_4);
        assertOrdersEmpty(result2.getWaiting());
    }

    @Test
    public void twoTriesMoreThanLimit() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_3, ORDER_4, ORDER_5);
        List<ShootingOrderRef> refs3 = Collections.singletonList(ORDER_1);

        ShootingOrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2);
        assertOrdersEmpty(result.getWaiting());

        ShootingOrderCancelScheduleResult result2 = limiter.schedule(refs2, now);
        assertOrders(result2.getScheduled(), ORDER_3, ORDER_4, ORDER_5);
        assertOrdersEmpty(result2.getWaiting());

        ShootingOrderCancelScheduleResult result3 = limiter.schedule(refs3, now);
        assertOrdersEmpty(result3.getScheduled());
        assertOrders(result3.getWaiting(), ORDER_1);
    }

    @Test
    public void twoTriesWithWaitingTail() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2, ORDER_3);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_4, ORDER_5, ORDER_1, ORDER_2);

        ShootingOrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3);
        assertOrdersEmpty(result.getWaiting());

        ShootingOrderCancelScheduleResult result2 = limiter.schedule(refs2, now);
        assertOrders(result2.getScheduled(), ORDER_4, ORDER_5);
        assertOrders(result2.getWaiting(), ORDER_1, ORDER_2);
    }

    @Test
    public void oneTryWithWaitingTail() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_3);

        ShootingOrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        assertOrders(result.getWaiting(), ORDER_3);
    }

    @Test
    public void twoTriesWithWithoutSchedule() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_1, ORDER_2);

        ShootingOrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        assertOrdersEmpty(result.getWaiting());

        ShootingOrderCancelScheduleResult result2 = limiter.schedule(refs2, now);
        assertOrdersEmpty(result2.getScheduled());
        assertOrders(result2.getWaiting(), ORDER_1, ORDER_2);
    }

    @Test
    public void twoTiresInTwoPeriodsWithoutWaiting() {
        Instant now = getNow();
        Instant next = nextTimeWindow(now);

        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_5, ORDER_3, ORDER_2, ORDER_1, ORDER_4);

        ShootingOrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        assertOrdersEmpty(result.getWaiting());

        ShootingOrderCancelScheduleResult result2 = limiter.schedule(refs2, next);
        assertOrders(result2.getScheduled(), ORDER_5, ORDER_3, ORDER_2, ORDER_1, ORDER_4);
        assertOrdersEmpty(result2.getWaiting());
    }

    @Test
    public void megaTriesWithOneBigQueueAndSomeEmptyWaitingPeriods() {
        Instant now = getNow();
        Instant next = nextTimeWindow(now);
        Instant next2 = nextTimeWindow(next);
        Instant next3 = nextTimeWindow(next2);
        Instant next4 = nextTimeWindow(next3);

        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_3);
        List<ShootingOrderRef> refs2 = Collections.singletonList(ORDER_4);
        List<ShootingOrderRef> refs3 = Arrays.asList(ORDER_5, ORDER_3, ORDER_2);
        List<ShootingOrderRef> refs4 = Arrays.asList(ORDER_4, ORDER_1, ORDER_3, ORDER_5);
        List<ShootingOrderRef> refs5 = Arrays.asList(ORDER_1, ORDER_3, ORDER_2, ORDER_5);
        List<ShootingOrderRef> refs6 = Arrays.asList(ORDER_2, ORDER_3, ORDER_5);

        ShootingOrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        assertOrders(result.getWaiting(), ORDER_3);

        ShootingOrderCancelScheduleResult result2 = limiter.schedule(refs2, next);
        assertOrders(result2.getScheduled(), ORDER_4);
        assertOrdersEmpty(result2.getWaiting());

        ShootingOrderCancelScheduleResult result3 = limiter.schedule(refs3, next2);
        assertOrders(result3.getScheduled(), ORDER_5, ORDER_3, ORDER_2);
        assertOrdersEmpty(result3.getWaiting());

        ShootingOrderCancelScheduleResult result4 = limiter.schedule(refs4, next2);
        assertOrders(result4.getScheduled(), ORDER_4, ORDER_1);
        assertOrders(result4.getWaiting(), ORDER_3, ORDER_5);

        ShootingOrderCancelScheduleResult result5 = limiter.schedule(refs5, next2);
        assertOrdersEmpty(result5.getScheduled());
        assertOrders(result5.getWaiting(), ORDER_1, ORDER_3, ORDER_2, ORDER_5);

        ShootingOrderCancelScheduleResult result6 = limiter.schedule(refs6, next4);
        assertOrders(result6.getScheduled(), ORDER_2, ORDER_3, ORDER_5);
        assertOrdersEmpty(result6.getWaiting());
    }


    private void assertOrdersEmpty(Collection<ShootingOrderRef> orders) {
        Assert.assertTrue(orders.isEmpty());
    }

    private void assertOrders(Collection<ShootingOrderRef> orders, ShootingOrderRef... refs) {
        Assert.assertEquals(refs.length, orders.size());
        Iterator<ShootingOrderRef> ordersIter = orders.iterator();
        for (int i = 0; i < refs.length; ++i) {
            ShootingOrderRef orderActual = ordersIter.next();
            ShootingOrderRef orderExpected = refs[i];
            Assert.assertEquals(orderExpected.getOrderId(), orderActual.getOrderId());
            Assert.assertEquals(orderExpected.getUid(), orderActual.getUid());
        }
    }

    private Instant getNow() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private Instant nextTimeWindow(Instant instant) {
        return instant.plus(60, ChronoUnit.SECONDS);
    }

}
