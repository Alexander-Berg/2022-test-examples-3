package ru.yandex.market.global.checkout.order;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.event.EventRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.domain.queue.task.PrePaymentSuccessProducer;
import ru.yandex.market.global.checkout.domain.queue.task.RestorePromoProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentAuthorizeProducer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.order.rules.OrderRulesTest;
import ru.yandex.market.global.checkout.order.rules.OrderRulesTestBuilder;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderEvent;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EProcessingMode;
import ru.yandex.market.global.db.jooq.enums.EShopOrderState;

import static ru.yandex.market.global.checkout.order.rules.OrderRulesTestBuilder.TestEvent.te;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_AUTHORIZE_FAIL;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PrePaymentOrderRulesServiceTest extends BaseFunctionalTest {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final ConfigurationService configurationService;
    private final TestOrderFactory testOrderFactory;

    @Test
    public void testSuccessToNew() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.WAITING_PAYMENT,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_WAITING_PAYMENT_START, 0),
                        te(EOrderEvent.SHOP_READY, 2)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.FINISHED,
                EDeliveryOrderState.ORDER_DELIVERED,
                EPaymentOrderState.CLEARED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testSuccessToScheduled() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.WAITING_PAYMENT,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setOrderIdProducer(PrePaymentSuccessProducer.class, List.of(
                        te(EOrderEvent.ORDER_SCHEDULED)
                ))
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_WAITING_PAYMENT_START, 0),
                        te(EOrderEvent.SHOP_READY, 2)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.SCHEDULED,
                EDeliveryOrderState.NEW,
                EPaymentOrderState.AUTHORIZED,
                EShopOrderState.NEW
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testAuthorizeFail() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.WAITING_PAYMENT,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setOrderIdProducer(PaymentAuthorizeProducer.class, List.of(
                        te(PAYMENT_AUTHORIZE_FAIL, 15)
                ))
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_WAITING_PAYMENT_START, 0),
                        te(EOrderEvent.SHOP_READY, 2)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.CANCELED,
                EDeliveryOrderState.NEW,
                EPaymentOrderState.NOT_AUTHORIZED,
                EShopOrderState.NEW
        );

        test.assertAllProducersInvokedOnlyOnce();
        test.assertThatProducerInvokedNTimes(RestorePromoProducer.class, 1);
    }

}
