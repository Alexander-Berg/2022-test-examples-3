package ru.yandex.market.global.checkout.order;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.global.checkout.config.OrderRulesConfig;
import ru.yandex.market.global.checkout.domain.order.rule.OrderRule;
import ru.yandex.market.global.checkout.domain.queue.task.CheckOrderTerminatedProducer;
import ru.yandex.market.global.checkout.domain.queue.task.CreateCheckDeliveredOrderTicketProducer;
import ru.yandex.market.global.checkout.domain.queue.task.CreateManualOrderTicketProducer;
import ru.yandex.market.global.checkout.domain.queue.task.DeliveryCancelProducer;
import ru.yandex.market.global.checkout.domain.queue.task.DeliveryCourierSearchProducer;
import ru.yandex.market.global.checkout.domain.queue.task.DeliveryPlaceOrderProducer;
import ru.yandex.market.global.checkout.domain.queue.task.FireEventProducer;
import ru.yandex.market.global.checkout.domain.queue.task.PrePaymentSuccessProducer;
import ru.yandex.market.global.checkout.domain.queue.task.RestorePromoProducer;
import ru.yandex.market.global.checkout.domain.queue.task.ScheduleOrderCourierSearchProducer;
import ru.yandex.market.global.checkout.domain.queue.task.ScheduleOrderOutdateProducer;
import ru.yandex.market.global.checkout.domain.queue.task.SendOrderNotificationProducer;
import ru.yandex.market.global.checkout.domain.queue.task.ShopPlaceOrderProducer;
import ru.yandex.market.global.checkout.domain.queue.task.deliveries.UpdateCargoClaimInfoProducer;
import ru.yandex.market.global.checkout.domain.queue.task.deliveries.UpdateCourierReceiveOrderCodeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.deliveries.UpdateCourierReturnOrderCodeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.notification.SendRecipientOrderNotificationProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.OrderPlusRewardTopUpProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentAuthorizeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentCancelProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentClearProducer;
import ru.yandex.market.global.checkout.domain.queue.task.receipt.PaymentCancellationProducer;
import ru.yandex.market.global.checkout.domain.queue.task.receipt.PaymentConfirmationProducer;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.CheckOrderCollectedInTimeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.CheckOrderDeliveredInTimeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.CheckOrderSeenProducer;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderEvent;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EProcessingMode;
import ru.yandex.market.global.db.jooq.enums.EShopOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;

public class OrderRulesCoverageTest {
    static private List<OrderRule> orderRules;

    @BeforeAll
    public static void setup() {
        OrderRulesConfig config = new OrderRulesConfig(
                Mockito.mock(FireEventProducer.class),
                Mockito.mock(DeliveryPlaceOrderProducer.class),
                Mockito.mock(DeliveryCourierSearchProducer.class),
                Mockito.mock(DeliveryCancelProducer.class),

                Mockito.mock(UpdateCargoClaimInfoProducer.class),
                Mockito.mock(UpdateCourierReceiveOrderCodeProducer.class),
                Mockito.mock(UpdateCourierReturnOrderCodeProducer.class),

                Mockito.mock(PaymentAuthorizeProducer.class),
                Mockito.mock(PaymentClearProducer.class),
                Mockito.mock(PaymentCancelProducer.class),
                Mockito.mock(OrderPlusRewardTopUpProducer.class),
                Mockito.mock(PaymentConfirmationProducer.class),
                Mockito.mock(PaymentCancellationProducer.class),
                Mockito.mock(SendOrderNotificationProducer.class),
                Mockito.mock(CreateCheckDeliveredOrderTicketProducer.class),
                Mockito.mock(CreateManualOrderTicketProducer.class),
                Mockito.mock(RestorePromoProducer.class),

                Mockito.mock(CheckOrderCollectedInTimeProducer.class),
                Mockito.mock(CheckOrderDeliveredInTimeProducer.class),
                Mockito.mock(CheckOrderSeenProducer.class),

                Mockito.mock(ScheduleOrderCourierSearchProducer.class),
                Mockito.mock(ScheduleOrderOutdateProducer.class),
                Mockito.mock(SendRecipientOrderNotificationProducer.class),
                Mockito.mock(CheckOrderTerminatedProducer.class),
                Mockito.mock(PrePaymentSuccessProducer.class),
                Mockito.mock(ShopPlaceOrderProducer.class),
                Mockito.mock(ConfigurationService.class)
        );
        config.init();

        orderRules = config.orderRules();
    }


    //Тест который проверяет что для любого заказа и события есть правило обработки
    @ParameterizedTest(name = "event = {0} order = {1}, {2}, {3}, {4}, {5}")
    @MethodSource
    public void testAllEventsMatchedToAnyOrder(
            EOrderEvent event,
            EProcessingMode processingMode,
            EOrderState systemOrderState,
            EDeliveryOrderState deliveryOrderState,
            EPaymentOrderState paymentOrderState,
            EShopOrderState shopOrderState
    ) {
        Order order = new Order()
                .setProcessingMode(processingMode)
                .setOrderState(systemOrderState)
                .setDeliveryState(deliveryOrderState)
                .setPaymentState(paymentOrderState)
                .setShopState(shopOrderState);

        Assertions.assertThat(orderRules).anyMatch(r -> r.test(order, event));
    }

    private static Stream<Arguments> testAllEventsMatchedToAnyOrder() {
        List<Arguments> arguments = new ArrayList<>();
        for (EOrderEvent event : EOrderEvent.values()) {
            for (EProcessingMode processingMode : EProcessingMode.values()) {
                for (EOrderState orderState : EOrderState.values()) {
                    for (EDeliveryOrderState deliveryOrderState : EDeliveryOrderState.values()) {
                        for (EPaymentOrderState paymentOrderState : EPaymentOrderState.values()) {
                            for (EShopOrderState shopOrderState : EShopOrderState.values()) {
                                arguments.add(Arguments.of(
                                        event,
                                        processingMode,
                                        orderState,
                                        deliveryOrderState,
                                        paymentOrderState,
                                        shopOrderState
                                ));
                            }
                        }
                    }
                }
            }
        }
        return arguments.stream();
    }
}
