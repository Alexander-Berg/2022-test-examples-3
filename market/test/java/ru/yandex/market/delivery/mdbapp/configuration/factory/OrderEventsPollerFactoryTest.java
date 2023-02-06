package ru.yandex.market.delivery.mdbapp.configuration.factory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.delivery.mdbapp.components.curator.managers.OrderEventManager;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.poller.order_events.OrderEventsFetchingManager;
import ru.yandex.market.delivery.mdbapp.components.poller.order_events.OrderEventsPoller;
import ru.yandex.market.delivery.mdbapp.components.poller.order_events.OrderEventsProcessor;
import ru.yandex.market.delivery.mdbapp.components.service.InternalVariableService;
import ru.yandex.market.delivery.mdbapp.configuration.EventsSchedulerConfiguration;

public class OrderEventsPollerFactoryTest {

    private EventsSchedulerConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = Mockito.mock(EventsSchedulerConfiguration.class);
    }

    @Test
    public void testDisabled() {
        Mockito.when(configuration.isEnabled()).thenReturn(false);

        OrderEventsPoller actual = new OrderEventsPollerFactory(
            null,
            null,
            null,
            null,
            null,
            configuration,
            null
        ).create(0);

        Assert.assertNull(actual);
    }

    @Test
    public void testCreate() {
        Mockito.when(configuration.isEnabled()).thenReturn(true);

        BeanFactory beanFactory = Mockito.mock(BeanFactory.class);
        OrderEventManager expectedOrderEventManager = Mockito.mock(OrderEventManager.class);
        Mockito
            .when(beanFactory.getBean(
                Mockito.eq(EventsSchedulerConfiguration.eventManagerBeanName(33)),
                Mockito.eq(OrderEventManager.class)
            ))
            .thenReturn(expectedOrderEventManager);

        OrderEventsPoller actual = new OrderEventsPollerFactory(
            beanFactory,
            Mockito.mock(OrderEventsFetchingManager.class),
            Mockito.mock(OrderEventFailoverableService.class),
            Mockito.mock(OrderEventsProcessor.class),
            null,
            configuration,
            Mockito.mock(InternalVariableService.class)
        ).create(33);

        OrderEventManager actualOrderEventManager =
            (OrderEventManager) ReflectionTestUtils.getField(actual, "eventManager");
        Assert.assertEquals(
            expectedOrderEventManager,
            actualOrderEventManager
        );
    }
}
