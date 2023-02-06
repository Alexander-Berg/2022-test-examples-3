package ru.yandex.market.checkout.application;


import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.OrderEventPublishService;
import ru.yandex.market.checkout.checkouter.tasks.Partition;

/**
 * @author sergeykoles
 * Created on: 04.04.18
 */
@Aspect
public class EventPublisherAspect {

    private static volatile boolean enabled = true;

    @Autowired
    private OrderEventPublishService publishService;

    @Before("bean(eventService) && execution(* *.get*Event*(..))")
    public void publishEventsOnServiceCall() {
        if (enabled) {
            publishService.publishEventsBatch(Integer.MAX_VALUE, Partition.of(0, 10));
            publishService.publishEventsBatch(Integer.MAX_VALUE, Partition.NULL);
        }
    }

    public static void setEnabled(boolean enabled) {
        EventPublisherAspect.enabled = enabled;
    }
}
