package ru.yandex.market.tpl.core.service.notification;

import java.util.Set;

import org.assertj.core.api.AbstractComparableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.tracking.SmsSkipType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.service.demo.DemoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERY_FAILED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.NOT_DELIVERED;
import static ru.yandex.market.tpl.api.model.tracking.SmsSkipType.ORDER_DELIVERED;
import static ru.yandex.market.tpl.api.model.tracking.SmsSkipType.ORDER_DELIVERY_FAILED;
import static ru.yandex.market.tpl.api.model.tracking.SmsSkipType.SHOULD_NOT_SKIP;
import static ru.yandex.market.tpl.api.model.tracking.SmsSkipType.SMS_DISABLED;

class NotificationCheckerTest {

    private static final long DISABLED_DS_ID = 2L;

    private final SortingCenterService sortingCenterService = mock(SortingCenterService.class);
    private final DemoService demoService = mock(DemoService.class);
    private final NotificationChecker notificationChecker = new NotificationChecker(sortingCenterService, demoService);

    @BeforeEach
    void setUp() {
        when(sortingCenterService.getLavkaDeliveryServiceIds()).thenReturn(Set.of(DISABLED_DS_ID));
    }

    @Test
    void shouldSkipOnDeliveryFinished() {
        sendSmsResult(DELIVERED).isEqualTo(ORDER_DELIVERED);
        sendSmsResult(DELIVERY_FAILED).isEqualTo(ORDER_DELIVERY_FAILED);
    }

    @Test
    void shouldSendOnUnfinishedDelivery() {
        sendSmsResult(NOT_DELIVERED).isEqualTo(SHOULD_NOT_SKIP);
    }

    @Test
    void shouldSkipOnDisabledDeliveryServiceId() {
        Order order = mock(Order.class);
        when(order.getDeliveryServiceId()).thenReturn(DISABLED_DS_ID);
        assertThat(notificationChecker.mapToSmsSkipType(NOT_DELIVERED, order)).isEqualTo(SMS_DISABLED);
    }

    private AbstractComparableAssert<?, SmsSkipType> sendSmsResult(OrderDeliveryTaskStatus status) {
        Order order = mock(Order.class);
        when(order.getDeliveryServiceId()).thenReturn(1L);
        return assertThat(notificationChecker.mapToSmsSkipType(status, order));
    }

}
