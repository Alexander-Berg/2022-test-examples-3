package ru.yandex.market.checkout.checkouter.context;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.tasks.eventexport.logbroker.OrderEventsLbkxExportTask;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {"market.checkout.lbkx.topic.order-event.partition.count=10"})
public class OrderEventExportContextTest extends AbstractServicesTestBase {

    @Autowired
    private ApplicationContext context;

    @Test
    void tasksCountTest() {
        assertEquals(10, Arrays.stream(context.getBeanDefinitionNames())
                .filter(name -> name.startsWith(OrderEventsLbkxExportTask.EVENT_EXPORT_TASK + "_"))
                .count());
    }
}
