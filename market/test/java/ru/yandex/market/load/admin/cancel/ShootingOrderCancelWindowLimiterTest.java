package ru.yandex.market.load.admin.cancel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.load.admin.service.cancel.OrderCancelLimiter;
import ru.yandex.market.load.admin.service.cancel.OrderCancelScheduleResult;
import ru.yandex.market.load.admin.service.cancel.OrderCancelWindowLimiter;
import ru.yandex.market.load.admin.service.cancel.OrderLimiterWindowConfig;
import ru.yandex.mj.generated.client.checkouter.model.ShootingOrderRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShootingOrderCancelWindowLimiterTest {
    private OrderLimiterWindowConfig config = new OrderLimiterWindowConfig();

    {
        config.setPeriodLimit(5);
        config.setDurationInSecond(60);
    }

    private OrderCancelLimiter limiter = new OrderCancelWindowLimiter(config);

    private static final ShootingOrderRef ORDER_1 = new ShootingOrderRef().orderId(1L).uid(1L);
    private static final ShootingOrderRef ORDER_2 = new ShootingOrderRef().orderId(2L).uid(2L);
    private static final ShootingOrderRef ORDER_3 = new ShootingOrderRef().orderId(3L).uid(3L);
    private static final ShootingOrderRef ORDER_4 = new ShootingOrderRef().orderId(4L).uid(4L);
    private static final ShootingOrderRef ORDER_5 = new ShootingOrderRef().orderId(5L).uid(5L);


    @Test
    public void nullableLimit() {
        OrderCancelScheduleResult result = limiter.schedule(null, Instant.now());
        assertOrdersEmpty(result.getScheduled());
        assertOrdersEmpty(result.getWaiting());
    }

    @Test
    public void emptyLimit() {
        OrderCancelScheduleResult result = limiter.schedule(Collections.emptyList(), Instant.now());
        assertOrdersEmpty(result.getScheduled());
        assertOrdersEmpty(result.getWaiting());
    }

    @Test
    public void twoTriesLessThanLimit() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_3, ORDER_4);

        OrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2);
        assertOrdersEmpty(result.getWaiting());

        OrderCancelScheduleResult result2 = limiter.schedule(refs2, now);
        assertOrders(result2.getScheduled(), ORDER_3, ORDER_4);
        assertOrdersEmpty(result2.getWaiting());
    }

    @Test
    public void twoTriesMoreThanLimit() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_3, ORDER_4, ORDER_5);
        List<ShootingOrderRef> refs3 = Collections.singletonList(ORDER_1);

        OrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2);
        assertOrdersEmpty(result.getWaiting());

        OrderCancelScheduleResult result2 = limiter.schedule(refs2, now);
        assertOrders(result2.getScheduled(), ORDER_3, ORDER_4, ORDER_5);
        assertOrdersEmpty(result2.getWaiting());

        OrderCancelScheduleResult result3 = limiter.schedule(refs3, now);
        assertOrdersEmpty(result3.getScheduled());
        assertOrders(result3.getWaiting(), ORDER_1);
    }

    @Test
    public void twoTriesWithWaitingTail() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2, ORDER_3);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_4, ORDER_5, ORDER_1, ORDER_2);

        OrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3);
        assertOrdersEmpty(result.getWaiting());

        OrderCancelScheduleResult result2 = limiter.schedule(refs2, now);
        assertOrders(result2.getScheduled(), ORDER_4, ORDER_5);
        assertOrders(result2.getWaiting(), ORDER_1, ORDER_2);
    }

    @Test
    public void oneTryWithWaitingTail() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5, ORDER_3);

        OrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        assertOrders(result.getWaiting(), ORDER_3);
    }

    @Test
    public void twoTriesWithWithoutSchedule() {
        Instant now = getNow();
        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_1, ORDER_2);

        OrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        assertOrdersEmpty(result.getWaiting());

        OrderCancelScheduleResult result2 = limiter.schedule(refs2, now);
        assertOrdersEmpty(result2.getScheduled());
        assertOrders(result2.getWaiting(), ORDER_1, ORDER_2);
    }

    @Test
    public void twoTiresInTwoPeriodsWithoutWaiting() {
        Instant now = getNow();
        Instant next = nextTimeWindow(now);

        List<ShootingOrderRef> refs1 = Arrays.asList(ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        List<ShootingOrderRef> refs2 = Arrays.asList(ORDER_5, ORDER_3, ORDER_2, ORDER_1, ORDER_4);

        OrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        assertOrdersEmpty(result.getWaiting());

        OrderCancelScheduleResult result2 = limiter.schedule(refs2, next);
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

        OrderCancelScheduleResult result = limiter.schedule(refs1, now);
        assertOrders(result.getScheduled(), ORDER_1, ORDER_2, ORDER_3, ORDER_4, ORDER_5);
        assertOrders(result.getWaiting(), ORDER_3);

        OrderCancelScheduleResult result2 = limiter.schedule(refs2, next);
        assertOrders(result2.getScheduled(), ORDER_4);
        assertOrdersEmpty(result2.getWaiting());

        OrderCancelScheduleResult result3 = limiter.schedule(refs3, next2);
        assertOrders(result3.getScheduled(), ORDER_5, ORDER_3, ORDER_2);
        assertOrdersEmpty(result3.getWaiting());

        OrderCancelScheduleResult result4 = limiter.schedule(refs4, next2);
        assertOrders(result4.getScheduled(), ORDER_4, ORDER_1);
        assertOrders(result4.getWaiting(), ORDER_3, ORDER_5);

        OrderCancelScheduleResult result5 = limiter.schedule(refs5, next2);
        assertOrdersEmpty(result5.getScheduled());
        assertOrders(result5.getWaiting(), ORDER_1, ORDER_3, ORDER_2, ORDER_5);

        OrderCancelScheduleResult result6 = limiter.schedule(refs6, next4);
        assertOrders(result6.getScheduled(), ORDER_2, ORDER_3, ORDER_5);
        assertOrdersEmpty(result6.getWaiting());
    }


    private void assertOrdersEmpty(Collection<ShootingOrderRef> orders) {
        assertTrue(orders.isEmpty());
    }

    private void assertOrders(Collection<ShootingOrderRef> orders, ShootingOrderRef... refs) {
        assertEquals(refs.length, orders.size());
        Iterator<ShootingOrderRef> ordersIter = orders.iterator();
        for (int i = 0; i < refs.length; ++i) {
            ShootingOrderRef orderActual = ordersIter.next();
            ShootingOrderRef orderExpected = refs[i];
            assertEquals(orderExpected.getOrderId(), orderActual.getOrderId());
            assertEquals(orderExpected.getUid(), orderActual.getUid());
        }
    }

    private Instant getNow() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private Instant nextTimeWindow(Instant instant) {
        return instant.plus(60, ChronoUnit.SECONDS);
    }

}
