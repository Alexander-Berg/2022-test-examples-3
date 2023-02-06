package ru.yandex.market.delivery.mdbapp.configuration.factory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.delivery.mdbapp.components.curator.managers.LockManager;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.poller.order_events.OrderEventsPoller;
import ru.yandex.market.delivery.mdbapp.configuration.EventsSchedulerConfiguration;
import ru.yandex.market.delivery.mdbapp.scheduled.checkouter.order.LockCountHolder;
import ru.yandex.market.delivery.mdbapp.scheduled.checkouter.order.OrderHistoryEventScheduler;
import ru.yandex.market.delivery.mdbapp.scheduled.checkouter.order.PollerRunningCountMeter;

public class OrderHistoryEventSchedulerFactoryTest {

    private final PollerRunningCountMeter runningCountMeter = Mockito.mock(PollerRunningCountMeter.class);
    private final LockCountHolder lockCountHolder = Mockito.mock(LockCountHolder.class);
    private EventsSchedulerConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = Mockito.mock(EventsSchedulerConfiguration.class);
    }
    @Test
    public void testDisabled() {
        Mockito.when(configuration.isEnabled()).thenReturn(false);

        OrderHistoryEventScheduler actual = new OrderHistoryEventSchedulerFactory(
            null,
            null,
            null,
            configuration,
            runningCountMeter,
            lockCountHolder
        ).create(0);

        Assert.assertNull(actual);
    }

    @Test
    public void testCreate() {
        Mockito.when(configuration.isEnabled()).thenReturn(true);

        BeanFactory beanFactory = Mockito.mock(BeanFactory.class);
        OrderEventsPoller expectedPoller = Mockito.mock(OrderEventsPoller.class);
        Mockito
            .when(beanFactory.getBean(
                Mockito.eq(EventsSchedulerConfiguration.pollerBeanName(33)),
                Mockito.eq(OrderEventsPoller.class)
            ))
            .thenReturn(expectedPoller);

        OrderHistoryEventScheduler actual = new OrderHistoryEventSchedulerFactory(
            beanFactory,
            Mockito.mock(HealthManager.class),
            Mockito.mock(LockManager.class),
            configuration,
            runningCountMeter,
            lockCountHolder
        ).create(33);

        OrderEventsPoller actualPoller = (OrderEventsPoller) ReflectionTestUtils.getField(actual, "eventsPoller");
        Assert.assertEquals(
            expectedPoller,
            actualPoller
        );

    }
}
