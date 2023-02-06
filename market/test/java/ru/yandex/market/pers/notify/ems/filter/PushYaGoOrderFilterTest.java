package ru.yandex.market.pers.notify.ems.filter;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.pers.notify.ems.NotificationEventConsumer;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class PushYaGoOrderFilterTest extends MarketMailerMockedDbTest {
    @Autowired
    private PushYaGoOrderFilter pushYaGoOrderFilter;

    /**
     * Если в событии отсутствует тип платформы, то фильтр такое событие пропускает
     */
    @Test
    public void testFilterEventWithoutOrderId() {
        NotificationEvent event = new NotificationEvent();
        event.setNotificationSubtype(NotificationSubtype.PUSH_STORE_PREPAID_DELAY);
        event.setData(Collections.emptyMap());

        NotificationEventConsumer eventConsumer = pushYaGoOrderFilter.filter(event, null);

        assertNull(eventConsumer);
    }

    /**
     * Если в событии есть признак платформы, и платформа является приложением Yandex GO,
     * то фильтр такое событие пропускает
     */
    @Test
    public void testFilterEventWithOrderFromNotYaGoPlatform() {
        NotificationEvent event = new NotificationEvent();
        event.setNotificationSubtype(NotificationSubtype.PUSH_STORE_PREPAID_DELAY);
        event.setData(Map.of(NotificationEventDataName.ORDER_PLATFORM, Platform.ANDROID.name()));

        NotificationEventConsumer eventConsumer = pushYaGoOrderFilter.filter(event, null);
        assertNull(eventConsumer);
    }

    /**
     * Если в событии есть признак платформы, и платформа является приложением Yandex GO,
     * то фильтр запрещает отправку уведомления
     */
    @Test
    public void testFilterEventWithOrderFromYaGoPlatform() {
        NotificationEvent event = new NotificationEvent();
        event.setNotificationSubtype(NotificationSubtype.PUSH_STORE_PREPAID_DELAY);

        event.setData(Map.of(NotificationEventDataName.ORDER_PLATFORM, Platform.YANDEX_GO_ANDROID.name()));
        assertEquals(pushYaGoOrderFilter.filter(event, null), PushYaGoOrderFilter.DO_NOT_SEND_CONSUMER);

        event.setData(Map.of(NotificationEventDataName.ORDER_PLATFORM, Platform.YANDEX_GO_ANDROID.name()));
        assertEquals(pushYaGoOrderFilter.filter(event, null), PushYaGoOrderFilter.DO_NOT_SEND_CONSUMER);
    }
}
