package ru.yandex.market.checkout.checkouter.tasks.eventexport;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.application.EventPublisherAspect;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.common.tasks.EnableAwareTask;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static ru.yandex.market.checkout.checkouter.tasks.eventexport.OrderEventPublisherTask.EVENT_PUBLISH_TASK;


public class OrderEventPublisherTaskTest extends AbstractWebTestBase {

    @Autowired
    private Map<String, ZooTask> taskMap;
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @BeforeEach
    public void init() {
        taskMap.entrySet()
                .stream()
                .filter(stringZooTaskEntry -> stringZooTaskEntry.getKey().contains(EVENT_PUBLISH_TASK))
                .map(Map.Entry::getValue)
                .map(ZooTask::asEnableAwareTask)
                .flatMap(Optional::stream)
                .forEach(EnableAwareTask::disable);
        EventPublisherAspect.setEnabled(false);
    }

    @AfterEach
    public void tearDown() {
        EventPublisherAspect.setEnabled(true);
    }

    @Test
    public void shouldNotPublishEventsWithoutTask() throws Exception {
        orderServiceHelper.createOrder(Color.BLUE);

        OrderHistoryEvents events = client.orderHistoryEvents().getOrderHistoryEvents(0L, 100, null, false, null,
                OrderFilter.builder().setRgb(Color.BLUE).build());
        Assertions.assertTrue(events.getContent().isEmpty());
    }

    @Test
    public void shoudPublishEventsAfterTaskRun() throws Exception {
        orderServiceHelper.createOrder(Color.BLUE);

        taskMap.entrySet()
                .stream()
                .filter(stringZooTaskEntry -> stringZooTaskEntry.getKey().contains(EVENT_PUBLISH_TASK))
                .map(Map.Entry::getValue)
                .map(ZooTask::asEnableAwareTask)
                .flatMap(Optional::stream)
                .peek(EnableAwareTask::enable)
                .forEach(ZooTask::runOnce);

        OrderHistoryEvents events = client.orderHistoryEvents().getOrderHistoryEvents(0L, 100, null, false, null,
                OrderFilter.builder().setRgb(Color.BLUE).build());
        Assertions.assertFalse(events.getContent().isEmpty());
    }
}
