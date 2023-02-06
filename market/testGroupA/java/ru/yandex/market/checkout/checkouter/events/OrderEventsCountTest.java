package ru.yandex.market.checkout.checkouter.events;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.helpers.EventCountHelper;


/**
 * Created by poluektov on 10.04.17.
 */
public class OrderEventsCountTest extends AbstractWebTestBase {

    @Autowired
    private EventCountHelper eventCountHelper;

    @Test
    public void testEventsCountResponse() throws Exception {
        eventCountHelper.getOrderEventsCount(
                1, 3, EnumSet.of(HistoryEventType.ORDER_STATUS_UPDATED), null
        );
    }
}
