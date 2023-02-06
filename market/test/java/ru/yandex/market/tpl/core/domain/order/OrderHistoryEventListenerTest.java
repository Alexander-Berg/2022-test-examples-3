package ru.yandex.market.tpl.core.domain.order;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransition;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransitionType;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.service.call_forwarding.ExternalCallForwardingManager;
import ru.yandex.market.tpl.core.service.call_forwarding.ExternalCallForwardingQueryService;
import ru.yandex.market.tpl.core.service.task.OrderDeliveryTaskService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

class OrderHistoryEventListenerTest {
    private static final long ORDER_ID = 123L;
    private static final User USER = UserUtil.createUserWithoutSchedule(1);

    private OrderHistoryEventListener orderHistoryEventListener;

    @BeforeEach
    void setup() {
        UserRepository userRepository = mock(UserRepository.class);
        ConfigurationProviderAdapter configurationProviderAdapter = mock(ConfigurationProviderAdapter.class);
        OrderManager orderManager = mock(OrderManager.class);
        DsZoneOffsetCachingService dsZoneOffsetCachingService = mock(DsZoneOffsetCachingService.class);
        OrderHistoryEventRepository orderHistoryEventRepository = mock(OrderHistoryEventRepository.class);
        ExternalCallForwardingManager externalCallForwardingManager = mock(ExternalCallForwardingManager.class);
        OrderDeliveryTaskService orderDeliveryTaskService = mock(OrderDeliveryTaskService.class);
        ExternalCallForwardingQueryService externalCallForwardingQueryService =
                mock(ExternalCallForwardingQueryService.class);

        when(userRepository.findUserForOrder(anyLong())).thenReturn(Optional.empty());
        when(userRepository.findUserForOrder(ORDER_ID)).thenReturn(Optional.of(USER));

        orderHistoryEventListener = new OrderHistoryEventListener(orderHistoryEventRepository, userRepository,
                configurationProviderAdapter, orderManager, dsZoneOffsetCachingService, externalCallForwardingManager,
                externalCallForwardingQueryService, orderDeliveryTaskService);
    }

    @Test
    void shouldReturnEmptyStringByStatusTransitionType() {
        assertThat(orderHistoryEventListener.getContext((StatusTransition<OrderFlowStatus, Source>) null)).isEqualTo(null);
    }

    @Test
    void shouldReturnContextWithOrderStatus() {
        var statusTransition = new StatusTransition<>(
                null,
                OrderFlowStatus.CREATED,
                StatusTransitionType.NORMAL,
                Source.SYSTEM
        );

        assertThat(orderHistoryEventListener.getContext(statusTransition)).isEqualTo(
                "Заказ создан (1)"
        );
    }

    @Test
    void shouldReturnEmptyStringByOrderDeliveryRescheduleType() {
        assertThat(
                orderHistoryEventListener.getContext(
                        (DeliveryReschedule) null,
                        null,
                        DateTimeUtil.DEFAULT_ZONE_ID)
        ).isEqualTo(null);
    }

    @Test
    void shouldReturnContextWithReasonAndNewInterval() {
        var startInterval = Instant.now();
        var endInterval = Instant.now().plus(2, ChronoUnit.HOURS);

        var deliveryReschedule = DeliveryReschedule.fromCourier(
                USER, startInterval, endInterval, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        Interval oldInterval = new Interval(
                Instant.now()
                        .plus(1, ChronoUnit.HOURS), Instant.now().plus(5, ChronoUnit.HOURS));
        assertThat(orderHistoryEventListener.getContext(deliveryReschedule, oldInterval,
                DateTimeUtil.DEFAULT_ZONE_ID)).isEqualTo(
                String.format(
                        "Курьер %s перенёс задание, Причина: По просьбе клиента, Старый интервал: %s - %s;" +
                                " Новый интервал: %s - %s",
                        USER.getFullName(),
                        OrderHistoryEventListener.DATE_TIME_FORMATTER
                                .format(oldInterval.getStart()),
                        OrderHistoryEventListener.TIME_FORMATTER
                                .format(oldInterval.getEnd()),
                        OrderHistoryEventListener.DATE_TIME_FORMATTER
                                .format(deliveryReschedule.getInterval().getStart()),
                        OrderHistoryEventListener.TIME_FORMATTER
                                .format(deliveryReschedule.getInterval().getEnd())
                )
        );
    }

    @Test
    void shouldReturnEmptyStringByOrderDeliveryFailReasonType() {
        assertThat(orderHistoryEventListener.getContext(null, ORDER_ID)).isEqualTo(null);
    }

    @Test
    void shouldReturnContextWithTypeAndComment() {
        var orderDeliveryFailReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.CANCEL_ORDER,
                "test comment"
        );

        assertThat(orderHistoryEventListener.getContext(orderDeliveryFailReason, ORDER_ID)).isEqualTo(
                sf("Курьер {} отменил задание, Причина: Заказ отменён, Комментарий: test comment", USER.getFullName())
        );
    }

    @Test
    void shouldReturnContextWithType() {
        var orderDeliveryFailReason = new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, null);

        assertThat(orderHistoryEventListener.getContext(orderDeliveryFailReason, ORDER_ID)).isEqualTo(
                sf("Курьер {} отменил задание, Причина: Заказ отменён", USER.getFullName())
        );

        orderDeliveryFailReason = new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, "");

        assertThat(orderHistoryEventListener.getContext(orderDeliveryFailReason, ORDER_ID)).isEqualTo(
                sf("Курьер {} отменил задание, Причина: Заказ отменён", USER.getFullName())
        );
    }

    @Test
    void shouldReturnCourierIsNeedHelpTest() {
        var failReason = new OrderDeliveryFailReason(COURIER_NEEDS_HELP, null);

        assertThat(
                orderHistoryEventListener.getContext(failReason, ORDER_ID)
        ).isEqualTo(
                sf("Курьер {} отменил задание, Причина: Нужна помощь с заказом!", USER.getFullName())
        );
    }
}
